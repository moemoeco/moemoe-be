package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.IdResponse;
import com.moemoe.mongo.entity.Product;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final UserEntityRepository userEntityRepository;
    private final ProductEntityRepository productEntityRepository;
    private final AwsS3Client awsS3Client;

    @Transactional
    public IdResponse register(RegisterProductRequest request,
                               List<MultipartFile> imageList) {
        validateSellerExists(request.getSellerId());

        List<String> imageUrlList = new ArrayList<>();
        try (S3Client s3Client = awsS3Client.getS3Client()) {
            for (MultipartFile image : imageList) {
                String imageUrl = awsS3Client.upload(s3Client, Path.of(request.getSellerId().toHexString(), getFileName(image)).toString(), image);
                imageUrlList.add(imageUrl);
            }
        } catch (Exception e) {
            throw new ClientRuntimeException(e.getMessage());
        }

        Product productEntity = createProductEntity(request, imageUrlList);
        return new IdResponse(productEntityRepository.save(productEntity).getId());
    }

    private String getFileName(MultipartFile image) {
        return Optional.ofNullable(image.getOriginalFilename())
                .map(fileName -> Path.of(fileName).getFileName().toString())
                .orElseThrow(() -> new IllegalArgumentException("파일 이름이 null 입니다."));
    }

    private Product createProductEntity(RegisterProductRequest request, List<String> imageUrlList) {
        return Product.of(request.getSellerId(),
                request.getTitle(),
                request.getDescription(),
                Product.Location.of(request.getLatitude(), request.getLongitude(), request.getDetailAddress()),
                request.getPrice(),
                imageUrlList,
                request.getTagIdList());
    }

    private void validateSellerExists(ObjectId sellerId) {
        if (!userEntityRepository.existsById(sellerId)) {
            throw new IllegalArgumentException("Seller with ID " + sellerId + " does not exist");
        }
    }
}
