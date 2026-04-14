package com.worldcup.hotelbooking.catalog.roomtypephoto.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderPhotosRequestDto {

    @NotEmpty
    private List<Long> photoIds;
}