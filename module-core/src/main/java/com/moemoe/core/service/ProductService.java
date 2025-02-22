package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.mongo.entity.Product;
import com.moemoe.mongo.entity.Tag;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.TagEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final UserEntityRepository userEntityRepository;
    private final ProductEntityRepository productEntityRepository;
    private final TagEntityRepository tagEntityRepository;
    private final AwsS3Client awsS3Client;

    @Transactional(readOnly = true)
    public GetProductsResponse findAll(
            String oldNextId,
            int pageSize
    ) {
        if (invalidProductId(oldNextId)) {
            throw new IllegalArgumentException("old next id is invalid");
        }

        List<Product> productList = productEntityRepository.findAll(oldNextId, pageSize);
        List<GetProductsResponse.Product> contents = getProductContents(productList);
        return new GetProductsResponse(contents, pageSize);
    }

    private List<GetProductsResponse.Product> getProductContents(List<Product> productList) {
        try (S3Presigner s3Presigner = awsS3Client.getS3Presigner();) {
            return productList.stream()
                    .map(product -> GetProductsResponse.Product.builder()
                            .id(product.getStringId())
                            .title(product.getTitle())
                            .detailedAddress(product.getDetailedAddress())
                            .price(product.getPrice())
                            .tagIdList(product.getTagNameList())
                            .thumbnailUrl(awsS3Client.getPreSignedUrl(s3Presigner, product.getThumbnailUrl()))
                            .createAt(product.getCreatedDate())
                            .build())
                    .toList();
        }
    }

    private boolean invalidProductId(String oldNextId) {
        return !productEntityRepository.existsById(new ObjectId(oldNextId));
    }

    @Transactional
    public IdResponse register(RegisterProductRequest request,
                               List<MultipartFile> imageList) {
        validateSellerExists(request.getSellerId());
        List<String> imageUrlList = new ArrayList<>();
        try (S3Client s3Client = awsS3Client.getS3Client()) {
            for (MultipartFile image : imageList) {
                String imageUrl = awsS3Client.upload(s3Client, Path.of(request.getSellerId().toHexString(), getFileName(image)).toString(), image);
                imageUrlList.add(imageUrl);
            }
        } catch (Exception e) {
            throw new ClientRuntimeException(e.getMessage());
        }

        incrementTag(request);
        Product productEntity = createProductEntity(request, imageUrlList);
        return new IdResponse(productEntityRepository.save(productEntity).getId());
    }

    private void incrementTag(RegisterProductRequest request) {
        for (String tagName : request.getTagNameList()) {
            Optional<Tag> optionalTag = tagEntityRepository.findById(tagName);
            if (optionalTag.isPresent()) {
                tagEntityRepository.incrementProductsCount(tagName);
            } else {
                tagEntityRepository.save(Tag.of(tagName, 1L));
            }
        }
    }

    private String getFileName(MultipartFile image) {
        return Optional.ofNullable(image.getOriginalFilename())
                .map(fileName -> Path.of(fileName).getFileName().toString())
                .orElseThrow(() -> new IllegalArgumentException("파일 이름이 null 입니다."));
    }

    private Product createProductEntity(RegisterProductRequest request, List<String> imageUrlList) {
        return Product.of(request.getSellerId(),
                request.getTitle(),
                request.getDescription(),
                Product.Location.of(request.getLatitude(), request.getLongitude(), request.getDetailAddress()),
                request.getPrice(),
                imageUrlList,
                request.getTagNameList(),
                request.getCondition());
    }

    private void validateSellerExists(ObjectId sellerId) {
        if (!userEntityRepository.existsById(sellerId)) {
            throw new IllegalArgumentException("Seller with ID " + sellerId + " does not exist");
        }
    }

    @Transactional
    public void delete(String productId) {
        ObjectId id = new ObjectId(productId);
        Product product = productEntityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("product not found."));

        List<String> tagNameList = product.getTagNameList();
        for (String tagName : tagNameList) {
            Tag tagEntity = tagEntityRepository.findById(tagName)
                    .orElseThrow(() -> new IllegalArgumentException("tag name not found."));
            if (tagEntity.getProductsCount() > 0) {
                tagEntityRepository.decrementProductsCount(tagName);
            }
        }
        productEntityRepository.delete(product);

        try (S3Client s3Client = awsS3Client.getS3Client()) {
            List<String> s3ObjectKeyList = product.getImageUrlList();
            awsS3Client.delete(s3Client, s3ObjectKeyList);
        }
    }
}
