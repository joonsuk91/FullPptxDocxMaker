package sermon;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

public final class ManuscriptParser {

    private static final java.util.regex.Pattern SECTION_START =
            java.util.regex.Pattern.compile("^\\d+\\.");

    private static final java.util.regex.Pattern PARENTHESES_CONTENT =
            java.util.regex.Pattern.compile("\\(([^)]*)\\)");

    private static final java.util.regex.Pattern REFERENCE =
            java.util.regex.Pattern.compile("([가-힣]+)\\s*(\\d+)[a-zA-Z]*\\s*:\\s*([\\d,\\-]+)"
                                                                            + "((?:(?![가-힣]+\\d+\\s*:).)*)");

    private static final java.util.regex.Pattern VERSE_ONLY =
            java.util.regex.Pattern.compile("^\\d+(?:[-,]\\d+)*$");

    private ManuscriptParser() {}

    public static ParsedSermon parse(java.nio.file.Path manuscriptFile)
        throws java.io.IOException {

        java.util.List<String> lines =
                java.nio.file.Files.readAllLines(manuscriptFile, StandardCharsets.UTF_8);

        // 제목(sectionStartLine)을 정리하는 코드
       java.util.List<String> titleLines = extractTitleSection(lines);

       String referenceRaw = extractReferenceRaw(titleLines);

       java.util.List<String> cleanedTitleLines = cleanTitleLines(titleLines);

       validateTitles(cleanedTitleLines);
       String koreanTitle = cleanedTitleLines.get(0);
       String englishTitle = cleanedTitleLines.get(1);

        System.out.println(koreanTitle);
        System.out.println(englishTitle);
        System.out.println();

        // 말씀차례(sectionTitleLines)를 정리하는 코드
        java.util.List<String> sectionTitleLines = extractSectionTitleLines(lines);

        java.util.List<String> sectionTitleLinesWithBlank = insertBlankLineBetweenSections(sectionTitleLines);

        validateSections(sectionTitleLinesWithBlank);
        for (int i = 0; i < sectionTitleLinesWithBlank.size(); i += 3) {
            System.out.println(sectionTitleLinesWithBlank.get(i));
            System.out.println(sectionTitleLinesWithBlank.get(i + 1));
        }

        System.out.println();

        // 본문(제목구간 성경구절)을 정리하는 코드
        String mainBookFUll = extractMainBookFull(referenceRaw);
        String mainChapter = extractMainChapter(referenceRaw);
        java.util.List<String> onlyVerseLines = extractOnlyVerseLines(lines, titleLines.size());
        System.out.println(referenceRaw);

        System.out.println("");

        // 본문(각각의 말씀차례 + 본문 조합)을 정리한 코드
        java.util.List<java.util.List<String>> blocks = new java.util.ArrayList<>();
        java.util.List<Integer> sectionStartIndexes = extractSectionStartIndexes(lines);
        int sectionCount = 3;
        if (onlyVerseLines.size() != sectionCount) {
            throw new IllegalArgumentException("절범위 개수(" + onlyVerseLines.size() + ")가 대지 개수(3)와 다릅니다.");
        }

            for (int i = 0; i < sectionCount; i++) {
            int sectionStart = sectionStartIndexes.get(i);
            int sectionEnd = (i < sectionCount - 1)
                    ? sectionStartIndexes.get(i + 1)
                    : lines.size();

                java.util.List<String> block = new java.util.ArrayList<>();
                System.out.println(sectionTitleLinesWithBlank.get(i * 3));
                block.add(sectionTitleLinesWithBlank.get(i * 3));
                System.out.println(sectionTitleLinesWithBlank.get(i * 3 + 1));
                block.add(sectionTitleLinesWithBlank.get(i * 3 + 1));

                System.out.println(resolveOnlyVerseLine(mainBookFUll, mainChapter, onlyVerseLines.get(i)));
                block.add(resolveOnlyVerseLine(mainBookFUll, mainChapter, onlyVerseLines.get(i)));

                java.util.List<String> citationInSection =
                        resolveCitationsInSection(lines, sectionStart, sectionEnd);
                for (String citation : citationInSection) {
                    System.out.println(citation);
                    block.add(citation);
                }

            System.out.println();
            blocks.add(block);

        }

        return new ParsedSermon(koreanTitle, englishTitle, sectionTitleLines, referenceRaw, blocks);

    }

