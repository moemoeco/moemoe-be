package com.moemoe.core.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

@Getter
@NoArgsConstructor
public class IdResponse {
    private String id;

    public IdResponse(ObjectId objectId) {
        this.id = objectId.toHexString();
    }
}
