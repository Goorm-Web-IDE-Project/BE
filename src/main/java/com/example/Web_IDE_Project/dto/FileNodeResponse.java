package com.example.Web_IDE_Project.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class FileNodeResponse {
    private String name;
    private String type;
    private boolean editable;
    private String path;
    private List<FileNodeResponse> children;
}