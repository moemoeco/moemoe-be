package com.moemoe.core.request;

import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.entity.ProductEntity;
import org.bson.types.ObjectId;

import java.util.List;

public record RegisterProductServiceRequest(
        String title,
        String description,
        long price,
        List<String> tagNames,
        List<String> fileKeys,
        LocationDto location,
        ProductCondition productCondition
) {
    public ProductEntity toEntity(ObjectId sellerId) {
        return ProductEntity.of(
                sellerId,
                title,
                description,
                ProductEntity.Location.of(location.latitude, location.longitude, location.detailAddress),
                price,
                fileKeys,
                tagNames,
                productCondition);
    }

    public record LocationDto(
            double latitude,
            double longitude,
            String detailAddress
    ) {
    }
}
