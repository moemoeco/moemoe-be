package com.moemoe.api.controller;


import com.moemoe.api.request.ProductPresignedUrlRequest;
import com.moemoe.api.request.RegisterProductRequest;
import com.moemoe.api.response.ProductPresignedUrlResponse;
import com.moemoe.core.request.GeneratePresignedUrlServiceRequest;
import com.moemoe.core.request.RegisterProductServiceRequest;
import com.moemoe.core.response.GeneratePresignedUrlServiceResponse;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.service.PresignedUrlService;
import com.moemoe.core.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final ProductService productService;
    private final PresignedUrlService presignedUrlService;

    @PostMapping(value = "/presigned-urls")
    public ProductPresignedUrlResponse generatePresignedUrl(
            @Valid @RequestBody ProductPresignedUrlRequest request
    ) {
        GeneratePresignedUrlServiceRequest serviceRequest = request.toServiceRequest();
        GeneratePresignedUrlServiceResponse serviceResponse = presignedUrlService.generatePresignedUrl(serviceRequest);
        return ProductPresignedUrlResponse.fromServiceResponse(serviceResponse);
    }

    @GetMapping
    public GetProductsResponse findAll(
            @RequestParam(value = "nextId", required = false) String nextId,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") @Min(1) @Max(50) Integer pageSize
    ) {
        return productService.findAll(nextId, pageSize);
    }

    @PostMapping
    public IdResponse register(
            @Valid @RequestBody RegisterProductRequest request
    ) {
        RegisterProductServiceRequest serviceRequest = request.toServiceRequest();
        return productService.register(serviceRequest);
    }

    @DeleteMapping
    public void delete(
            @RequestParam("productId") String productId
    ) {
        productService.delete(productId);
    }
}
