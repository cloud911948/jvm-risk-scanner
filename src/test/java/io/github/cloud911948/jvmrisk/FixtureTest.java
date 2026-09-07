package io.github.cloud911948.jvmrisk;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * fixture/ 세 개로 규칙 회귀를 잡는다. 날짜를 2026-09-03 으로 고정하는 이유는 EOL 잔여일이 결과 제목에 들어가기 때문이다.
 */
class FixtureTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 3);
    private static final Rules RULES = Rules.load(Path.of("rules.json"));

    private static List<Finding> scan(String dir, Facts[] out) {
        Facts f = new Collector(RULES).collect(Path.of(dir));
        out[0] = f;
        return new Evaluator(RULES, TODAY).evaluate(f);
    }

    private static Set<String> ids(List<Finding> findings) {
        return findings.stream().map(Finding::id).collect(Collectors.toSet());
    }

    @Test
    void gradleKtsWithEverything() {
        Facts[] f = new Facts[1];
        List<Finding> findings = scan("fixture", f);
        Set<String> ids = ids(findings);
        String titles = findings.stream().map(Finding::title).collect(Collectors.joining(" | "));

        assertEquals(Set.of("21"), f[0].jdk);
        assertEquals("4.0.8", f[0].deps.get("spring-boot"));
        assertEquals("7.0.6", f[0].deps.get("spring-security"));
        assertEquals("2.0.4", f[0].deps.get("spring-graphql"));
        assertTrue(f[0].images.contains(new Facts.Image("redis", "8.2.7")));
        for (String must : List.of("JDK27-GC-DEFAULT", "JDK27-COH-DEFAULT", "JDK27-COH-UNSAFE", "JDK27-COH-LAYOUT-TOOLS",
                "JDK27-COH-AGENT", "JDK27-JFR-REDACT", "EOL-SOON", "CVE-2026-59270", "CVE-2026-59285", "CVE-2026-41707",
                "CVE-2026-47841", "CVE-2026-47877", "CVE-2026-81934")) {
            assertTrue(ids.contains(must), must + " 누락: " + ids);
        }
        assertTrue(titles.contains("spring-boot 4.0.8") && titles.contains("119일"), titles); // 4.0 라인 EOL 2026-12-31
        assertFalse(ids.contains("JDK27-GC-EXPLICIT"));
    }

    @Test
    void versionCatalogAliasResolvesThroughBom() {
        Facts[] f = new Facts[1];
        Set<String> ids = ids(scan("fixture2", f));
        assertEquals("4.0.7", f[0].deps.get("spring-boot"));
        assertEquals("7.0.6", f[0].deps.get("spring-security"));
        assertEquals("bom", f[0].src.get("spring-security"));
        assertTrue(ids.contains("CVE-2026-59270"), ids.toString());
        assertFalse(ids.contains("DEP-ESTIMATED"));
    }

    @Test
    void mavenBomImportFallsBackToNearestPatch() {
        Facts[] f = new Facts[1];
        Set<String> ids = ids(scan("fixture3", f));
        assertEquals("3.5.13", f[0].deps.get("spring-boot"));
        assertEquals("6.5.9", f[0].deps.get("spring-security"));
        assertEquals("bom~", f[0].src.get("spring-security"));
        assertTrue(ids.containsAll(List.of("DEP-ESTIMATED", "EOL-PAST", "EOL-SOON", "CVE-2026-59270")), ids.toString());
    }

    @Test
    void versionCompareIgnoresQualifier() {
        assertTrue(Version.inRange("3.5.13-SNAPSHOT", "3.5.0", "3.5.13"));
        assertFalse(Version.inRange("7.0.7", "7.0.0", "7.0.6"));
        assertEquals("3.5", Version.line("3.5.13"));
    }
}
