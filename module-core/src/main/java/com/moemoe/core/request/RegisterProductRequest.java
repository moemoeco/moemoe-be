package com.moemoe.core.request;

import com.moemoe.mongo.constant.ProductCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Objects;

@Getter
public class RegisterProductRequest {
    @NotEmpty(message = "Seller Id must not be empty")
    @Getter(AccessLevel.NONE)
    private String sellerId;
    @NotEmpty(message = "Title must not be empty")
    private String title;
    private String description;
    @NotNull
    private Location location;
    @Min(value = 0)
    private long price;
    @NotNull
    @Size(max = 5, message = "Tags can include up to 5 items only.")
    private List<String> tagIdList;
    @NotNull
    private ProductCondition condition;

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
    public static class Location {
        private double latitude;
        private double longitude;
        private String detailAddress;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Location location = (Location) o;
            return Double.compare(getLatitude(), location.getLatitude()) == 0 && Double.compare(getLongitude(), location.getLongitude()) == 0 && Objects.equals(getDetailAddress(), location.getDetailAddress());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getLatitude(), getLongitude(), getDetailAddress());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RegisterProductRequest request = (RegisterProductRequest) o;
        return getPrice() == request.getPrice() && Objects.equals(getSellerId(), request.getSellerId()) && Objects.equals(getTitle(), request.getTitle()) && Objects.equals(getDescription(), request.getDescription()) && Objects.equals(getLocation(), request.getLocation()) && Objects.equals(getTagIdList(), request.getTagIdList()) && getCondition() == request.getCondition();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSellerId(), getTitle(), getDescription(), getLocation(), getPrice(), getTagIdList(), getCondition());
    }
}
