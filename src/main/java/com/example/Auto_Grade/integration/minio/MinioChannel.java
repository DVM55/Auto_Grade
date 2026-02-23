package com.example.Auto_Grade.integration.minio;

import com.example.Auto_Grade.service.RedisService;
import io.minio.*;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioChannel {

    private final MinioProps props;
    private final MinioClient minioClient;
    private final RedisService redisService;

    @PostConstruct
    private void init() {
        try {
            createBucketIfNeeded(props.getBucket(), props.isMakeBucketPublic());
        } catch (Exception e) {
            throw new RuntimeException("Không thể kiểm tra/tạo bucket " + props.getBucket(), e);
        }
    }

    private void createBucketIfNeeded(final String name, boolean makePublic) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(name).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
            if (makePublic) {
                final var policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [{
                            "Effect": "Allow",
                            "Principal": "*",
                            "Action": "s3:GetObject",
                            "Resource": "arn:aws:s3:::%s/*"
                          }]
                        }
                        """.formatted(name);
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(name).config(policy).build()
                );
            }
        }
    }

    /** Sinh objectKey duy nhất: {prefix/}{uuid}-{filename} */
    public String buildObjectKey(String originalName) {
        String safeName = (originalName == null || originalName.isBlank())
                ? "file.bin"
                : originalName.strip().replace("\\", "/");
        String nameOnly = safeName.substring(safeName.lastIndexOf('/') + 1);

        String prefix = props.getKeyPrefix() == null ? "" : props.getKeyPrefix().trim();
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }
        return prefix + UUID.randomUUID() + "-" + nameOnly;
    }

    /**
     * Upload trực tiếp file từ BE lên MinIO và trả về objectKey + URL trong Map.
     */
    public Map<String, String> uploadFile(
            String originalName,
            InputStream inputStream,
            long size,
            String contentType,
            int ttlSeconds
    ) throws Exception {

        // Tạo objectKey duy nhất
        String objectKey = buildObjectKey(originalName);

        // Upload thẳng lên MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(props.getBucket())
                        .object(objectKey)
                        .stream(inputStream, size, -1)
                        .contentType(contentType)
                        .build()
        );

        // Tạo Presigned GET URL
        String url = getPresignedUrlSafe(objectKey, ttlSeconds);

        return Map.of(
                "objectKey", objectKey,
                "url", url
        );
    }


    /** Presigned URL PUT (FE upload trực tiếp file lên MinIO) */
    public String presignedPutUrl(String objectKey, int ttlSeconds, String contentType) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(io.minio.http.Method.PUT)
                        .bucket(props.getBucket())
                        .object(objectKey)
                        .expiry(ttlSeconds > 0 ? ttlSeconds : props.getPresignExpirySeconds())
                        .extraHeaders(Map.of("Content-Type", contentType)) // Ép bắt buộc FE phải upload đúng loại file
                        .build()
        );
    }

    /** Presigned URL GET (FE tải file từ MinIO) */
    public String presignedGetUrl(String objectKey, int ttlSeconds) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(io.minio.http.Method.GET)
                        .bucket(props.getBucket())
                        .object(objectKey)
                        .expiry(ttlSeconds > 0 ? ttlSeconds : props.getPresignExpirySeconds())
                        .build()
        );
    }

    public String getPresignedUrlSafe(String objectKey, int ttlSeconds) {

        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String redisKey = "presigned:" + objectKey;

        String cachedUrl = (String) redisService.get(redisKey);
        if (cachedUrl != null) {
            return cachedUrl;
        }

        try {
            String url = presignedGetUrl(objectKey, ttlSeconds);

            redisService.setWithTTL(redisKey, url, ttlSeconds);

            return url;
        } catch (Exception e) {
            return null;
        }
    }




}
