package com.moemoe.api.controller;


import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IdResponse register(
            @Valid @RequestPart(value = "request") RegisterProductRequest request,
            @Valid @Size(min = 1, max = 10, message = "Images must include at least 1 item and up to 10 items.") @RequestPart(value = "imageList") List<MultipartFile> imageList
    ) {
        return productService.register(request, imageList);
    }
}
