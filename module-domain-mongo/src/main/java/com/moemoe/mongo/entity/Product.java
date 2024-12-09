package com.moemoe.mongo.entity;

import com.moemoe.mongo.constant.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
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
    private String id;
    @NotNull(message = "Seller Id must not be null")
    private ObjectId sellerId;
    @NotEmpty(message = "Title must not be empty")
    private String title;
    private String description;
    @Min(value = 1, message = "Price must be greater than 0")
    private long price;
    private String location;
    @Size(max = 10, message = "Images can include up to 10 items only.")
    private List<String> imageUrlList;
    @Size(max = 10, message = "Tags can include up to 10 items only.")
    private List<String> tagIdList;
    private ProductStatus status;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @Builder
    public Product(ObjectId sellerId,
                   String title,
                   String description,
                   long price,
                   String location,
                   List<String> imageUrlList,
                   List<String> tagIdList) {
        this.sellerId = sellerId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.location = location;
        this.imageUrlList = ObjectUtils.isEmpty(imageUrlList) ? new ArrayList<>() : imageUrlList;
        this.tagIdList = ObjectUtils.isEmpty(tagIdList) ? new ArrayList<>() : tagIdList;
        this.status = ProductStatus.ON_SALE;
    }
}
