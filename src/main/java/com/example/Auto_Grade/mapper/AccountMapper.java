package com.example.Auto_Grade.mapper;


import com.example.Auto_Grade.dto.req.UpdateAccountRequest;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.entity.UserDetail;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateAccountFromDTO(UpdateAccountRequest accountDTO, @MappingTarget Account account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    void updateUserDetailFromDTO(UpdateAccountRequest userDetailDto, @MappingTarget UserDetail userDetail);

}
