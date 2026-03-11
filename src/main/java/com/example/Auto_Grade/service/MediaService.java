package com.example.Auto_Grade.service;


import com.example.Auto_Grade.dto.req.MediaRequest;
import com.example.Auto_Grade.dto.req.UpdateMediaRequest;

import com.example.Auto_Grade.dto.res.MediaResponse;
import com.example.Auto_Grade.dto.res.PagingResponse;


import java.util.List;

public interface MediaService {
    void deleteMediaById(Long mediaId);

    void createMedia(List<MediaRequest> requests);

    void updateMedia(UpdateMediaRequest request, Long mediaId);

    PagingResponse<MediaResponse> getMediasByCreator( String fileName, int page, int size);

    void deleteAllMediaByCreator();
}
