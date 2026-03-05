package com.example.Auto_Grade.service;


import com.example.Auto_Grade.dto.req.MediaRequest;
import com.example.Auto_Grade.dto.req.UpdateMediaRequest;

import com.example.Auto_Grade.dto.res.MediaResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MediaService {
    void deleteMediaById(Long mediaId);

    void createMedia(List<MediaRequest> requests);

    void updateMedia(UpdateMediaRequest request, Long mediaId);

    Page<MediaResponse> getMedias(Long accountId, String fileName, int page, int size);
}
