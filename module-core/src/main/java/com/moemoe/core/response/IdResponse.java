package com.moemoe.core.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.Objects;

@Getter
@NoArgsConstructor
public class IdResponse {
    private String id;

    public IdResponse(ObjectId objectId) {
        this.id = objectId.toHexString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IdResponse response = (IdResponse) o;
        return Objects.equals(getId(), response.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
