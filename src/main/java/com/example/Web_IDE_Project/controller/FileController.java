package com.example.Web_IDE_Project.controller;

import com.example.Web_IDE_Project.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileController {

    private final String BASE_PATH = "/home/ubuntu/ide-workspace/";

    // 공통 보안 체크 메서드
    private void validatePath(String filePath) {
        Path path = Paths.get(filePath).normalize();
        if (!path.toString().startsWith(BASE_PATH)) {
            throw new SecurityException("접근 권한이 없는 경로입니다: " + filePath);
        }
    }

    @GetMapping("/tree")
    public List<FileNodeResponse> getFileTree(@RequestParam String userId) {
        // userId가 비어있거나 "나"인 경우 방어 로직
        if (userId == null || userId.trim().isEmpty() || "나".equals(userId)) {
            return Collections.emptyList();
        }

        File root = new File(BASE_PATH + userId);
        if (!root.exists()) root.mkdirs();
        return List.of(buildFileTree(root));
    }

    @GetMapping("/content")
    public ResponseEntity<String> getFileContent(@RequestParam String filePath) {
        try {
            validatePath(filePath); // 보안 체크
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(Files.readString(path));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("파일 읽기 실패");
        }
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveFile(@RequestBody SaveRequest request) {
        try {
            validatePath(request.getFilePath()); // 보안 체크
            Path path = Paths.get(request.getFilePath());
            Files.createDirectories(path.getParent());
            Files.writeString(path, request.getContent());
            return ResponseEntity.ok("저장 완료!");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("저장 중 오류 발생");
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeCode(@RequestBody ExecutionRequest request) {
        try {
            validatePath(request.getFilePath()); // 보안 체크
            StringBuilder output = new StringBuilder();
            String command = "";

            // 실행 시 절대 경로를 확실히 사용
            if ("java".equals(request.getLanguage())) {
                command = "java " + request.getFilePath();
            } else if ("python".equals(request.getLanguage())) {
                command = "python3 " + request.getFilePath();
            }

            Process process = Runtime.getRuntime().exec(command);

            // 표준 출력 읽기
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> output.append(line).append("\n"));
            }

            // 에러 출력 읽기
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                errorReader.lines().forEach(line -> output.append("[ERROR] ").append(line).append("\n"));
            }

            process.waitFor();
            return ResponseEntity.ok(new ExecutionResponse(output.toString()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ExecutionResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ExecutionResponse("실행 오류: " + e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> createFile(@RequestBody CreateRequest request) {
        try {
            Path path = Paths.get(request.getParentPath(), request.getName()).normalize();
            validatePath(path.toString()); // 보안 체크

            if ("folder".equals(request.getType())) {
                Files.createDirectories(path);
            } else {
                Files.createDirectories(path.getParent());
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
            }
            return ResponseEntity.ok("성공적으로 생성되었습니다!");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("생성 실패");
        }
    }

    private FileNodeResponse buildFileTree(File file) {
        return FileNodeResponse.builder()
                .name(file.getName())
                .type(file.isDirectory() ? "folder" : "file")
                .path(file.getAbsolutePath())
                .editable(true)
                .children(file.isDirectory() ?
                        Optional.ofNullable(file.listFiles())
                                .map(Arrays::stream)
                                .orElse(java.util.stream.Stream.empty())
                                .map(this::buildFileTree)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}