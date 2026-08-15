package sermon;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public final class DocxBuilder {

    private static final boolean KOREAN_TITLE_BOLD = true;
    private static final int KOREAN_TITLE_FONT_SIZE = 14;
    private static final boolean ENGLISH_TITLE_BOLD = false;
    private static final int ENGLISH_TITLE_FONT_SIZE = 13;
    private static final String SECTION_HEADER_TEXT = "<말씀 차례>";
    private static final boolean SECTION_HEADER_BOLD = true;
    private static final int SECTION_HEADER_FONT_SIZE = 11;
    private static final boolean SECTION_TITLE_KOREAN_BOLD = true;
    private static final int SECTION_TITLE_KOREAN_FONT_SIZE = 11;
    private static final boolean SECTION_TITLE_ENGLISH_BOLD = false;
    private static final int SECTION_TITLE_ENGLISH_FONT_SIZE = 11;
    private static final boolean REFERENCE_HEADER_BOLD = true;
    private static final int REFERENCE_HEADER_FONT_SIZE = 11;
    private static final boolean VERSE_TEXT_BOLD = false;
    private static final int VERSE_TEXT_FONT_SIZE = 11;
    private static final boolean BODY_TITLE_KOREAN_BOLD = true;
    private static final int BODY_TITLE_KOREAN_FONT_SIZE = 11;
    private static final boolean BODY_TITLE_ENGLISH_BOLD = false;
    private static final int BODY_TITLE_ENGLISH_FONT_SIZE = 11;

    private static final String BIBLE_RESOURCE_PATH = "/data/bible_ko.json";
    private static final java.util.regex.Pattern REFERENCE_HEADER_PATTERN =
            java.util.regex.Pattern.compile("([가-힣]+)\\s*(\\d+)\\s*:\\s*(\\d+)-(\\d+)");
    private static final java.util.regex.Pattern PLAIN_CITATION_PATTERN =
            java.util.regex.Pattern.compile("^([가-힣]+)\\s*(\\d+):(\\d+)(?:-(\\d+))?$");
    private static final java.util.regex.Pattern MARKED_CITATION_PATTERN =
            java.util.regex.Pattern.compile("^([가-힣]+)\\s*(\\d+):(\\d+)(.*)$");


    private DocxBuilder() {
    }


    public static XWPFDocument build(ParsedSermon sermon) throws java.io.IOException {
        XWPFDocument document = new XWPFDocument();

        // 제목
        writeTitleSection(document, sermon.koreanTitle(), sermon.englishTitle());

        // 말씀차례
        writeSectionTitleBlock(document, sermon.sectionTitleLines());

        // 본문
        writeReferenceBlock(document, sermon.referenceRaw());
        writeBlocks(document, sermon.blocks());

        return document;
    }

    // 제목

    static void writeTitleSection(XWPFDocument document, String koreanTitle, String englishTitle) {
        writeTitleLine(document, koreanTitle, KOREAN_TITLE_BOLD, KOREAN_TITLE_FONT_SIZE, ParagraphAlignment.CENTER, true);
        writeTitleLine(document, englishTitle, ENGLISH_TITLE_BOLD, ENGLISH_TITLE_FONT_SIZE, ParagraphAlignment.CENTER, false);
    }

    static void writeTitleLine(XWPFDocument document, String text, boolean bold, int fontSize, ParagraphAlignment alignment, boolean pageBreak) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        if (pageBreak) {
            paragraph.setPageBreak(true);
        }

        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
    }

    // 말씀차례
    static void writeSectionTitleBlock(XWPFDocument document, java.util.List<String> sectionTitleLines) {
        writeTitleLine(document, SECTION_HEADER_TEXT, SECTION_HEADER_BOLD, SECTION_HEADER_FONT_SIZE, ParagraphAlignment.LEFT, false);

        for (int i = 0; i < sectionTitleLines.size(); i += 2) {
            String koreanLine = sectionTitleLines.get(i);
            String englishLine = sectionTitleLines.get(i + 1);

            writeTitleLine(document, koreanLine, SECTION_TITLE_KOREAN_BOLD, SECTION_TITLE_KOREAN_FONT_SIZE, ParagraphAlignment.LEFT, false);
            writeTitleLine(document, englishLine, SECTION_TITLE_ENGLISH_BOLD, SECTION_TITLE_ENGLISH_FONT_SIZE, ParagraphAlignment.LEFT, false);
        }
    }

    // 본문
    static void writeReferenceBlock(XWPFDocument document, String referenceRaw) throws java.io.IOException {
        writeTitleLine(document, referenceRaw, REFERENCE_HEADER_BOLD, REFERENCE_HEADER_FONT_SIZE, ParagraphAlignment.LEFT, false);

        java.util.regex.Matcher matcher = REFERENCE_HEADER_PATTERN.matcher(referenceRaw);
        if (matcher.find()) {
            String bookFull = matcher.group(1);
            String chapter = matcher.group(2);
            int startVerse = Integer.parseInt(matcher.group(3));
            int endVerse = Integer.parseInt(matcher.group(4));

            for (int verseNumber = startVerse; verseNumber <= endVerse; verseNumber++) {
                String verseText = BibleData.lookupVerse(BIBLE_RESOURCE_PATH, bookFull, chapter, String.valueOf(verseNumber));
                writeTitleLine(document, verseNumber + ". " + verseText, VERSE_TEXT_BOLD, VERSE_TEXT_FONT_SIZE, ParagraphAlignment.LEFT, false);
            }
        }
    }

    static void writeBlocks(XWPFDocument document, java.util.List<java.util.List<String>> blocks) throws java.io.IOException {
        for (java.util.List<String> block : blocks) {
            String koreanTitle = block.get(0);
            String englishTitle = block.get(1);

            writeTitleLine(document, koreanTitle, BODY_TITLE_KOREAN_BOLD, BODY_TITLE_KOREAN_FONT_SIZE, ParagraphAlignment.LEFT, true);
            writeTitleLine(document, englishTitle, BODY_TITLE_ENGLISH_BOLD, BODY_TITLE_ENGLISH_FONT_SIZE, ParagraphAlignment.LEFT, false);

            for (int i = 2; i < block.size(); i++) {
                writeVersesByNumber(document, block.get(i));
            }
        }
    }

    static void writeVersesByNumber(XWPFDocument document, String citation) throws java.io.IOException {
        String trimmed = citation.trim();

        java.util.regex.Matcher matcher = PLAIN_CITATION_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String bookFull = matcher.group(1);
            String chapter = matcher.group(2);
            int startVerse = Integer.parseInt(matcher.group(3));
            int endVerse = (matcher.group(4) != null) ? Integer.parseInt(matcher.group(4)) : startVerse;

            for (int verseNumber = startVerse; verseNumber <= endVerse; verseNumber++) {
                String verseText = BibleData.lookupVerse(BIBLE_RESOURCE_PATH, bookFull, chapter, String.valueOf(verseNumber));
                writeTitleLine(document, verseNumber + ". " + verseText, VERSE_TEXT_BOLD, VERSE_TEXT_FONT_SIZE, ParagraphAlignment.LEFT, false);
            }
            return;
        }

        writeMarkedVerse(document, trimmed);
    }

    static void writeMarkedVerse(XWPFDocument document, String citation) throws java.io.IOException {
        java.util.regex.Matcher matcher = MARKED_CITATION_PATTERN.matcher(citation.trim());
        if (!matcher.matches()) {
            return;
        }

        String bookFull = matcher.group(1);
        String chapter = matcher.group(2);
        int startVerse = Integer.parseInt(matcher.group(3));
        String rest = matcher.group(4).trim();

        rest = rest.replaceAll("[()]", "").trim();

        int endVerse = startVerse;
        java.util.regex.Matcher range = java.util.regex.Pattern.compile("-(\\d+)$").matcher(rest);
        if (range.find()) {
            endVerse = Integer.parseInt(range.group(1));
            rest = rest.substring(0, range.start()).trim();
        }

        String marker = rest.isEmpty() ? "" : rest.substring(0, 1);
        String extra = rest.isEmpty() ? "" : rest.substring(1).replaceAll("~", "").trim();

        String header = bookFull + " " + chapter + ":" + startVerse + marker + (endVerse != startVerse ? "-" + endVerse : "");
        writeTitleLine(document, header, BODY_TITLE_KOREAN_BOLD, BODY_TITLE_KOREAN_FONT_SIZE, ParagraphAlignment.LEFT, false);

        for (int v = startVerse; v <= endVerse; v++) {
            String verseText = BibleData.lookupVerse(BIBLE_RESOURCE_PATH, bookFull, chapter, String.valueOf(v));
            if (v == startVerse && !extra.isEmpty()) {
                int index = verseText.indexOf(extra);
                if (index >= 0) {
                    verseText = verseText.substring(index);
                }
            }
            writeTitleLine(document, v + ". " + verseText, VERSE_TEXT_BOLD, VERSE_TEXT_FONT_SIZE, ParagraphAlignment.LEFT, false);
        }
    }
}

