package sermon;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record ParsedCitation(
        String bookFull,
        String chapter,
        List<Integer> verses,
        String marker,
        String extra
) {
}

final class CitationParser {

    private static final Pattern PLAIN_CITATION_PATTERN =
            Pattern.compile("^([가-힣]+)\\s*(\\d+)\\s*:\\s*([\\d,\\-]+)$");

    private static final Pattern MARKED_CITATION_PATTERN =
            Pattern.compile("^([가-힣]+)\\s*(\\d+)\\s*:\\s*(\\d+)(.*)$");

    private static final Pattern TRAILING_VERSE_PATTERN =
            Pattern.compile("[-,](\\d+)$");

    private CitationParser() {
    }

    static ParsedCitation parseReferenceCitation(String referenceRaw) {
        return parseCitation(referenceRaw);
    }

    static ParsedCitation parseCitation(String citation) {
        String trimmed = citation.trim();

        Matcher plain = PLAIN_CITATION_PATTERN.matcher(trimmed);
        if (plain.matches()) {
            return new ParsedCitation(plain.group(1), plain.group(2), expandVerses(plain.group(3)), "", "");
        }

        return parseMarkedCitation(trimmed);
    }

    private static ParsedCitation parseMarkedCitation(String citation) {
        Matcher marked = MARKED_CITATION_PATTERN.matcher(citation);
        if (!marked.matches()) {
            return null;
        }

        String bookFull = marked.group(1);
        String chapter = marked.group(2);
        int firstVerse = Integer.parseInt(marked.group(3));
        String rest = marked.group(4).trim();

        String marker = "";
        if (!rest.isEmpty() && Character.isLetter(rest.charAt(0)) && rest.charAt(0) < 128) {
            marker = rest.substring(0, 1);
            rest = rest.substring(1).trim();
        }

        List<Integer> verses = new ArrayList<>();
        verses.add(firstVerse);

        Matcher trailing = TRAILING_VERSE_PATTERN.matcher(rest);
        if (trailing.find()) {
            int lastVerse = Integer.parseInt(trailing.group(1));
            char separator = rest.charAt(trailing.start());
            if (separator == '-') {
                for (int v = firstVerse + 1; v <= lastVerse; v++) {
                    verses.add(v);
                }
            } else {
                verses.add(lastVerse);
            }
            rest = rest.substring(0, trailing.start()).trim();
        }

        String extra = rest.replaceAll("[()~]", "").trim();

        return new ParsedCitation(bookFull, chapter, verses, marker, extra);
    }

    static List<Integer> expandVerses(String versePart) {
        List<Integer> verses = new ArrayList<>();
        for (String token : versePart.split(",")) {
            String[] bounds = token.trim().split("-");
            int from = Integer.parseInt(bounds[0]);
            int to = Integer.parseInt(bounds[bounds.length - 1]);
            for (int v = from; v <= to; v++) {
                verses.add(v);
            }
        }
        return verses;
    }

    static String formatVerses(List<Integer> verses, String marker) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < verses.size()) {
            int runStart = i;
            while (i + 1 < verses.size() && verses.get(i + 1) == verses.get(i) + 1) {
                i++;
            }
            if (result.length() > 0) {
                result.append(",");
            }
            result.append(verses.get(runStart));
            if (runStart == 0) {
                result.append(marker);
            }
            if (i > runStart) {
                result.append("-").append(verses.get(i));
            }
            i++;
        }
        return result.toString();
    }

    static String trimFrom(String verseText, String extra) {
        if (extra.isEmpty()) {
            return verseText;
        }

        String strippedText = verseText.replaceAll("\\s", "");
        String strippedExtra = extra.replaceAll("\\s", "");

        int strippedIndex = strippedText.indexOf(strippedExtra);
        if (strippedIndex < 0) {
            return verseText;
        }

        int seen = 0;
        for (int i = 0; i < verseText.length(); i++) {
            if (seen == strippedIndex) {
                return verseText.substring(i);
            }
            if (!Character.isWhitespace(verseText.charAt(i))) {
                seen++;
            }
        }
        return verseText;
    }
}