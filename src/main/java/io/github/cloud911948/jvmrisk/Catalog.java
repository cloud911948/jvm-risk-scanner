package io.github.cloud911948.jvmrisk;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * gradle/libs.versions.toml. [versions] 참조(version.ref)와 인라인 버전만 본다.
 * TOML 파서 없이 정규식으로 읽으므로 테이블 중첩·멀티라인 문자열은 지원하지 않는다. 그 경우 "미확인"으로 남는다.
 */
final class Catalog {

    private static final Pattern VERSION_ENTRY = Pattern.compile("^\\s*([\\w\\-]+)\\s*=\\s*\"([\\d][\\w.\\-]*)\"", Pattern.MULTILINE);
    private static final Pattern LIBRARY_ENTRY = Pattern.compile("^\\s*[\\w\\-]+\\s*=\\s*(\\{[^}]*\\}|\"[^\"]+\")", Pattern.MULTILINE);
    private static final Pattern REF = Pattern.compile("version\\.ref\\s*=\\s*\"([\\w\\-]+)\"");
    private static final Pattern INLINE = Pattern.compile("version\\s*=\\s*\"([\\d][\\w.\\-]*)\"");
    private static final Pattern COORD = Pattern.compile("(?:module\\s*=\\s*\"([^\"]+)\"|group\\s*=\\s*\"([^\"]+)\"\\s*,\\s*name\\s*=\\s*\"([^\"]+)\"|id\\s*=\\s*\"([^\"]+)\")");

    private Catalog() {}

    static void scan(String toml, Facts f) {
        Map<String, String> versions = new HashMap<>();
        Matcher v = VERSION_ENTRY.matcher(toml);
        while (v.find()) versions.put(v.group(1), v.group(2));

        Matcher e = LIBRARY_ENTRY.matcher(toml);
        while (e.find()) {
            String body = e.group(1);
            String ga;
            String ver;
            if (body.startsWith("\"")) {
                String[] parts = body.substring(1, body.length() - 1).split(":");
                if (parts.length < 2) continue;
                ga = parts[0] + ":" + parts[1];
                ver = parts.length > 2 ? parts[2] : null;
            } else {
                Matcher c = COORD.matcher(body);
                if (!c.find()) continue;
                ga = c.group(1) != null ? c.group(1) : c.group(2) != null ? c.group(2) + ":" + c.group(3) : c.group(4);
                Matcher r = REF.matcher(body);
                Matcher i = INLINE.matcher(body);
                ver = r.find() ? versions.get(r.group(1)) : i.find() ? i.group(1) : null;
            }
            if (ver == null) continue;
            if (ga.equals("org.springframework.boot")) f.bootAll.add(ver);
            else if (ga.startsWith("org.springframework.security:")) f.deps.put("spring-security", ver);
            else if (ga.startsWith("org.springframework.graphql:")) f.deps.put("spring-graphql", ver);
            else if (ga.equals("org.springframework:spring-core") || ga.equals("org.springframework:spring-context") || ga.equals("org.springframework:spring-web")) f.deps.put("spring-framework", ver);
        }
    }
}
