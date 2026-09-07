# jvm-risk-scanner

JDK 27로 올리기 전에 저장소만 보고 걸리는 것을 PR 코멘트로 남기는 GitHub Action입니다.

Dependabot이나 Snyk는 의존성 버전을 봅니다. 이 도구는 그 옆의 빈자리를 봅니다. JVM 플래그, 에이전트, `sun.misc.Unsafe` 사용처처럼 JDK 27 기본값 변화(JEP 523 G1 기본화, JEP 534 압축 객체 헤더)에 실제로 영향을 받는 지점과, 런타임·프레임워크 라인의 지원 종료, 그리고 최근 고위험 CVE 중 이 저장소가 실제로 취약 경로를 쓰는지까지 한 번에 확인합니다.

```
### [CRITICAL] CVE-2026-59270 — spring-security (Boot BOM 관리) 7.0.6: 내장 UnboundID LDAP 서버가 … (CVSS 9.4)
- 조건: spring-security-ldap 의 내장(UnboundID) LDAP 서버 사용 시 — 사용 흔적: src/main/java/app/LdapCfg.java
### [HIGH] JDK27-COH-UNSAFE — sun.misc.Unsafe 직접 사용 — 압축 객체 헤더와 충돌 위험
### [MEDIUM] JDK27-GC-DEFAULT — JDK 27 부터 모든 환경에서 G1 이 기본 GC (JEP 523)
- GC 플래그 없음. 현재 JDK 21 → 27 이행 시 소형 컨테이너(1 CPU / <1792MB)에서 Serial→G1 로 바뀜.
```

## 사용

`.github/workflows/jvm-risk.yml`

```yaml
name: jvm-risk
on: [pull_request]
permissions: { contents: read, pull-requests: write }
jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: cloud911948/jvm-risk-scanner@main
```

로컬에서는 JDK 21 이상이면 됩니다.

```
./gradlew -q fatJar
java -jar build/libs/jvm-risk-scanner.jar /path/to/repo            # 마크다운 리포트
java -jar build/libs/jvm-risk-scanner.jar /path/to/repo --json     # 감지 사실 + 지적 사항
java -jar build/libs/jvm-risk-scanner.jar /path/to/repo --rules my-rules.json
```

## 무엇을 보나

| 영역 | 판정 | 근거 |
|---|---|---|
| JDK 27 기본값 | GC 플래그가 없으면 소형 컨테이너에서 Serial→G1 전환을 경고. 압축 객체 헤더 기본화에 대해 `Unsafe`·JOL·lincheck·`-javaagent` 를 충돌 후보로 표시. JFR 사용 시 자동 마스킹 안내 | [JEP 523](https://openjdk.org/jeps/523), [JEP 534](https://openjdk.org/jeps/534) |
| EOL | JDK·Spring Boot·Spring Framework·Spring Security·Redis·Tomcat 라인의 지원 종료가 지났거나 120일 이내 | endoflife.date. 배포판별로 다른 JDK 날짜는 규칙의 `note` 에 적어 두었습니다 |
| CVE | Spring Security 2026-08-20 권고(CVSS 9.4 내장 LDAP, DPoP, WebAuthn, XSS), Spring for GraphQL RCE(9.2), Redis TLS UAF | 규칙마다 출처 URL |

### 버전만 보고 CRITICAL 을 찍지 않습니다

취약 버전 범위에 있어도 소스·빌드·설정 어디에도 그 기능을 쓰는 흔적(예: 내장 LDAP 이면 `UnboundIdContainer`, `spring-security-ldap`)이 없으면 심각도를 INFO 로 내리고 "취약 버전이나 사용 흔적 없음"으로 적습니다. 흔적이 있으면 원래 심각도에 발견 파일을 붙입니다. 세 저장소에서 실측했을 때 버전만으로는 셋 다 CRITICAL 이었고 실제로는 셋 다 해당 기능을 쓰지 않았습니다. 그 결과를 그대로 내보내면 운영자가 도구를 믿지 않게 됩니다.

### Boot 버전만 있는 프로젝트

`spring-boot-dependencies` POM 에서 뽑은 관리 버전 표(`rules.json` 의 `boot_bom`)로 Security·Framework·GraphQL 버전을 채웁니다. 표에 없는 패치 버전은 같은 minor 의 가장 가까운 아래 패치로 채우고 `DEP-ESTIMATED` 로 따로 표시합니다. 멀티모듈에서 Boot 버전이 섞여 있으면 가장 오래된 버전을 기준으로 대조하고 `BOOT-MIXED` 로 알립니다.

## 규칙은 데이터

판정 기준은 전부 [`rules.json`](rules.json) 에 있고 코드는 대조만 합니다. 새 CVE·EOL·JDK 기본값 변화는 JSON 항목 하나로 추가합니다. `--rules` 로 자체 규칙 파일을 바꿔 끼울 수 있습니다. PR 은 출처 URL 을 붙여 주십시오.

## 한계

- 빌드 파일을 정규식으로 읽습니다. Gradle·Maven 파서를 쓰지 않는 것은 의도입니다. DSL 변형마다 파서가 깨지는 것보다 못 찾으면 "미확인"으로 남기는 쪽이 운영에서 덜 위험했습니다.
- JEP 534 는 깨지는 도구 목록을 제공하지 않습니다. `Unsafe`·JOL·lincheck·javaagent 항목은 "확인 필요" 신호이지 확정 판정이 아닙니다.
- 사용 흔적 검색은 문자열 매칭입니다. 서드파티 라이브러리가 같은 이름을 쓰면 오탐이 납니다. WebAuthn 규칙이 Yubico 구현을 잡았던 사례가 있어 Spring Security 전용 심볼로 좁혔습니다.
- Windows 의 WSL 마운트(`/mnt/c`)에서는 파일 I/O 가 느립니다. 자바 파일 1,700개짜리 저장소가 35초 걸렸습니다. CI 러너에서는 문제가 되지 않습니다.

## 개발

```
./gradlew test        # fixture/ fixture2/ fixture3/ 로 규칙 회귀 테스트
./gradlew fatJar      # build/libs/jvm-risk-scanner.jar
```

구조는 `Collector`(사실 수집) → `Evaluator`(규칙 대조) → `Report`(출력) 세 단계이고, `Rules` 가 JSON 을 타입으로 읽습니다. 수집기는 판단하지 않고 평가기는 파일을 읽지 않습니다.

## 라이선스

MIT
