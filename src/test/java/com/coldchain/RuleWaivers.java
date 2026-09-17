package com.coldchain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RuleWaivers {

    private static final Pattern ROW = Pattern.compile("^[|] (R[0-9]+[.][0-9]+) [|]");

    private static final int EXPECTED_CELLS = 7;

    private RuleWaivers() {
    }

    record Waiver(String ruleId, String rule, String tolerated, String owner, String expiry) {

        List<String> toleratedViolations() {
            return List.of(tolerated.split(";")).stream().map(String::trim).filter(part -> !part.isEmpty())
                    .toList();
        }

        LocalDate expiresOn() {
            return LocalDate.parse(expiry);
        }
    }

    static List<Waiver> waivers() {
        return waivers(RuleCatalogue.lines());
    }

    static List<Waiver> waivers(List<String> lines) {
        List<Waiver> waivers = new ArrayList<>();
        boolean insideRegister = false;
        for (String line : lines) {
            if (line.startsWith("## Waiver register")) {
                insideRegister = true;
                continue;
            }
            if (!insideRegister) {
                continue;
            }
            Matcher header = ROW.matcher(line);
            String[] cells = line.split("[|]", -1);
            if (header.find() && cells.length == EXPECTED_CELLS) {
                waivers.add(new Waiver(header.group(1), cells[2].trim(), cells[3].trim(), cells[4].trim(),
                        cells[5].trim()));
            }
        }
        return List.copyOf(waivers);
    }

    static boolean covers(String ruleId) {
        return waivers().stream().anyMatch(waiver -> waiver.ruleId().equals(ruleId));
    }

    static void assertOnlyWaived(String ruleId, Collection<String> violations, List<Waiver> register) {
        Set<String> tolerated = new TreeSet<>(register.stream()
                .filter(waiver -> waiver.ruleId().equals(ruleId))
                .flatMap(waiver -> waiver.toleratedViolations().stream())
                .toList());
        Set<String> found = new TreeSet<>(violations);

        Set<String> appeared = new TreeSet<>(found);
        appeared.removeAll(tolerated);
        Set<String> disappeared = new TreeSet<>(tolerated);
        disappeared.removeAll(found);

        if (!appeared.isEmpty() || !disappeared.isEmpty()) {
            throw new AssertionError(ruleId + ": debt does not grow and ground gained is not given back."
                    + (appeared.isEmpty() ? "" : " New violations: " + String.join(", ", appeared) + ".")
                    + (disappeared.isEmpty() ? "" : " Waivers to remove: " + String.join(", ", disappeared) + "."));
        }
    }

    static void assertOnlyWaived(String ruleId, Collection<String> violations) {
        assertOnlyWaived(ruleId, violations, waivers());
    }
}
