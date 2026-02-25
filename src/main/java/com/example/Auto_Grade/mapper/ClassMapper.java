package com.example.Auto_Grade.mapper;

import com.example.Auto_Grade.dto.req.ClassRequest;


import com.example.Auto_Grade.entity.Class;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClassMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "classCode", ignore = true)
    @Mapping(target = "creator", ignore = true)
    void updateClassFromDTO(ClassRequest accountDTO, @MappingTarget Class classEntity);
}
