package com.moemoe.mongo.entity;

import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.constant.ProductStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Document(collection = "products")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductEntity extends BaseTimeEntity {
    @Id
    private ObjectId id;
    private ObjectId sellerId;
    private String title;
    private String description;
    private long price;
    private long viewCount;
    private Location location;
    private List<String> imageKeys;
    private List<String> tagNames;
    private ProductStatus status;
    private ProductCondition condition;

    public String getStringId() {
        return this.id.toHexString();
    }

    public String getDetailedAddress() {
        return this.location.detailedAddress;
    }

    public String getThumbnailUrl() {
        return this.imageKeys.getFirst();
    }

    public static ProductEntity of(ObjectId sellerId,
                                   String title,
                                   String description,
                                   Location location,
                                   long price,
                                   List<String> imageUrlList,
                                   List<String> tagNameList,
                                   ProductCondition condition) {
        return new ProductEntity(sellerId, title, description, price, location, imageUrlList, tagNameList, condition);
    }

    private ProductEntity(ObjectId sellerId,
                          String title,
                          String description,
                          long price,
                          Location location,
                          List<String> imageKeys,
                          List<String> tagNames,
                          ProductCondition condition) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.viewCount = 0;
        this.location = location;
        this.imageKeys = ObjectUtils.isEmpty(imageKeys) ? new ArrayList<>() : imageKeys;
        this.tagNames = ObjectUtils.isEmpty(tagNames) ? new ArrayList<>() : tagNames;
        this.condition = condition;
        this.status = ProductStatus.ON_SALE;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Location {
        private double latitude;
        private double longitude;
        private String detailedAddress;

        private Location(double latitude, double longitude, String detailedAddress) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.detailedAddress = detailedAddress;
        }

        public static Location of(double latitude, double longitude, String detailedAddress) {
            return new Location(latitude, longitude, detailedAddress);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProductEntity that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
