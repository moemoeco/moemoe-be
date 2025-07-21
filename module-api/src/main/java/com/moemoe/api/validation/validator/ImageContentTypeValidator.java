package com.moemoe.api.validation.validator;

import com.moemoe.api.validation.annotation.ImageContentType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;

public class ImageContentTypeValidator implements ConstraintValidator<ImageContentType, MediaType> {

    @Override
    public boolean isValid(MediaType value, ConstraintValidatorContext context) {
        if (ObjectUtils.isEmpty(value)) {
            return false;
        }

        try {
            return MediaType.IMAGE_JPEG.includes(value)
                    || MediaType.IMAGE_PNG.includes(value);
        } catch (IllegalArgumentException e) {
            return false; // 유효하지 않은 MIME 타입
        }
    }
}