    // 제목을 정리하는 코드
    static java.util.List<String> extractTitleSection(java.util.List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (SECTION_START.matcher(lines.get(i).trim()).find()) {
                return new java.util.ArrayList<>(lines.subList(0, i));
            }
        }
        throw new IllegalArgumentException("말씀 차례를 찾지 못했습니다.");
    }

    static String extractReferenceRaw(java.util.List<String> titleLines) {
        for (String line : titleLines) {
            java.util.regex.Matcher matcher = PARENTHESES_CONTENT.matcher(line);
            if (matcher.find()) {
                String rawReference = matcher.group(1).trim();
                java.util.regex.Matcher referenceMatcher = REFERENCE.matcher(rawReference);
                if (referenceMatcher.find()) {
                String bookFull = BookNames.toFullName(referenceMatcher.group(1));
                return bookFull + rawReference.substring(referenceMatcher.end(1));
                }
            }
        }


        throw new IllegalArgumentException("제목 구간에서 괄호 안 내용을 찾지 못했습니다.");
    }

    static java.util.List<String> cleanTitleLines(java.util.List<String> titleLines) {
        java.util.List<String> cleanedTitleLines = new java.util.ArrayList<>();
        for (String line : titleLines) {
            String cleanedLine = PARENTHESES_CONTENT.matcher(line).replaceAll("").trim();
            if (!cleanedLine.isEmpty()) {
                cleanedTitleLines.add(cleanedLine);
            }
        }

        return cleanedTitleLines;
    }

    static void validateTitles(java.util.List<String> cleanedTitleLines) {
        if (cleanedTitleLines.size() < 2) {
            throw new IllegalArgumentException("한글 제목 또는 영어 제목이 없습니다.");
        }
    }

    // 말씀차례를 정리하는 코드
    static java.util.List<String> extractSectionTitleLines(java.util.List<String> lines) {
        java.util.List<String> sectionTitleLines = new java.util.ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (SECTION_START.matcher(trimmed).find()) {
                sectionTitleLines.add(normalizeSectionTitleLine(trimmed));
            }
        }

        if (sectionTitleLines.size() % 2 != 0) {
            throw new IllegalArgumentException("대지 제목이 한글·영문 짝을 이루지 않습니다.");
        }

        return sectionTitleLines;
    }

    private static String normalizeSectionTitleLine(String line) {
        return line.replaceFirst("^(\\d+)\\.+\\s*", "$1. ");
    }

    static java.util.List<String> insertBlankLineBetweenSections(java.util.List<String> sectionTitleLines) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i < sectionTitleLines.size(); i += 2) {
            result.add(sectionTitleLines.get(i)); // < 한글 section
            result.add(sectionTitleLines.get(i + 1)); // < 영어 section
            result.add("");
        }

        return result;
    }

    static void validateSections(java.util.List<String> sectionTitleLinesWithBlank) {
        for (int i = 0; i < sectionTitleLinesWithBlank.size(); i += 3) {
                if (!sectionTitleLinesWithBlank.get(i + 2).isEmpty()) {
                    throw new IllegalArgumentException("대지 사이에 빈 줄이 와야 하는 자리에 빈 줄이 없습니다.");
                }
        }
    }

    // 본문(메인 성경구절만 나열하는 1번째 구간) 정리하는 코드
    static String extractMainBookFull(String referenceRaw) {
        java.util.regex.Matcher matcher = REFERENCE.matcher(referenceRaw);
        if (!matcher.find()) {
            throw new IllegalArgumentException("제목구간 성경구절에서 책이름을 찾지 못했습니다.");
        }

        return BookNames.toFullName(matcher.group(1));
    }

    static String extractMainChapter(String referenceRaw) {
        java.util.regex.Matcher matcher = REFERENCE.matcher(referenceRaw);
        if (!matcher.find()) {
            throw new IllegalArgumentException("제목구간 성경구절에서 장을 찾지 못했습니다.");
        }

        return matcher.group(2);
    }

    static java.util.List<String> extractOnlyVerseLines(java.util.List<String> lines, int bodyStartIndex) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String line : lines.subList(bodyStartIndex, lines.size())) {
            String trimmed = line.trim();
            if (VERSE_ONLY.matcher(trimmed).matches()) {
                result.add(trimmed);
            }
        }
        return result;

    }

    static String resolveOnlyVerseLine(String mainBookFull, String mainChapter, String line) {
        String onlyVerse = line.trim();
        if (!VERSE_ONLY.matcher(onlyVerse).matches()) {
            throw new IllegalArgumentException("숫자만 있는 절 줄이 아닙니다: " + line);
        }

        return mainBookFull + " " + mainChapter + ":" + onlyVerse;
    }

    // 본문(각각의 말씀차례 + 본문 조합)을 정리한 코드
    static java.util.List<Integer> extractSectionStartIndexes(java.util.List<String> lines) {
        java.util.List<Integer> allMatchIndexes = new java.util.ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (SECTION_START.matcher(lines.get(i).trim()).find()) {
                allMatchIndexes.add(i);
            }
        }
        java.util.List<Integer> sectionStartIndexes = new java.util.ArrayList<>();
        for (int i = 0; i < allMatchIndexes.size(); i += 2) {
            sectionStartIndexes.add(allMatchIndexes.get(i));
        }
        return sectionStartIndexes;
    }

    static java.util.List<String> resolveCitationsInSection(java.util.List<String> lines, int sectionStart, int sectionEnd) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String line : lines.subList(sectionStart, sectionEnd)) {
            java.util.regex.Matcher matcher = REFERENCE.matcher(line);
            while (matcher.find()) {
                String bookFull = BookNames.toFullName(matcher.group(1));
                String chapter = matcher.group(2);
                String versePart = matcher.group(3);
                String extraInformation = matcher.group(4);
                result.add(bookFull + " " + chapter + ":" + versePart + extraInformation);
            }
        }
        return result;
    }

}
