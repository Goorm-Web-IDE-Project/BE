package com.example.Web_IDE_Project.controller;

import com.example.Web_IDE_Project.config.JwtTokenProvider;
import com.example.Web_IDE_Project.domain.Role;
import com.example.Web_IDE_Project.domain.User;
import com.example.Web_IDE_Project.dto.SignupRequest;
import com.example.Web_IDE_Project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public String signup(@RequestBody SignupRequest request) {
        User user = User.builder()
                .userId(request.getUserId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
        return "회원가입 성공!";
    }

    @PostMapping("/login")
    public String login(@RequestBody SignupRequest request) {
        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 아이디입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return jwtTokenProvider.createToken(user.getUserId());
    }
}