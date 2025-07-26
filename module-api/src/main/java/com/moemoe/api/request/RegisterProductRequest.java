package com.moemoe.api.request;

import com.moemoe.core.request.RegisterProductServiceRequest;
import com.moemoe.mongo.constant.ProductCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;


public record RegisterProductRequest(
        @NotBlank(message = "title must not be blank.")
        String title,
        String description,
        @Valid
        @NotNull(message = "location must not be null.")
        LocationRequest location,
        @Min(value = 0, message = "price must be greater than or equal to 0.")
        Long price,
        @NotNull(message = "imageKeys must not be null.")
        @Size(min = 1, max = 10, message = "imageKeys must contain between 1 and 10 items.")
        List<String> imageKeys,
        @NotNull
        @Size(max = 5, message = "tagNames must not contain more than 5 items.")
        List<String> tagNames,
        @NotNull(message = "productCondition must not be null.")
        ProductCondition productCondition) {

    public RegisterProductServiceRequest toServiceRequest() {
        return new RegisterProductServiceRequest(
                title,
                description,
                price,
                tagNames,
                imageKeys,
                location.toDto(),
                productCondition
        );
    }

    public record LocationRequest(
            @NotNull(message = "latitude must not be null.")
            @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90.0.")
            @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90.0.")
            Double latitude,
            @NotNull(message = "longitude must not be null")
            @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180.0.")
            @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180.0.")
            Double longitude,
            @NotBlank(message = "detailAddress must not be blank.")
            String detailAddress
    ) {
        public RegisterProductServiceRequest.LocationDto toDto() {
            return new RegisterProductServiceRequest.LocationDto(latitude, longitude, detailAddress);
        }
    }
}
