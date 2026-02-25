package com.example.Auto_Grade.integration.minio;

import com.example.Auto_Grade.enums.FileType;
import com.example.Auto_Grade.integration.minio.dto.PresignPutRequest;
import com.example.Auto_Grade.integration.minio.dto.UploadResult;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {
    private final MinioChannel minioChannel;
    private final MinioProps props;

    /**
     * FE gửi danh sách file (filename + contentType)
     * BE trả về danh sách presigned PUT URLs để FE upload trực tiếp lên MinIO
     */
    @PostMapping("/upload-urls")
    public List<UploadResult> getUploadUrls(@RequestBody List<PresignPutRequest> files,
                                            @RequestParam(defaultValue = "0") int ttlSeconds) throws Exception {
        int ttl = ttlSeconds > 0 ? ttlSeconds : props.getPresignExpirySeconds();
        List<UploadResult> result = new ArrayList<>();

        for (PresignPutRequest file : files) {
            String objectKey = minioChannel.buildObjectKey(file.getFileName());
            String url = minioChannel.presignedPutUrl(objectKey, ttl, file.getContentType());

            FileType fileType = mapContentTypeToFileType(file.getContentType());

            result.add(UploadResult.builder()
                    .objectKey(objectKey)
                    .uploadUrl(url)
                    .fileType(fileType)
                    .fileName(file.getFileName())
                    .contentType(file.getContentType())
                    .fileSize(file.getFileSize())
                    .build()
            );
        }
        return result;
    }

    /**
     *
     *
     *
     *
     * Hàm map contentType → FileType (IMAGE, FILE, NONE)
     */
    public static FileType mapContentTypeToFileType(String contentType) {
        if (contentType.startsWith("image/")) return FileType.IMAGE;
        if (contentType.startsWith("video/")) return FileType.VIDEO;

        // các file còn lại xem như FILE
        return FileType.FILE;
    }

}
