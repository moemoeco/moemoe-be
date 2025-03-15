package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.security.SecurityContextHolderUtils;
import com.moemoe.mongo.entity.ProductEntity;
import com.moemoe.mongo.entity.TagEntity;
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

        List<ProductEntity> productEntityList = productEntityRepository.findAll(oldNextId, pageSize);
        List<GetProductsResponse.Product> contents = getProductContents(productEntityList);
        return new GetProductsResponse(contents, pageSize);
    }

    private List<GetProductsResponse.Product> getProductContents(List<ProductEntity> productEntityList) {
        try (S3Presigner s3Presigner = awsS3Client.getS3Presigner();) {
            return productEntityList.stream()
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
        ObjectId sellerId = SecurityContextHolderUtils.getUserId();
        validateSellerExists(sellerId);
        List<String> imageUrlList = new ArrayList<>();
        try (S3Client s3Client = awsS3Client.getS3Client()) {
            for (MultipartFile image : imageList) {
                String imageUrl = awsS3Client.upload(s3Client, Path.of(sellerId.toHexString(), getFileName(image)).toString(), image);
                imageUrlList.add(imageUrl);
            }
        } catch (Exception e) {
            throw new ClientRuntimeException(e.getMessage());
        }

        incrementTag(request);
        ProductEntity productEntity = createProductEntity(sellerId, request, imageUrlList);
        return new IdResponse(productEntityRepository.save(productEntity).getId());
    }

    private void incrementTag(RegisterProductRequest request) {
        for (String tagName : request.getTagNameList()) {
            Optional<TagEntity> optionalTag = tagEntityRepository.findById(tagName);
            if (optionalTag.isPresent()) {
                tagEntityRepository.incrementProductsCount(tagName);
            } else {
                tagEntityRepository.save(TagEntity.of(tagName, 1L));
            }
        }
    }

    private String getFileName(MultipartFile image) {
        return Optional.ofNullable(image.getOriginalFilename())
                .map(fileName -> Path.of(fileName).getFileName().toString())
                .orElseThrow(() -> new IllegalArgumentException("파일 이름이 null 입니다."));
    }

    private ProductEntity createProductEntity(ObjectId sellerId, RegisterProductRequest request, List<String> imageUrlList) {
        return ProductEntity.of(
                sellerId,
                request.getTitle(),
                request.getDescription(),
                ProductEntity.Location.of(request.getLatitude(), request.getLongitude(), request.getDetailAddress()),
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
        ObjectId objectId = new ObjectId(productId);
        Optional<ProductEntity> optionalProduct = productEntityRepository.findById(objectId);
        if (optionalProduct.isEmpty()) {
            return;
        }

        ProductEntity productEntity = optionalProduct.get();
        List<String> tagNameList = productEntity.getTagNameList();
        List<TagEntity> tagEntityEntityList = tagEntityRepository.findAllById(tagNameList);

        for (TagEntity tagEntity : tagEntityEntityList) {
            if (tagEntity.getProductsCount() > 0) {
                tagEntityRepository.decrementProductsCount(tagEntity.getName());
            }
        }
        productEntityRepository.delete(productEntity);

        try (S3Client s3Client = awsS3Client.getS3Client()) {
            List<String> s3ObjectKeyList = productEntity.getImageUrlList();
            awsS3Client.delete(s3Client, s3ObjectKeyList);
        } catch (ClientRuntimeException e) {
            log.info("Failed to delete product images.");
        }
    }
}
