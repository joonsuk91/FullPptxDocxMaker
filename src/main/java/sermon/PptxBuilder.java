package sermon;

import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

final class PptxBuilder {

    static XMLSlideShow build(XMLSlideShow template, String koreanTitle, String englishTitle, String doxologyChapter, String responsiveReadingNumber, String praiseChapter, String confessionPrayReference, String hymnChapter, String prayerName, ParsedSermon sermon, String closingHymnChapter, XMLSlideShow doxologyHymnPptx, XMLSlideShow praiseHymnPptx, XMLSlideShow hymnPptx, XMLSlideShow closingHymnPptx, String churchNews) throws Exception {
        XMLSlideShow output = copyTemplateWithoutSlides(template);

        writeTitleSlide(output, template, koreanTitle, englishTitle);
        writeConfessionSlides(output, template);
        writeChapterTitleWithHymnSlides(output, template, 11, doxologyChapter, doxologyHymnPptx);
        writeResponsiveReadingSlides(output, template, responsiveReadingNumber);
        writeChapterTitleWithHymnSlides(output, template, 21, praiseChapter, praiseHymnPptx);
        writeConfessionPraySlide(output, template, confessionPrayReference);
        writeChapterTitleWithHymnSlides(output, template, 31, hymnChapter, hymnPptx);
        writePrayerSlide(output, template, prayerName);
        writeTitleSlide(output, template, koreanTitle, englishTitle);
        writeSermonBodySlides(output, sermon);
        writeTitleSlide(output, template, koreanTitle, englishTitle);
        writeChapterTitleWithHymnSlides(output, template, 213, closingHymnChapter, closingHymnPptx);
        writeLastSlides(output, template);
        writeChapterTitleWithHymnSlides(output, template, 241, hymnChapter, hymnPptx);
        writeFinalSlides(output, template);
        writeTitleSlide(output, template, koreanTitle, englishTitle);
        writeChurchNewsSlide(output, churchNews);

        return output;
    }

    static void writeChurchNewsSlide(XMLSlideShow output, String churchNews) {
        if (churchNews == null || churchNews.isBlank()) {
            return;
        }

        XSLFSlide slide = output.createSlide();
        XSLFTextBox textBox = slide.createTextBox();
        textBox.setAnchor(new Rectangle2D.Double(0, 0, output.getPageSize().getWidth(), output.getPageSize().getHeight()));
        textBox.setVerticalAlignment(VerticalAlignment.TOP);

        addTextParagraph(textBox, "<교회소식>", 32.0, true);
        addTextParagraph(textBox, "", 28.0, false);

        for (String line : churchNews.split("\\R")) {
            addTextParagraph(textBox, line, 28.0, false);
        }
    }

    static XMLSlideShow copyTemplateWithoutSlides(XMLSlideShow template) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        template.write(buffer);

