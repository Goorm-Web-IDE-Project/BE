package com.example.Web_IDE_Project.controller;

import com.example.Web_IDE_Project.dto.*; // DTO 패키지 확인 필요
import lombok.RequiredArgsConstructor;
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

    // 1. 파일 트리 가져오기
    @GetMapping("/tree")
    public List<FileNodeResponse> getFileTree(@RequestParam String userId) {
        File root = new File(BASE_PATH + userId);
        if (!root.exists()) root.mkdirs();
        return List.of(buildFileTree(root));
    }

    // 2. 파일 내용 읽기
    @GetMapping("/content")
    public ResponseEntity<String> getFileContent(@RequestParam String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Files.readString(path));
    }

    // 3. 파일 저장하기
    @PostMapping("/save")
    public ResponseEntity<String> saveFile(@RequestBody SaveRequest request) throws IOException {
        Path path = Paths.get(request.getFilePath());
        Files.createDirectories(path.getParent()); // 폴더가 없으면 생성
        Files.writeString(path, request.getContent());
        return ResponseEntity.ok("저장 완료!");
    }

    // 4. 코드 실행 (핵심!)
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

            // 표준 출력 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 에러 출력 읽기
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
}