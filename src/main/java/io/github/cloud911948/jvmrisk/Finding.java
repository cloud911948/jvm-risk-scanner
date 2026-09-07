package io.github.cloud911948.jvmrisk;

import java.util.Comparator;
import java.util.List;

public record Finding(String severity, String id, String title, String detail, String action) {

    private static final List<String> ORDER = List.of("critical", "high", "medium", "low", "info");

    public static final Comparator<Finding> BY_SEVERITY = Comparator.comparingInt(f -> ORDER.indexOf(f.severity()));
}
