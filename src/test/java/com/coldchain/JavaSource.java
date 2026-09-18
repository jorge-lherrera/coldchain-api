package com.coldchain;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class JavaSource {

    private JavaSource() {
    }

    record Comment(int line, int depth, String text) {
    }

    record Scan(Path file, String code, List<Comment> comments) {

        List<Comment> insideBlocks() {
            return comments.stream().filter(comment -> comment.depth() >= 2).toList();
        }

        List<String> imports() {
            return code.lines()
                    .filter(line -> line.startsWith("import "))
                    .map(line -> line.substring("import ".length()).replace(";", "").trim())
                    .toList();
        }

        String body() {
            return code.lines()
                    .filter(line -> !line.startsWith("import "))
                    .filter(line -> !line.startsWith("package "))
                    .reduce("", (all, line) -> all + "\n" + line);
        }
    }

    static Scan scan(Path file) {
        String text = SourceTree.read(file);
        StringBuilder code = new StringBuilder();
        List<Comment> comments = new ArrayList<>();
        int depth = 0;
        int line = 1;
        int index = 0;
        while (index < text.length()) {
            char character = text.charAt(index);
            if (text.startsWith("\"\"\"", index)) {
                int end = text.indexOf("\"\"\"", index + 3);
                end = end < 0 ? text.length() : end + 3;
                line += countLines(text, index, end);
                code.append("\"\"");
                index = end;
            } else if (character == '"' || character == '\'') {
                int end = endOfLiteral(text, index, character);
                line += countLines(text, index, end);
                code.append("\"\"");
                index = end;
            } else if (text.startsWith("//", index)) {
                int end = text.indexOf('\n', index);
                end = end < 0 ? text.length() : end;
                comments.add(new Comment(line, depth, text.substring(index, end).trim()));
                index = end;
            } else if (text.startsWith("/*", index)) {
                int end = text.indexOf("*/", index + 2);
                end = end < 0 ? text.length() : end + 2;
                comments.add(new Comment(line, depth, text.substring(index, end).trim()));
                line += countLines(text, index, end);
                index = end;
            } else {
                if (character == '{') {
                    depth++;
                } else if (character == '}') {
                    depth--;
                } else if (character == '\n') {
                    line++;
                }
                code.append(character);
                index++;
            }
        }
        return new Scan(file, code.toString(), List.copyOf(comments));
    }

    static List<Scan> scanAll(Path root) {
        return SourceTree.javaFiles(root).stream().map(JavaSource::scan).toList();
    }

    private static int endOfLiteral(String text, int start, char quote) {
        int index = start + 1;
        while (index < text.length()) {
            char character = text.charAt(index);
            if (character == '\\') {
                index += 2;
                continue;
            }
            if (character == quote) {
                return index + 1;
            }
            index++;
        }
        return text.length();
    }

    private static int countLines(String text, int start, int end) {
        int lines = 0;
        for (int index = start; index < end && index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}
