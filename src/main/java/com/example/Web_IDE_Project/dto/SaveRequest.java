package com.example.Web_IDE_Project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SaveRequest {
    private String filePath;
    private String content;
}