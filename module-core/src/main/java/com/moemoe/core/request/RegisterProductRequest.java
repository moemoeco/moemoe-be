package com.moemoe.core.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;

import java.util.List;

@Getter
public class RegisterProductRequest {
    @NotNull(message = "Seller Id must not be null")
    @Getter(AccessLevel.NONE)
    private String sellerId;
    @NotEmpty(message = "Title must not be empty")
    private String title;
    private String description;
    @NotNull
    @Getter(AccessLevel.NONE)
    private Location location;
    @Min(value = 0)
    private long price;
    @Size(max = 5, message = "Tags can include up to 5 items only.")
    private List<String> tagIdList;

    public double getLatitude() {
        return this.location.getLatitude();
    }

    public double getLongitude() {
        return this.location.getLongitude();
    }

    public String getDetailAddress() {
        return this.location.getDetailAddress();
    }

    public ObjectId getSellerId() {
        return new ObjectId(this.sellerId);
    }

    @Getter
    @RequiredArgsConstructor
    private static class Location {
        private double latitude;
        private double longitude;
        private String detailAddress;
    }
}
