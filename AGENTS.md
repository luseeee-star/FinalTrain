# FinalTrain AI Agent Guidelines

## Architecture Overview
FinalTrain is a Spring Boot 3.5.9 application for a student lost-and-found system with real-time chat. Key components:
- **Controllers**: REST APIs under `/Users`, `/StuItems`, `/message` with JWT auth
- **Services**: Business logic in `impl/` packages using MyBatis-Plus
- **WebSocket**: Real-time messaging at `/chat` endpoint
- **Data**: MySQL with MyBatis-Plus (camelCase mapping, stdout logging)
- **Auth**: JWT tokens in `Authorization` header, ThreadLocal claims storage

## Key Patterns
- **Response Format**: Use `ResultJson.success(data)` or `ResultJson.error(msg)` for all API responses
- **Authentication**: Extract user ID from `ThreadLocalUtil.get().get("userid")` in protected endpoints
- **File Uploads**: Save to `D:\Java\FinalTrain\msg\` with `UUID.randomUUID() + ext`, serve via `/msg/**` mapping
- **Validation**: Throw `IllegalArgumentException` for business errors, caught in controllers
- **Passwords**: Encode with `BCryptPasswordEncoder` on registration/login
- **Cross-Origin**: `@CrossOrigin(origins = "*")` on all controllers
- **WebSocket Auth**: Token in query param `?token=...`, verify and store user in session
- **AI Description Generation**: POST to `/StuItems/ai/preview` for streaming AI descriptions; backend returns `Flux<String>` mapped to SSE "data: " format with `[DONE]` marker; frontend uses `fetch` with `ReadableStream` to display data in real-time until `[DONE]`

## Development Workflow
- **Build**: `./mvnw.cmd clean package` (requires Java 21)
- **Run**: `java -jar target/FinalTrain-0.0.1-SNAPSHOT.jar` (set env vars: `DB_URL1`, `DB_USER`, `DB_PASSWORD`)
- **Debug**: Check MyBatis logs in console; interceptor logs auth attempts
- **Test**: Use `@SpringBootTest` with test DB; no custom test setup found

## Conventions
- **POJOs**: `@Data` with `@TableName`, `@TableId(type = IdType.AUTO)`, `@TableField` for custom mappings
- **Mappers**: Extend `BaseMapper<Entity>` in `lsj.qg.finaltrain.mapper`
- **Services**: Interface in `service/`, impl in `service/impl/` with `@Service`
- **Config**: Exclude Spring Security auto-config; custom CORS and resource handlers in `WebConfig`
- **Exceptions**: Global handler in `GlobalExceptionHandler` (not detailed in code)
- **Reactive Streams**: Use `Flux<String>` for streaming responses like AI generation, with `produces = MediaType.TEXT_EVENT_STREAM_VALUE`

## Key Files
- `src/main/resources/application.yml`: DB config via env vars
- `src/main/java/lsj/qg/finaltrain/config/WebConfig.java`: Interceptors, CORS, file serving
- `src/main/java/lsj/qg/finaltrain/intercepters/LoginInterceptor.java`: JWT verification
- `src/main/java/lsj/qg/finaltrain/utils/ResultJson.java`: Response wrapper
- `src/main/java/lsj/qg/finaltrain/websocket/ChatEndpoint.java`: Real-time chat logic
- `src/main/java/lsj/qg/finaltrain/service/impl/ItemServiceImpl.java`: AI description logic with Spring AI streaming
