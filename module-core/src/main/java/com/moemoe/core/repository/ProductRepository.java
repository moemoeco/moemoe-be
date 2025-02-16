package com.moemoe.core.repository;


import com.moemoe.mongo.entity.Product;
import com.moemoe.mongo.entity.Tag;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.TagEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductRepository {
    private final ProductEntityRepository productEntityRepository;
    private final TagEntityRepository tagEntityRepository;

    @Transactional
    public void delete(Product productEntity) {
        List<String> tagNameList = productEntity.getTagNameList();
        for (String tagName : tagNameList) {
            Tag tagEntity = tagEntityRepository.findById(tagName)
                    .orElseThrow(() -> new IllegalArgumentException("tag name not found."));
            if (tagEntity.getProductsCount() > 0) {
                tagEntityRepository.decrementProductsCount(tagName);
            }
        }

        productEntityRepository.delete(productEntity);
    }
}
