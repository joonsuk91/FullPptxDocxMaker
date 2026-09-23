package sermon;

import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.List;

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
    private static final String BIBLE_RESOURCE_PATH_ENGLISH = "/data/bible_en.json";

    private DocxBuilder() {
    }

    public static XWPFDocument build(ParsedSermon sermon) throws java.io.IOException {
        XWPFDocument document = new XWPFDocument();

        writeTitleSection(document, sermon.koreanTitle(), sermon.englishTitle());
        writeBlankLine(document);
        writeSectionTitleBlock(document, sermon.sectionTitleLines());
        writeBlankLine(document);
        writeReferenceBlock(document, sermon.referenceRaw());
        writeBlocks(document, sermon.blocks());
        writePageBreak(document);

        return document;
    }

    static void writeTitleSection(XWPFDocument document, String koreanTitle, String englishTitle) {
        writeTitleLine(document, koreanTitle, KOREAN_TITLE_BOLD, KOREAN_TITLE_FONT_SIZE, ParagraphAlignment.CENTER);
        writeTitleLine(document, englishTitle, ENGLISH_TITLE_BOLD, ENGLISH_TITLE_FONT_SIZE, ParagraphAlignment.CENTER);
    }

    private static final String KOREAN_FONT = "맑은 고딕";

    static void writeTitleLine(XWPFDocument document, String text, boolean bold, int fontSize, ParagraphAlignment alignment) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);

        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily(KOREAN_FONT);
    }

    static void writeBlankLine(XWPFDocument document) {
        writeTitleLine(document, "", false, VERSE_TEXT_FONT_SIZE, ParagraphAlignment.LEFT);
    }

    static void writePageBreak(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.createRun().addBreak(BreakType.PAGE);
    }

    static void writeSectionTitleBlock(XWPFDocument document, List<String> sectionTitleLines) {
        writeTitleLine(document, SECTION_HEADER_TEXT, SECTION_HEADER_BOLD, SECTION_HEADER_FONT_SIZE, ParagraphAlignment.LEFT);

        for (int i = 0; i < sectionTitleLines.size(); i += 2) {
            writeTitleLine(document, sectionTitleLines.get(i), SECTION_TITLE_KOREAN_BOLD, SECTION_TITLE_KOREAN_FONT_SIZE, ParagraphAlignment.LEFT);
            writeTitleLine(document, sectionTitleLines.get(i + 1), SECTION_TITLE_ENGLISH_BOLD, SECTION_TITLE_ENGLISH_FONT_SIZE, ParagraphAlignment.LEFT);
        }
    }

    static void writeReferenceBlock(XWPFDocument document, String referenceRaw) throws java.io.IOException {
        writeTitleLine(document, referenceRaw, REFERENCE_HEADER_BOLD, REFERENCE_HEADER_FONT_SIZE, ParagraphAlignment.LEFT);

        ParsedCitation parsed = CitationParser.parseReferenceCitation(referenceRaw);
        if (parsed == null) {
            return;
        }

        for (int verseNumber : parsed.verses()) {
            writeVersePair(document, parsed.bookFull(), parsed.chapter(), verseNumber, "");
        }
    }

    static void writeBlocks(XWPFDocument document, List<List<String>> blocks) throws java.io.IOException {
        for (List<String> block : blocks) {
            writePageBreak(document);

            writeTitleLine(document, block.get(0), BODY_TITLE_KOREAN_BOLD, BODY_TITLE_KOREAN_FONT_SIZE, ParagraphAlignment.LEFT);
            writeTitleLine(document, block.get(1), BODY_TITLE_ENGLISH_BOLD, BODY_TITLE_ENGLISH_FONT_SIZE, ParagraphAlignment.LEFT);

            for (int i = 2; i < block.size(); i++) {
                writeBlankLine(document);
                writeVersesByNumber(document, block.get(i));
            }
        }
    }

    static void writeVersesByNumber(XWPFDocument document, String citation) throws java.io.IOException {
        ParsedCitation parsed = CitationParser.parseCitation(citation);
        if (parsed == null) {
            return;
        }

        writeCitationBlock(document, parsed);
    }

    static void writeCitationBlock(XWPFDocument document, ParsedCitation parsed) throws java.io.IOException {
        String header = parsed.bookFull() + " " + parsed.chapter() + ":"
                + CitationParser.formatVerses(parsed.verses(), parsed.marker());
        writeTitleLine(document, header, BODY_TITLE_KOREAN_BOLD, BODY_TITLE_KOREAN_FONT_SIZE, ParagraphAlignment.LEFT);

        int firstVerse = parsed.verses().get(0);

        for (int verseNumber : parsed.verses()) {
            String extra = (verseNumber == firstVerse) ? parsed.extra() : "";
            writeVersePair(document, parsed.bookFull(), parsed.chapter(), verseNumber, extra);
        }
    }

    static void writeVersePair(XWPFDocument document, String bookFull, String chapter, int verseNumber, String extra) throws java.io.IOException {
        String verseText = BibleData.lookupVerse(BIBLE_RESOURCE_PATH, bookFull, chapter, String.valueOf(verseNumber));
        String verseTextEnglish = BibleData.lookupVerse(BIBLE_RESOURCE_PATH_ENGLISH, bookFull, chapter, String.valueOf(verseNumber));

        verseText = CitationParser.trimFrom(verseText, extra);

        writeTitleLine(document, verseNumber + ". " + verseText, VERSE_TEXT_BOLD, VERSE_TEXT_FONT_SIZE, ParagraphAlignment.LEFT);
        writeTitleLine(document, verseNumber + ". " + verseTextEnglish, VERSE_TEXT_BOLD, VERSE_TEXT_FONT_SIZE, ParagraphAlignment.LEFT);
    }
}