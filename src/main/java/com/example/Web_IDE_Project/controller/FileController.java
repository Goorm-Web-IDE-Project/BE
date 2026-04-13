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

    // 공통 보안 체크 및 경로 정규화 메서드
    private void validatePath(String filePath) {
        String normalized = Paths.get(filePath).normalize().toString().replace("\\", "/");
        String base = Paths.get(BASE_PATH).normalize().toString().replace("\\", "/");

        if (!normalized.startsWith(base)) {
            throw new SecurityException("접근 권한이 없는 경로입니다: " + normalized);
        }
    }

    @GetMapping("/tree")
    public List<FileNodeResponse> getFileTree(@RequestParam String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        File root = new File(BASE_PATH + userId);
        if (!root.exists()) root.mkdirs();

        return List.of(buildFileTree(root));
    }

    @GetMapping("/content")
    public ResponseEntity<String> getFileContent(@RequestParam String filePath) {
        try {
            validatePath(filePath);
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(Files.readString(path));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("파일 읽기 실패");
        }
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveFile(@RequestBody SaveRequest request) {
        try {
            validatePath(request.getFilePath());
            Path path = Paths.get(request.getFilePath());
            Files.createDirectories(path.getParent());
            Files.writeString(path, request.getContent());
            return ResponseEntity.ok("저장 완료!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("저장 중 오류 발생");
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecutionResponse> executeCode(@RequestBody ExecutionRequest request) {
        try {
            validatePath(request.getFilePath());
            File file = new File(request.getFilePath());
            String parentDir = file.getParent();
            String fileName = file.getName();

            StringBuilder output = new StringBuilder();
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(parentDir)); // 파일이 위치한 디렉토리에서 실행

            // Java 실행 시 컴파일(javac) 과정을 추가해야 결과가 나옵니다.
            if ("java".equals(request.getLanguage())) {
                String className = fileName.substring(0, fileName.lastIndexOf("."));
                // 컴파일 후 실행 명령어를 Bash 체인으로 연결
                pb.command("bash", "-c", "javac " + fileName + " && java " + className);
            } else if ("python".equals(request.getLanguage())) {
                pb.command("python3", fileName);
            }

            Process process = pb.start();

            // 표준 출력 읽기
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append("\n");
            }
            // 에러 출력 읽기
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) output.append("[ERROR] ").append(line).append("\n");
            }

            process.waitFor();
            return ResponseEntity.ok(new ExecutionResponse(output.toString()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ExecutionResponse("실행 오류: " + e.getMessage()));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> createFile(@RequestBody CreateRequest request) {
        try {
            Path path = Paths.get(request.getParentPath(), request.getName()).normalize();
            validatePath(path.toString());

            if ("folder".equals(request.getType())) {
                Files.createDirectories(path);
            } else {
                if (path.getParent() != null) Files.createDirectories(path.getParent());
                if (!Files.exists(path)) {
                    // [템플릿 기능] 파일 생성 시 기본 코드 삽입
                    String content = "";
                    if (request.getName().endsWith(".java")) {
                        String className = request.getName().substring(0, request.getName().lastIndexOf("."));
                        content = "public class " + className + " {\n" +
                                "    public static void main(String[] args) {\n" +
                                "        System.out.println(\"Hello, Java!\");\n" +
                                "    }\n" +
                                "}";
                    } else if (request.getName().endsWith(".py")) {
                        content = "print(\"Hello, Python!\")";
                    }
                    Files.writeString(path, content);
                }
            }
            return ResponseEntity.ok("성공적으로 생성되었습니다!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("생성 실패: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestBody Map<String, String> request) {
        try {
            String filePath = request.get("filePath");
            validatePath(filePath);
            Path path = Paths.get(filePath);

            if (Files.isDirectory(path)) {
                // 폴더 삭제 시 하위 파일부터 역순으로 삭제
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } else {
                Files.deleteIfExists(path);
            }
            return ResponseEntity.ok("삭제 성공");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("삭제 실패: " + e.getMessage());
        }
    }

    private FileNodeResponse buildFileTree(File file) {
        String path = file.getAbsolutePath().replace("\\", "/");
        List<FileNodeResponse> children = null;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            children = (files == null) ? new ArrayList<>() :
                    Arrays.stream(files)
                            .map(this::buildFileTree)
                            .sorted(Comparator.comparing(FileNodeResponse::getType)
                                    .thenComparing(FileNodeResponse::getName))
                            .collect(Collectors.toList());
        }
        return FileNodeResponse.builder()
                .name(file.getName())
                .type(file.isDirectory() ? "folder" : "file")
                .path(path)
                .editable(true)
                .children(children)
                .build();
    }
}