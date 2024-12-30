package com.moemoe.mongo.entity;

import com.moemoe.mongo.constant.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class Product {
    @Id
    private ObjectId id;
    @NotNull(message = "Seller Id must not be null")
    private ObjectId sellerId;
    @NotEmpty(message = "Title must not be empty")
    private String title;
    private String description;
    @Min(value = 1, message = "Price must be greater than 0")
    private long price;
    private long viewCount;
    private Location location;
    @Size(min = 1, max = 10, message = "Images must include at least 1 item and up to 10 items.")
    private List<String> imageUrlList;
    @Size(max = 5, message = "Tags can include up to 5 items only.")
    private List<String> tagIdList;
    private ProductStatus status;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    public static Product of(ObjectId sellerId,
                             String title,
                             String description,
                             Location location,
                             long price,
                             List<String> imageUrlList,
                             List<String> tagIdList) {
        return new Product(sellerId, title, description, price, location, imageUrlList, tagIdList);
    }

    private Product(ObjectId sellerId,
                    String title,
                    String description,
                    long price,
                    Location location,
                    List<String> imageUrlList,
                    List<String> tagIdList) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.viewCount = 0;
        this.location = location;
        this.imageUrlList = ObjectUtils.isEmpty(imageUrlList) ? new ArrayList<>() : imageUrlList;
        this.tagIdList = ObjectUtils.isEmpty(tagIdList) ? new ArrayList<>() : tagIdList;
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
