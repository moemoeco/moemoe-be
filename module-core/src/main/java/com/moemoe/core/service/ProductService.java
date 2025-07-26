package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.request.RegisterProductServiceRequest;
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
    private final TagService tagService;
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
        return productEntityList.stream()
                .map(product -> GetProductsResponse.Product.builder()
                        .id(product.getStringId())
                        .title(product.getTitle())
                        .detailedAddress(product.getDetailedAddress())
                        .price(product.getPrice())
                        .tagIdList(product.getTagNames())
                        .thumbnailUrl(awsS3Client.getPreSignedUrl(product.getThumbnailUrl()))
                        .createAt(product.getCreatedAt())
                        .build())
                .toList();
    }

    private boolean invalidProductId(String oldNextId) {
        return !productEntityRepository.existsById(new ObjectId(oldNextId));
    }

    @Transactional
    public IdResponse register(RegisterProductServiceRequest request) {
        ObjectId sellerId = SecurityContextHolderUtils.getUserId();
        log.info("Product registration started: sellerId={}, title={}",
                sellerId, request.title());
        try {
            validateSellerExists(sellerId);
            tagService.incrementProductsCount(request.tagNames());

            ProductEntity productEntity = request.toEntity(sellerId);
            ProductEntity savedEntity = productEntityRepository.save(productEntity);

            log.info("Product registration completed: productId={}", savedEntity.getId());
            return new IdResponse(savedEntity.getId());
        } catch (Exception ex) {
            log.error("Product registration failed: sellerId={}, title={}",
                    sellerId, request.title(), ex);
            throw ex;
        }
    }

    private void validateSellerExists(ObjectId sellerId) {
        if (!userEntityRepository.existsById(sellerId)) {
            throw new IllegalArgumentException("Seller with ID " + sellerId + " does not exist");
        }
    }

    @Transactional
    @Deprecated(forRemoval = true)
    public IdResponse register(RegisterProductRequest request,
                               List<MultipartFile> imageList) {
        ObjectId sellerId = SecurityContextHolderUtils.getUserId();
        validateSellerExists(sellerId);

        String title = slugifyTitle(request.getTitle());

        List<String> imageUrlList = new ArrayList<>();
        for (MultipartFile image : imageList) {
            String imageUrl = awsS3Client.upload(Path.of(sellerId.toHexString(), title, getFileName(image)).toString(), image);
            imageUrlList.add(imageUrl);
        }

        incrementTag(request);
        ProductEntity productEntity = createProductEntity(sellerId, request, imageUrlList);
        return new IdResponse(productEntityRepository.save(productEntity).getId());
    }

    @Deprecated(forRemoval = true)
    private String slugifyTitle(String title) {
        if (title == null || title.isEmpty()) {
            return "untitled";
        }
        return title
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "")
                .replaceAll("[^\\w\\s-\\uAC00-\\uD7A3]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }

    @Deprecated(forRemoval = true)
    private void incrementTag(RegisterProductRequest request) {
        for (String tagName : request.getTagNameList()) {
            Optional<TagEntity> optionalTag = tagEntityRepository.findTagEntityByName(tagName);
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

    @Deprecated(forRemoval = true)
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

    @Transactional
    public void delete(String productId) {
        ObjectId objectId = new ObjectId(productId);
        Optional<ProductEntity> optionalProduct = productEntityRepository.findById(objectId);
        if (optionalProduct.isEmpty()) {
            return;
        }

        ProductEntity productEntity = optionalProduct.get();
        List<String> tagNameList = productEntity.getTagNames();
        List<TagEntity> tagEntityEntityList = tagEntityRepository.findAllByNameIn(tagNameList);

        for (TagEntity tagEntity : tagEntityEntityList) {
            if (tagEntity.getProductsCount() > 0) {
                tagEntityRepository.decrementProductsCount(tagEntity.getName());
            }
        }
        productEntityRepository.delete(productEntity);

        List<String> s3ObjectKeyList = productEntity.getImageKeys();
        awsS3Client.delete(s3ObjectKeyList);
    }
}
