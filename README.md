# bep4-1-mission

수업 실습 저장소입니다. [sik2/bep4-1-mission](https://github.com/sik2/bep4-1-mission)을 fork 했고,
upstream에는 프로젝트 골격(커밋 `0000`: Gradle 설정과 `BackApplication`)만 있습니다.
그 뒤의 커밋(`0001`~`0024`)은 모두 이 저장소에서 작성했습니다.

## 기술 스택

- Java 25 (Gradle toolchain), Gradle 9.2.1 (wrapper)
- Spring Boot 4.0.1: Web MVC, Data JPA, DevTools
- H2 (파일 DB `./db_dev`, `MODE=MySQL`), H2 콘솔 의존성 포함
- Lombok

## 구조: 모듈러 모놀리스

하나의 애플리케이션 안에서 도메인을 `boundedContext` 패키지로 나누고,
컨텍스트끼리는 서로의 엔티티를 직접 참조하지 않고 이벤트와 DTO로 주고받습니다.

```
src/main/java/com/back
├── boundedContext
│   ├── member   회원 (원본 회원 데이터, 활동 점수)
│   ├── post     글 · 댓글
│   └── cash     지갑
├── shared       컨텍스트 사이에서 공유하는 DTO · 이벤트 · 회원 베이스 클래스 · API 클라이언트
└── global       공통 설정, 이벤트 발행기, BaseEntity, RsData, 예외, 초기 데이터
```

각 컨텍스트 안은 같은 규칙으로 나뉩니다.

| 패키지 | 역할 | 예 |
| --- | --- | --- |
| `app` | 유스케이스 · 퍼사드 | `MemberFacade`, `MemberJoinUseCase`, `PostWriteUseCase` |
| `domain` | 엔티티 · 정책 | `Member`, `MemberPolicy`, `Post`, `PostComment`, `Wallet` |
| `in` | 들어오는 쪽: 컨트롤러, 이벤트 리스너, 초기 데이터 | `ApiV1MemberController`, `PostEventListener` |
| `out` | 나가는 쪽: 리포지토리 | `MemberRepository`, `WalletRepository` |

### 회원 데이터 복제

`member` 컨텍스트의 `Member`(`SourceMember`)가 원본이고, `post`와 `cash`는 각자 테이블에
회원 사본(`PostMember`, `CashMember`, 공통 부모 `ReplicaMember`)을 둡니다. 사본은 원본과 같은 id를 쓰고,
이벤트를 받아 동기화됩니다.

### 이벤트 흐름

이벤트는 `global/eventPublish/EventPublisher`(Spring `ApplicationEventPublisher` 래퍼)로 발행하고,
엔티티 안에서는 `BaseEntity.publishEvent()`로 발행합니다. 리스너는 모두
`@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)`로,
발행한 트랜잭션이 커밋된 뒤 새 트랜잭션에서 처리합니다.

| 이벤트 | 발행 위치 | 받는 곳 → 처리 |
| --- | --- | --- |
| `MemberJoinedEvent` | `MemberJoinUseCase.join` | post → `PostMember` 동기화 / cash → `CashMember` 동기화 + `Wallet` 생성 |
| `MemberModifiedEvent` | `Member.increaseActivityScore` | post, cash → 회원 사본 동기화 |
| `PostCreatedEvent` | `PostWriteUseCase.write` | member → 작성자 활동 점수 +3 |
| `PostCommentCreatedEvent` | `Post.addComment` | member → 댓글 작성자 활동 점수 +1 |

이벤트 외에 HTTP 호출 예도 하나 있습니다. 글을 쓸 때 `shared/member/out/MemberApiClient`가
`RestClient`로 `GET http://localhost:8080/api/v1/member/members/randomSecureTip`을 호출해
`MemberPolicy`의 비밀번호 변경 주기 안내 문구를 받아 결과 메시지에 붙입니다.

### 초기 데이터

앱이 시작되면 `MemberDataInit` → `PostDataInit` 순서(`@Order`)로 회원 6명
(`system`, `holding`, `admin`, `user1`~`user3`, 비밀번호 `1234`)과 글 6개, 댓글 8개를 만듭니다.
이미 데이터가 있으면 건너뜁니다.

## 실행

JDK 25가 필요합니다(Gradle toolchain이 25를 찾습니다).

```bash
./gradlew bootRun
```

- 기본 프로필은 `dev`이고 포트는 8080입니다.
- DB는 프로젝트 폴더의 `db_dev.mv.db` 파일에 저장됩니다(`.gitignore`에 포함). 처음부터 다시 시작하려면 이 파일을 지우면 됩니다.
- 글 작성 시 자기 자신(`localhost:8080`)의 API를 호출하므로 포트를 바꾸면 `MemberApiClient`의 주소도 맞춰야 합니다.

## 이 저장소에서 구현한 것

upstream 골격 이후 커밋 기준으로 정리했습니다. 커밋 메시지는 단계 번호(`0001`~`0024`, 일부 번호 없음)만
적혀 있어서 각 단계의 의도는 코드 변경으로만 확인할 수 있습니다.

| 커밋 | 변경 내용 |
| --- | --- |
| `0001`~`0004` | `Member`, `Post`, `PostComment` 엔티티와 서비스 · 리포지토리, `BaseEntity`/`BaseIdAndTime`, 초기 데이터, yaml 설정 |
| `0005` | 패키지를 `boundedContext/member`, `boundedContext/post`, `global`로 재배치 |
| `0006` | `EventPublisher`, `GlobalConfig`, `shared/post`의 DTO · 이벤트, 글 · 댓글 생성 시 활동 점수를 올리는 `MemberEventListener` |
| `0007` | 컨텍스트 내부를 `app` / `domain` / `in` / `out`으로 재배치 |
| `0008`~`0012` | 서비스를 퍼사드 + 유스케이스(`MemberJoinUseCase`, `PostWriteUseCase`)로 분리, `MemberPolicy`, `RsData` |
| `0018`~`0020` | `shared/member`의 `BaseMember` / `SourceMember` / `ReplicaMember`, `MemberDto`, `MemberJoinedEvent` · `MemberModifiedEvent`, `PostMember` 복제와 동기화 |
| `0021` | 글 · 댓글이 `PostMember`를 작성자로 쓰도록 변경, `ApiV1MemberController`와 `MemberApiClient`(HTTP 호출) |
| `0022` | 초기 데이터를 컨텍스트별 `MemberDataInit`, `PostDataInit`으로 분리 |
| `0023`~`0024` | `cash` 컨텍스트: `CashMember` 복제, 가입 시 `Wallet` 생성, `BaseManualIdAndTime`(id를 직접 지정하는 베이스 엔티티) |
