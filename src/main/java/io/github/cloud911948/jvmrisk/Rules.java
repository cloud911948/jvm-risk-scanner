package io.github.cloud911948.jvmrisk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * rules.json 의 타입 모델. 규칙은 코드가 아니라 데이터이므로 새 CVE·EOL 은 JSON 한 항목으로 추가한다.
 * 모르는 필드는 무시한다. 규칙 파일에 설명용 note 필드가 많기 때문이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Rules(
        String version,
        List<DefaultRule> jdk27Defaults,
        List<Eol> eol,
        int eolWarnDays,
        List<Cve> cves,
        Map<String, Map<String, String>> bootBom) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DefaultRule(String id, String title, String action, String severity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Eol(String product, String cycle, String eol, String latest, String note) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cve(String id, String product, String severity, Double cvss, String title,
                      List<List<String>> affected, List<String> fixed, String condition,
                      String source, List<String> evidence) {}

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static Rules load(Path file) {
        try {
            return MAPPER.readValue(Files.readAllBytes(file), Rules.class);
        } catch (IOException e) {
            throw new UncheckedIOException("규칙 파일을 읽을 수 없습니다: " + file, e);
        }
    }

    /** jar 에 포함된 기본 규칙. 릴리스 시점의 규칙 스냅샷이며, 최신 규칙은 --rules 로 바꿔 끼운다. */
    public static Rules bundled() {
        try (InputStream in = Rules.class.getResourceAsStream("/rules.json")) {
            if (in == null) throw new IllegalStateException("jar 안에 rules.json 이 없습니다");
            return MAPPER.readValue(in, Rules.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public DefaultRule defaultRule(String id) {
        return jdk27Defaults.stream().filter(r -> r.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("rules.json jdk27_defaults 에 " + id + " 가 없습니다"));
    }
}