        XMLSlideShow output = new XMLSlideShow(new ByteArrayInputStream(buffer.toByteArray()));
        for (int i = output.getSlides().size() - 1; i >= 0; i--) {
            output.removeSlide(i);
        }
        return output;
    }

    static void writeTitleSlide(XMLSlideShow output, XMLSlideShow template, String koreanTitle, String englishTitle) {
        XSLFSlide slide = PptxCitationParser.copySlideFromTemplate(output, template, 0);
        PptxCitationParser.insertText(slide, 1, 0, koreanTitle);
        PptxCitationParser.insertText(slide, 1, 1, englishTitle);
    }

    static void writeConfessionSlides(XMLSlideShow output, XMLSlideShow template) {
        for (int i = 1; i <= 10; i++) {
            PptxCitationParser.copySlideFromTemplate(output, template, i);
        }
    }

    static void writeChapterTitleWithHymnSlides(XMLSlideShow output, XMLSlideShow template, int slideIndex, String chapter, XMLSlideShow hymnPptx) {
        XSLFSlide slide = PptxCitationParser.copySlideFromTemplate(output, template, slideIndex);
        PptxCitationParser.insertText(slide, 0, 2, chapter);
        PptxCitationParser.insertExtraPptx(output, hymnPptx);
    }

    static void writeResponsiveReadingSlides(XMLSlideShow output, XMLSlideShow template, String number) throws Exception {
        XSLFSlide titleSlide = PptxCitationParser.copySlideFromTemplate(output, template, 15);
        PptxCitationParser.insertText(titleSlide, 0, 2, number);

        List<ResponsiveReading.Line> lines = ResponsiveReading.lookup(number);

        for (int i = 0; i < lines.size(); i += 2) {
            XSLFSlide contentSlide = output.createSlide();
            XSLFTextBox textBox = contentSlide.createTextBox();
            textBox.setAnchor(new Rectangle2D.Double(0, 0, output.getPageSize().getWidth(), 100));

            addResponsiveReadingLine(textBox, lines.get(i));

            if (i + 1 < lines.size()) {
                addTextParagraph(textBox, "", 44.0, false);
                addResponsiveReadingLine(textBox, lines.get(i + 1));
            }

            textBox.resizeToFitText();
        }
    }

    static void addResponsiveReadingLine(XSLFTextShape textShape, ResponsiveReading.Line line) {
        boolean bold = line.role.equals("Congregation");
        addTextParagraph(textShape, line.korean, 44.0, bold);
        addTextParagraph(textShape, line.english, 44.0, bold);
    }

    static void addTextParagraph(XSLFTextShape textShape, String text, double fontSize, boolean bold) {
        if (text == null || text.isEmpty()) {
            textShape.addNewTextParagraph().addNewTextRun().setText("");
            return;
        }

        XSLFTextParagraph paragraph = textShape.addNewTextParagraph();

        try {
            org.apache.xmlbeans.XmlCursor cursor = paragraph.getXmlObject().newCursor();
            String uri = "http://schemas.openxmlformats.org/drawingml/2006/main";

            javax.xml.namespace.QName pPr = new javax.xml.namespace.QName(uri, "pPr");
            if (!cursor.toChild(pPr)) {
                cursor.toFirstContentToken();
                cursor.insertElement(pPr);
                cursor.toPrevSibling();
            }

            javax.xml.namespace.QName defRPr = new javax.xml.namespace.QName(uri, "defRPr");
            if (!cursor.toChild(defRPr)) {
                cursor.toFirstContentToken();
                cursor.insertElement(defRPr);
                cursor.toPrevSibling();
            }

            javax.xml.namespace.QName bAttr = new javax.xml.namespace.QName("", "b");
            cursor.removeAttribute(bAttr);
            cursor.insertAttributeWithValue(bAttr, bold ? "1" : "0");

            cursor.dispose();
        } catch (Exception ignored) {
        }

        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);

        org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties rPr = run.getRPr(true);
        rPr.addNewLatin().setTypeface("Malgun Gothic");
        rPr.addNewEa().setTypeface("Malgun Gothic");
    }

    static void writeConfessionPraySlide(XMLSlideShow output, XMLSlideShow template, String reference) {
        XSLFSlide slide = PptxCitationParser.copySlideFromTemplate(output, template, 30);
        PptxCitationParser.insertText(slide, 0, 2, "(" + reference.trim() + ")");
    }

    static void writePrayerSlide(XMLSlideShow output, XMLSlideShow template, String name) {
        XSLFSlide slide = PptxCitationParser.copySlideFromTemplate(output, template, 52);
        PptxCitationParser.insertText(slide, 0, 3, name);
    }

    static void writeVerseContentSlide(XMLSlideShow output, String referenceKorean, String referenceEnglish, String verseKorean, String verseEnglish) {
        XSLFSlide slide = output.createSlide();
        XSLFTextBox textBox = slide.createTextBox();
        textBox.setAnchor(new Rectangle2D.Double(0, 0, output.getPageSize().getWidth(), output.getPageSize().getHeight()));
        textBox.setVerticalAlignment(VerticalAlignment.TOP);

        double[] fontSize = chooseVerseFontSizes(verseKorean, output.getPageSize().getWidth());

        addTextParagraph(textBox, referenceKorean, fontSize[2], true);
        addTextParagraph(textBox, verseKorean, fontSize[0], true);
        addTextParagraph(textBox, "", fontSize[0], false);
        addTextParagraph(textBox, referenceEnglish, fontSize[3], true);
        addTextParagraph(textBox, verseEnglish, fontSize[1], false);
    }

    static double[] chooseVerseFontSizes(String koreanText, double pageWidth) {
        if (estimateLineCount(koreanText, 48.0, pageWidth) <= 3) {
            return new double[]{48.0, 37.0, 42.0, 37.0};
        }
        return new double[]{36.0, 28.0, 32.0, 28.0};
    }

    static int estimateLineCount(String text, double fontSize, double usableWidth) {
        return (int) Math.ceil(text.length() / (usableWidth / fontSize));
    }

    static void writeSermonBodySlides(XMLSlideShow output, ParsedSermon sermon) throws Exception {
        writeReferenceBlockSlides(output, sermon.referenceRaw());
        writeSectionOverviewSlide(output, sermon.sectionTitleLines());
        writeBlocksSlides(output, sermon.blocks());
    }

    static void writeReferenceBlockSlides(XMLSlideShow output, String referenceRaw) throws Exception {
        writeCitationSlides(output, referenceRaw);
    }

    static String buildReference(String book, String chapter, ParsedCitation parsed) {
        return book + " " + chapter + ":" + CitationParser.formatVerses(parsed.verses(), parsed.marker());
    }

    static void writeSectionOverviewSlide(XMLSlideShow output, List<String> sectionTitleLines) {
        XSLFSlide slide = output.createSlide();
        XSLFTextBox textBox = slide.createTextBox();
        textBox.setAnchor(new Rectangle2D.Double(0, 0, output.getPageSize().getWidth(), output.getPageSize().getHeight()));
        textBox.setVerticalAlignment(VerticalAlignment.MIDDLE);

        for (int i = 0; i < sectionTitleLines.size(); i += 2) {
            if (i > 0) {
                addTextParagraph(textBox, "", 32.0, false);
            }

            addTextParagraph(textBox, sectionTitleLines.get(i), 32.0, true);
            addTextParagraph(textBox, sectionTitleLines.get(i + 1), 32.0, false);
        }
    }

    static void writeBlocksSlides(XMLSlideShow output, List<List<String>> blocks) throws Exception {
        for (List<String> block : blocks) {
            for (int i = 2; i < block.size(); i++) {
                writeCitationSlides(output, block.get(i));
            }
        }
    }

    static void writeCitationSlides(XMLSlideShow output, String citation) throws Exception {
        ParsedCitation parsed = CitationParser.parseCitation(citation);
        if (parsed == null) {
            return;
        }

        String bookEnglish = BookNamesEnglish.toEnglish(parsed.bookFull());
        String referenceKorean = buildReference(parsed.bookFull(), parsed.chapter(), parsed);
        String referenceEnglish = buildReference(bookEnglish, parsed.chapter(), parsed);
        int firstVerse = parsed.verses().get(0);

        for (int verseNumber : parsed.verses()) {
            String[] versePair = PptxCitationParser.lookupVersePair(parsed.bookFull(), parsed.chapter(), String.valueOf(verseNumber));
            String verseKorean = versePair[0];

            if (verseNumber == firstVerse) {
                verseKorean = CitationParser.trimFrom(verseKorean, parsed.extra());
            }

            writeVerseContentSlide(output, referenceKorean, referenceEnglish,
                    verseNumber + ". " + verseKorean, verseNumber + ". " + versePair[1]);
        }
    }

    static void writeLastSlides(XMLSlideShow output, XMLSlideShow template) {
        for (int i = 229; i <= 240; i++) {
            PptxCitationParser.copySlideFromTemplate(output, template, i);
        }
    }

    static void writeFinalSlides(XMLSlideShow output, XMLSlideShow template) {
        PptxCitationParser.copySlideFromTemplate(output, template, 262);
    }
}