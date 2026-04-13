# 🚀 Cloud-Link IDE (Backend)

Cloud-Link IDE는 웹 기반의 통합 개발 환경을 위한 강력한 백엔드 서버입니다.

Spring Boot 기반으로 구현되었으며, 실시간 협업 채팅, 원격 코드 실행, 그리고 안정적인 파일 시스템 관리를 지원합니다.

---

## 🛠 주요 백엔드 기능

### 📡 실시간 통신 (WebSocket & STOMP)
- SockJS/STOMP 기반 메시징: 안정적인 전이중 통신 환경 구축
- 세션 관리 시스템: EventListener를 활용하여 브라우저 강제 종료, 로그아웃 등 예외 상황에서도 정확한 사용자 입장/퇴장 상태 관리
- 실시간 참여자 동기화: Thread-safe한 컬렉션을 사용하여 모든 클라이언트에 최신 접속자 명단 공유

### ⚙️ 원격 코드 실행 엔진
- 다국어 지원: Java, Python 등의 소스 코드를 서버 측에서 동기/비동기로 실행
- Runtime 환경 관리: 시스템 프로세스를 활용한 코드 컴파일 및 실행 결과 반환

### 📂 파일 시스템 API
- 프로젝트 구조 관리: 트리 구조의 파일/디렉토리 CRUD 처리
- 실시간 저장: 클라이언트의 편집 상태를 서버 파일 시스템에 즉각 반영

### 🔐 인증 및 보안
- 세션 기반 인증: 사용자 인증 및 권한 관리
- 사용자 격리: 개별 유저별 독립된 작업 디렉토리 할당 계획 중

---

## 🏗 기술 스택
- Framework: Spring Boot 3.x
- Language: Java 17
- Communication: Spring WebFlux, Spring WebSocket (STOMP)
- Build Tool: Gradle / Maven
- Library:
    - Lombok: 코드 간소화
    - SockJS: 브라우저 호환성 확보
    - Spring Messaging: 메시지 라우팅

---

## 📂 프로젝트 구조 (Backend)

```plaintext
src/main/java/com/example/Web_IDE_Project/
├── config/
│   ├── WebSocketConfig.java        # STOMP 엔드포인트 및 브로커 설정
│   ├── WebSocketEventListener.java # [핵심] 연결 해제(Disconnect) 감지 및 뒷정리
│   ├── JwtTokenProvider.java       # JWT 토큰 생성 및 검증
│   ├── JwtAuthenticationFilter.java# JWT 인증 필터 처리
│   ├── SecurityConfig.java         # Spring Security 설정
│   └── SwaggerConfig.java          # API 문서화 Swagger 설정
├── controller/
│   ├── ChatController.java         # 채팅 메시지 핸들링 및 유저 리스트 관리
│   ├── AuthController.java         # 인증 및 로그인/회원가입 API
│   └── FileController.java         # 파일 CRUD 및 트리 구조 반환 API
├── domain/
│   ├── Role.java                   # 사용자 권한(Role) 엔티티
│   └── User.java                   # 사용자 엔티티
├── dto/
│   ├── ChatMessage.java            # 메시지 타입 및 데이터 규격 (Builder 패턴)
│   ├── CreateRequest.java          # 파일/디렉토리 생성 요청 DTO
│   ├── DeleteRequest.java          # 파일/디렉토리 삭제 요청 DTO
│   ├── ExecutionRequest.java       # 코드 실행 요청 DTO
│   ├── ExecutionResponse.java      # 코드 실행 결과 응답 DTO
│   ├── FileNodeResponse.java       # 파일 트리 응답 DTO
│   ├── SaveRequest.java            # 파일 저장 요청 DTO
│   └── SignupRequest.java          # 회원가입 요청 DTO
└── repository/
    └── UserRepository.java         # 사용자 엔티티를 관리하는 Spring Data JPA 저장소
```

---

## 💬 핵심 로직 설명: 실시간 채팅 & 세션 관리

본 프로젝트는 클라이언트의 비정상 종료 시에도 데이터 무결성을 유지하기 위해 이벤트 리스너 방식을 채택했습니다.

### 1. 세션 트래킹
- 사용자가 `/pub/chat/message`를 통해 `ENTER` 타입을 보낼 때, `SimpMessageHeaderAccessor`를 사용하여 WebSocket 세션에 사용자 이름을 바인딩합니다.

```java
headerAccessor.getSessionAttributes().put("username", message.getSender());
```

### 2. 자동 퇴장 처리 (EventListener)
- `SessionDisconnectEvent`를 구독하여 사용자가 로그아웃 버튼을 누르거나 창을 닫는 즉시 `userList`에서 제거하고 전역 퇴장 알림을 전송합니다.

**장점**: 프론트엔드의 비정상 종료와 상관없이 100% 인원수 동기화 보장.

---

## 🔌 API 엔드포인트

### 🗨️ WebSocket (STOMP)
- Endpoint: `/ws-ide`
- Publish: `/pub/chat/message` (Type: `ENTER`, `TALK`, `LEAVE`)
- Subscribe: `/sub/chat/room/global`

### 📝 Code Execution
- `POST /api/code/run` - 코드를 전송받아 서버 런타임에서 실행 후 결과 반환

### 📁 File Management
- `GET /api/files/tree` - 전체 파일 구조 조회
- `POST /api/files/save` - 편집된 코드 내용 저장

---

## 🚀 실행 방법

### 환경 변수 설정
`application.properties` 또는 `application.yml`에서 포트 및 설정을 확인하세요.

```properties
server.port=8080
# CORS 설정 (프론트엔드 주소 허용)
cors.allowed-origins=http://localhost:5173
```

### 빌드 및 실행

```bash
./gradlew bootRun
```

---

## ⚠️ 주의사항 및 배포 정보
- 현재 백엔드 서버는 `3.39.191.82:8080` 환경에서 동작 중입니다.
- CORS 설정: 프론트엔드 도메인이 변경될 경우 `WebConfig`에서 Origin 허용 범위를 수정해야 합니다.
- Thread Safety: `userList`는 다중 접속 환경을 고려하여 `Collections.synchronizedSet`으로 관리됩니다.

---

## 👨‍💻 Contributor
- Backend Lead: bys0212
- Hackathon: Goorm Hackathon - Encouragement Award 🏆
