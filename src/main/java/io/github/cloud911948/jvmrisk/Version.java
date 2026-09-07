package io.github.cloud911948.jvmrisk;

import java.util.Comparator;

/**
 * "7.0.6", "3.5.13-SNAPSHOT" 같은 문자열을 앞 세 자리 숫자로만 비교한다.
 * 의도적으로 SemVer 전체를 구현하지 않는다. 여기서 다루는 건 Spring·JDK 라인 번호뿐이고,
 * qualifier 순서까지 따지기 시작하면 규칙 표와 어긋나는 경우가 더 많았다.
 */
public record Version(int major, int minor, int patch) implements Comparable<Version> {

    public static final Comparator<String> ORDER = Comparator.comparing(Version::parse);

    public static Version parse(String text) {
        String[] parts = text.split("[.\\-]");
        int[] n = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++) {
            n[i] = parts[i].chars().allMatch(Character::isDigit) && !parts[i].isEmpty() ? Integer.parseInt(parts[i]) : 0;
        }
        return new Version(n[0], n[1], n[2]);
    }

    public static boolean inRange(String v, String lo, String hi) {
        Version x = parse(v);
        return parse(lo).compareTo(x) <= 0 && x.compareTo(parse(hi)) <= 0;
    }

    /** "3.5.13" → "3.5". EOL 표와 BOM 표는 minor 라인 단위다. */
    public static String line(String v) {
        String[] p = v.split("\\.");
        return p.length >= 2 ? p[0] + "." + p[1] : v;
    }

    @Override
    public int compareTo(Version o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) c = Integer.compare(minor, o.minor);
        return c == 0 ? Integer.compare(patch, o.patch) : c;
    }
}
