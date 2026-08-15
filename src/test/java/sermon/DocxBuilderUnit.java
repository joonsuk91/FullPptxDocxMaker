package sermon;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class DocxBuilderUnit {

    @Order(1)
    @Test
    void writeTitleSection_제목_() {

        String koreanTitle = "하나가 되라";
        String englishTitle = "Be United";

        XWPFDocument document = new XWPFDocument();
        DocxBuilder.writeTitleSection(document, koreanTitle, englishTitle);

        java.util.List<XWPFParagraph> paragraphs = document.getParagraphs();
        XWPFParagraph koreanParagraph = paragraphs.get(0);
        assertEquals(koreanTitle, koreanParagraph.getText());
        assertEquals(true, koreanParagraph.getRuns().get(0).isBold());
        assertEquals(14, koreanParagraph.getRuns().get(0).getFontSize());
        XWPFParagraph englishParagraph = paragraphs.get(1);
        assertEquals(englishTitle, englishParagraph.getText());
        assertEquals(false, englishParagraph.getRuns().get(0).isBold());
        assertEquals(13, englishParagraph.getRuns().get(0).getFontSize());

    }

    @Order(2)
    @Test
    void writeSectionTitleBlock_말씀차례_() {

        String sectionTitleKorean1 = "1. 겸손으로 하나되라";
        String sectionTitleEnglish1 = "1. Be United in humility";
        String sectionTitleKorean2 = "2. 성령으로 하나 되라";
        String sectionTitleEnglish2 = "2. Be United in the Holy Spirit";

        XWPFDocument document = new XWPFDocument();
        DocxBuilder.writeSectionTitleBlock(document, java.util.List.of(
                sectionTitleKorean1,
                sectionTitleEnglish1,
                sectionTitleKorean2,
                sectionTitleEnglish2
        ));

        java.util.List<XWPFParagraph> paragraphs = document.getParagraphs();
        XWPFParagraph headerParagraph = paragraphs.get(0);
        assertEquals("<말씀 차례>", headerParagraph.getText());
        assertEquals(true, headerParagraph.getRuns().get(0).isBold());
        assertEquals(11, headerParagraph.getRuns().get(0).getFontSize());
        XWPFParagraph sectionKoreanParagraph1 = paragraphs.get(1);
        assertEquals(sectionTitleKorean1, sectionKoreanParagraph1.getText());
        assertEquals(true, sectionKoreanParagraph1.getRuns().get(0).isBold());
        assertEquals(11, sectionKoreanParagraph1.getRuns().get(0).getFontSize());
        XWPFParagraph sectionEnglishParagraph1 = paragraphs.get(2);
        assertEquals(sectionTitleEnglish1, sectionEnglishParagraph1.getText());
        assertEquals(false, sectionEnglishParagraph1.getRuns().get(0).isBold());
        assertEquals(11, sectionEnglishParagraph1.getRuns().get(0).getFontSize());
        XWPFParagraph sectionKoreanParagraph2 = paragraphs.get(3);
        assertEquals(sectionTitleKorean2, sectionKoreanParagraph2.getText());
        assertEquals(true, sectionKoreanParagraph2.getRuns().get(0).isBold());
        assertEquals(11, sectionKoreanParagraph2.getRuns().get(0).getFontSize());
        XWPFParagraph sectionEnglishParagraph2 = paragraphs.get(4);
        assertEquals(sectionTitleEnglish2, sectionEnglishParagraph2.getText());
        assertEquals(false, sectionEnglishParagraph2.getRuns().get(0).isBold());
        assertEquals(11, sectionEnglishParagraph2.getRuns().get(0).getFontSize());

    }

    @Order(3)
    @Test
    void writeReferenceBlock_제목구간성경구절과일반본문_() throws Exception {
        String referenceRaw = "에베소서 4:1-6";
        String bookFull = "에베소서";
        String chapter = "4";
        int startVerse = 1;
        int endVerse = 6;

        XWPFDocument document = new XWPFDocument();
        DocxBuilder.writeReferenceBlock(document, referenceRaw);

        java.util.List<XWPFParagraph> paragraphs = document.getParagraphs();

        XWPFParagraph headerParagraph = paragraphs.get(0);
        assertEquals(referenceRaw, headerParagraph.getText());
        assertEquals(true, headerParagraph.getRuns().get(0).isBold());
        assertEquals(11, headerParagraph.getRuns().get(0).getFontSize());

        for (int verseNumber = startVerse; verseNumber <= endVerse; verseNumber++) {
            String expectedVerseText = verseNumber + ". " + BibleData.lookupVerse("/data/bible_ko.json", bookFull, chapter, String.valueOf(verseNumber));
            XWPFParagraph verseParagraph = paragraphs.get(verseNumber); // 0번은 헤더 줄이라서, 절 번호가 그대로 목록 위치가 됩니다
            assertEquals(expectedVerseText, verseParagraph.getText());
            assertEquals(false, verseParagraph.getRuns().get(0).isBold());
            assertEquals(11, verseParagraph.getRuns().get(0).getFontSize());
        }

    }

    @Order(4)
    @Test
    void writeMarkedVerse_괄호있는설경구절_() throws Exception {
        String citation = "베드로전서 5:5b(하나님은 교만한자를 대적~~)-6";
        String bookFull = "베드로전서";
        String chapter = "5";
        int startVerse = 5;
        int endVerse = 6;
        String marker = "b";

        XWPFDocument document = new XWPFDocument();
        DocxBuilder.writeMarkedVerse(document, citation);

        java.util.List<XWPFParagraph> paragraphs = document.getParagraphs();
        assertEquals(3, paragraphs.size());

        String expectedHeader = bookFull + " " + chapter + ":" + startVerse + marker + "-" + endVerse;
        XWPFParagraph headerParagraph = paragraphs.get(0);
        assertEquals(expectedHeader, headerParagraph.getText());
        assertEquals(true, headerParagraph.getRuns().get(0).isBold());
        assertEquals(11, headerParagraph.getRuns().get(0).getFontSize());

        String expectedVerse5 = startVerse + ". " + BibleData.lookupVerse("/data/bible_ko.json", bookFull, chapter, String.valueOf(startVerse));
        XWPFParagraph verse5Paragraph = paragraphs.get(1);
        assertEquals(expectedVerse5, verse5Paragraph.getText());
        assertEquals(false, verse5Paragraph.getRuns().get(0).isBold());
        assertEquals(11, verse5Paragraph.getRuns().get(0).getFontSize());

        String expectedVerse6 = endVerse + ". " + BibleData.lookupVerse("/data/bible_ko.json", bookFull, chapter, String.valueOf(endVerse));
        XWPFParagraph verse6Paragraph = paragraphs.get(2);
        assertEquals(expectedVerse6, verse6Paragraph.getText());
        assertEquals(false, verse6Paragraph.getRuns().get(0).isBold());
        assertEquals(11, verse6Paragraph.getRuns().get(0).getFontSize());
    }

    @Order(5)
    @Test
    void writeMarkedVerse_괄호없는성경구절_() throws Exception {
        String citation = "고린도전서 12:13b 다 한성령으로~~~";
        String bookFull = "고린도전서";
        String chapter = "12";
        int startVerse = 13;
        String marker = "b";
        String extra = "다 한성령으로";

        XWPFDocument document = new XWPFDocument();
        DocxBuilder.writeMarkedVerse(document, citation);

        java.util.List<XWPFParagraph> paragraphs = document.getParagraphs();

        String expectedHeader = bookFull + " " + chapter + ":" + startVerse + marker;
        XWPFParagraph headerParagraph = paragraphs.get(0);
        assertEquals(expectedHeader, headerParagraph.getText());
        assertEquals(true, headerParagraph.getRuns().get(0).isBold());
        assertEquals(11, headerParagraph.getRuns().get(0).getFontSize());

        String fullVerseText = startVerse + ". " + BibleData.lookupVerse("/data/bible_ko.json", bookFull, chapter, String.valueOf(startVerse));
        int index = fullVerseText.indexOf(extra);
        String expectedBody = index >= 0 ? fullVerseText.substring(index) : fullVerseText;
        XWPFParagraph bodyParagraph = paragraphs.get(1);
        assertEquals(expectedBody, bodyParagraph.getText());
        assertEquals(false, bodyParagraph.getRuns().get(0).isBold());
        assertEquals(11, bodyParagraph.getRuns().get(0).getFontSize());
    }

}

