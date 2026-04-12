package com.example.Web_IDE_Project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ExecutionRequest {
    private String filePath;
    private String language;
}