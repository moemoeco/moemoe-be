package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.core.request.RegisterProductServiceRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.security.SecurityContextHolderUtils;
import com.moemoe.mongo.entity.ProductEntity;
import com.moemoe.mongo.entity.TagEntity;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.TagEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final UserEntityRepository userEntityRepository;
    private final ProductEntityRepository productEntityRepository;
    private final TagEntityRepository tagEntityRepository;
    private final TagService tagService;
    private final AwsS3Client awsS3Client;

    public GetProductsResponse findAll(String nextId, int requestedPageSize) {
        log.debug("findAll called: nextId='{}', requestedPageSize={}", nextId, requestedPageSize);

        int pageSize = normalizePageSize(requestedPageSize);
        log.debug("Normalized pageSize={}", pageSize);

        if (StringUtils.isNotBlank(nextId) && !ObjectId.isValid(nextId)) {
            log.warn("Rejecting request: invalid nextId format (not hex ObjectId), nextId='{}'", nextId);
            throw new IllegalArgumentException("nextId is invalid hex ObjectId");
        }

        log.debug("Querying repository: findPage(nextId='{}', pageSize={})", nextId, pageSize);
        List<ProductEntity> entities = productEntityRepository.findPage(nextId, pageSize);
        log.debug("Repository returned {} entities", entities.size());

        // 로깅용 페이지 메타(실제 계산은 GetProductsResponse가 하더라도 참고용으로 남김)
        boolean hasNextCandidate = entities.size() > pageSize;
        String nextIdCandidate = hasNextCandidate ? entities.get(pageSize - 1).getStringId() : "";
        log.debug("Computed meta (for log): hasNextCandidate={}, nextIdCandidate='{}'",
                hasNextCandidate, nextIdCandidate);

        List<GetProductsResponse.Product> contents = entities.stream()
                .map(this::toDto)
                .toList();

        GetProductsResponse response = new GetProductsResponse(contents, pageSize);
        log.debug("Returning response: contents.size={}, hasNext={}, nextId='{}'",
                response.getContents().size(), response.isHasNext(), response.getNextId());
        return response;
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            log.debug("Requested pageSize {} is non-positive. Using DEFAULT_SIZE={}", size, DEFAULT_SIZE);
            return DEFAULT_SIZE;
        }
        int normalized = Math.min(size, MAX_SIZE);
        if (normalized != size) {
            log.debug("Requested pageSize {} exceeds MAX_SIZE={}. Using normalized={}", size, MAX_SIZE, normalized);
        }
        return normalized;
    }

    private GetProductsResponse.Product toDto(ProductEntity e) {
        GetProductsResponse.Product dto = new GetProductsResponse.Product(
                e.getStringId(),
                e.getTitle(),
                e.getTagNames(),
                // TODO 2025-09-15: 썸네일 매핑 로직 교체 예정
                "",
                e.getDetailedAddress(),
                e.getPrice(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
        log.trace("Mapped entity to dto: id='{}', title='{}'", e.getStringId(), e.getTitle());
        return dto;
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
