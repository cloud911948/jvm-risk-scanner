package io.github.cloud911948.jvmrisk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * 사용: java -jar jvm-risk-scanner.jar [디렉터리] [--json] [--rules 경로]
 * 종료 코드는 항상 0 이다. PR 을 막는 게 아니라 리포트를 남기는 도구이고, 막을지는 워크플로에서 정한다.
 */
public final class Main {

    public static void main(String[] args) {
        Path root = Path.of(".");
        Path rulesFile = null;
        boolean json = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--json" -> json = true;
                case "--rules" -> {
                    if (i + 1 >= args.length) usage("--rules 뒤에 규칙 파일 경로가 필요합니다");
                    rulesFile = Path.of(args[++i]);
                }
                default -> root = Path.of(args[i]);
            }
        }
        if (!Files.isDirectory(root)) usage("디렉터리가 아닙니다: " + root);
        Rules rules = rulesFile != null ? Rules.load(rulesFile) : Rules.bundled();
        Facts facts = new Collector(rules).collect(root);
        List<Finding> findings = new Evaluator(rules, LocalDate.now()).evaluate(facts);
        System.out.print(json ? Report.json(facts, findings) : Report.markdown(rules, facts, findings));
    }

    private static void usage(String reason) {
        System.err.println(reason);
        System.err.println("사용: java -jar jvm-risk-scanner.jar [디렉터리] [--json] [--rules 경로]");
        System.exit(2);
    }
}
