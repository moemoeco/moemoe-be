package com.moemoe.mongo.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductCondition {
    NEW("Brand new, unused product"),
    NO_SIGNS_OF_USE("Used but without visible marks or stains"),
    SLIGHT_SIGNS_OF_USE("Used with slight visible marks or stains"),
    HEAVY_SIGNS_OF_USE("Used with significant visible marks or stains"),
    DAMAGED("Product is damaged or requires repair");

    private final String description;
}
