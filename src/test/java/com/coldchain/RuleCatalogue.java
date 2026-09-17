package com.coldchain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RuleCatalogue {

    static final Path FILE = Path.of("rules", "project-rules.md");

    static final List<String> SEVERITIES = List.of("SEC", "INT", "CON", "COST", "STYLE");

    static final List<String> MACHINES = List.of("yes", "partial", "planned", "none");

    static final List<String> STATUSES = List.of("green", "red", "pending", "—");

    private static final Pattern ROW = Pattern.compile("^[|] (R([0-9]+)[.]([0-9]+)) [|]");

    private static final Pattern CITATION = Pattern.compile("`([^`]+)`");

    private static final Pattern GRADLE_TASK = Pattern.compile("`[^`]+`[ ]*[(]Gradle task[)]");

    private static final int EXPECTED_CELLS = 8;

    private RuleCatalogue() {
    }

    record Rule(String id, int group, int number, String statement, String severity, String enforcerCell,
            String machine, String status) {

        boolean declaresAMachine() {
            return machine.equals("yes") || machine.equals("partial");
        }

        List<String> citedEnforcers() {
            List<String> cited = new ArrayList<>();
            Matcher matcher = CITATION.matcher(GRADLE_TASK.matcher(enforcerCell).replaceAll(""));
            while (matcher.find()) {
                cited.add(matcher.group(1));
            }
            return cited;
        }

        List<String> citedGradleTasks() {
            List<String> tasks = new ArrayList<>();
            Matcher matcher = GRADLE_TASK.matcher(enforcerCell);
            while (matcher.find()) {
                Matcher citation = CITATION.matcher(matcher.group());
                if (citation.find()) {
                    tasks.add(citation.group(1));
                }
            }
            return tasks;
        }
    }

    static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        for (String line : lines()) {
            Matcher header = ROW.matcher(line);
            if (!header.find()) {
                continue;
            }
            String[] cells = line.split("[|]", -1);
            if (cells.length != EXPECTED_CELLS) {
                continue;
            }
            rules.add(new Rule(header.group(1), Integer.parseInt(header.group(2)),
                    Integer.parseInt(header.group(3)), cells[2].trim(), cells[3].trim(), cells[4].trim(),
                    cells[5].trim(), cells[6].trim()));
        }
        return List.copyOf(rules);
    }

    static List<String> lines() {
        try {
            return Files.readAllLines(FILE);
        } catch (IOException cause) {
            throw new UncheckedIOException("The rule catalogue could not be read from " + FILE.toAbsolutePath(),
                    cause);
        }
    }
}
