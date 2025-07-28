package com.moemoe.api.validation.annotation;

import com.moemoe.api.validation.validator.ImageContentTypeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageContentTypeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageContentType {
    String message() default "Only image content types (jpeg, png) are allowed.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
