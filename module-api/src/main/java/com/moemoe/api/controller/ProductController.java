package com.moemoe.api.controller;


import com.moemoe.api.request.ProductPresignedUrlRequest;
import com.moemoe.api.response.ProductPresignedUrlResponse;
import com.moemoe.core.request.GeneratePresignedUrlServiceRequest;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.GeneratePresignedUrlServiceResponse;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.service.PresignedUrlService;
import com.moemoe.core.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
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
            @RequestParam("nextId") String nextId,
            @RequestParam("pageSize") int pageSize
    ) {
        return productService.findAll(nextId, pageSize);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IdResponse register(
            @Valid @RequestPart(value = "request") RegisterProductRequest request,
            @Valid @Size(max = 10, message = "Images must include at least 1 item and up to 10 items.") @RequestPart(value = "imageList") List<MultipartFile> imageList
    ) {
        return productService.register(request, imageList);
    }

    @DeleteMapping
    public void delete(
            @RequestParam("productId") String productId
    ) {
        productService.delete(productId);
    }
}
