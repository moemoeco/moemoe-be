package com.moemoe.mongo.entity;

import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.constant.ProductStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Document(collection = "products")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductEntity {
    @Id
    private ObjectId id;
    private ObjectId sellerId;
    private String title;
    private String description;
    private long price;
    private long viewCount;
    private Location location;
    private List<String> imageUrlList;
    private List<String> tagNameList;
    private ProductStatus status;
    private ProductCondition condition;

    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    public String getStringId(){
        return this.id.toHexString();
    }

    public String getDetailedAddress() {
        return this.location.detailedAddress;
    }

    public String getThumbnailUrl() {
        return this.imageUrlList.getFirst();
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
                          List<String> imageUrlList,
                          List<String> tagNameList,
                          ProductCondition condition) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.viewCount = 0;
        this.location = location;
        this.imageUrlList = ObjectUtils.isEmpty(imageUrlList) ? new ArrayList<>() : imageUrlList;
        this.tagNameList = ObjectUtils.isEmpty(tagNameList) ? new ArrayList<>() : tagNameList;
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
}
