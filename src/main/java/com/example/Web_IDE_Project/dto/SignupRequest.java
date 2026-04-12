package com.example.Web_IDE_Project.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String userId;
    private String password;
    private String name;
}