package com.example.Web_IDE_Project.controller;

import com.example.Web_IDE_Project.dto.*; // DTO 패키지 확인 필요
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

    @GetMapping("/tree")
    public List<FileNodeResponse> getFileTree(@RequestParam String userId) {
        File root = new File(BASE_PATH + userId);
        if (!root.exists()) root.mkdirs();
        return List.of(buildFileTree(root));
    }

    @GetMapping("/content")
    public ResponseEntity<String> getFileContent(@RequestParam String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Files.readString(path));
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveFile(@RequestBody SaveRequest request) throws IOException {
        Path path = Paths.get(request.getFilePath());
        Files.createDirectories(path.getParent()); // 폴더가 없으면 생성
        Files.writeString(path, request.getContent());
        return ResponseEntity.ok("저장 완료!");
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeCode(@RequestBody ExecutionRequest request) {
        StringBuilder output = new StringBuilder();
        String command = "";

        if ("java".equals(request.getLanguage())) {
            command = "java " + request.getFilePath();
        } else if ("python".equals(request.getLanguage())) {
            command = "python3 " + request.getFilePath();
        }

        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                output.append("[ERROR] ").append(line).append("\n");
            }

            process.waitFor();
            return ResponseEntity.ok(new ExecutionResponse(output.toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ExecutionResponse("실행 중 오류 발생: " + e.getMessage()));
        }
    }

    private FileNodeResponse buildFileTree(File file) {
        return FileNodeResponse.builder()
                .name(file.getName())
                .type(file.isDirectory() ? "folder" : "file")
                .path(file.getAbsolutePath())
                .editable(true)
                .children(file.isDirectory() ?
                        Arrays.stream(Objects.requireNonNull(file.listFiles()))
                                .map(this::buildFileTree)
                                .collect(Collectors.toList()) : null)
                .build();
    }
    @PostMapping("/create")
    public ResponseEntity<String> createFile(@RequestBody CreateRequest request) throws IOException {
        Path path = Paths.get(request.getParentPath(), request.getName()).normalize();
        String normalizedPath = path.toString();

        if (!normalizedPath.contains("ide-workspace")) {
            System.out.println("보안 위반 의심 경로: " + normalizedPath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("접근 권한이 없는 경로입니다.");
        }

        if ("folder".equals(request.getType())) {
            Files.createDirectories(path);
        } else {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        }
        return ResponseEntity.ok("성공적으로 생성되었습니다!");
    }
}