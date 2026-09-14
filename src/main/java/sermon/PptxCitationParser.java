package sermon;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;

import javax.xml.namespace.QName;
import java.util.List;

final class PptxCitationParser {

    private static final String BIBLE_RESOURCE_PATH_KOREAN = "/data/bible_ko.json";
    private static final String BIBLE_RESOURCE_PATH_ENGLISH = "/data/bible_en.json";

    private static final String DRAWING_NAMESPACE = "http://schemas.openxmlformats.org/drawingml/2006/main";

    private PptxCitationParser() {}

    static XSLFSlide copySlideFromTemplate(XMLSlideShow output, XMLSlideShow template, int slideIndex) {
        XSLFSlide newSlide = output.createSlide();
        newSlide.importContent(template.getSlides().get(slideIndex));
        restoreLineBreaks(newSlide);
        return newSlide;
    }

    static void restoreLineBreaks(XSLFSlide slide) {
        for (XSLFShape shape : slide.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }

            for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                CTTextParagraph ctParagraph = paragraph.getXmlObject();

                for (int i = ctParagraph.sizeOfRArray() - 1; i >= 0; i--) {
                    CTRegularTextRun run = ctParagraph.getRArray(i);
                    if (run.getT() != null) {
                        continue;
                    }

                    XmlCursor cursor = run.newCursor();
                    cursor.removeXml();
                    cursor.beginElement(new QName(DRAWING_NAMESPACE, "br"));
                    cursor.dispose();
                }
            }
        }
    }

    static void insertText(XSLFSlide slide, int shapeIndex, int paragraphIndex, String text) {
        XSLFTextShape textShape = (XSLFTextShape) slide.getShapes().get(shapeIndex);
        XSLFTextParagraph paragraph = textShape.getTextParagraphs().get(paragraphIndex);
        List<XSLFTextRun> runs = paragraph.getTextRuns();

        runs.get(0).setText(text);

        for (int i = runs.size() - 1; i >= 1; i--) {
            paragraph.removeTextRun(runs.get(i));
        }
    }

    static void insertExtraPptx(XMLSlideShow output, XMLSlideShow hymnPptx) {
        for (XSLFSlide sourceSlide : hymnPptx.getSlides()) {
            XSLFSlide newSlide = output.createSlide();
            newSlide.importContent(sourceSlide);
            restoreLineBreaks(newSlide);
        }
    }

    static String[] lookupVersePair(String bookKorean, String chapter, String verseNumber) throws Exception {
        String verseKorean = BibleData.lookupVerse(BIBLE_RESOURCE_PATH_KOREAN, bookKorean, chapter, verseNumber);
        String verseEnglish = BibleData.lookupVerse(BIBLE_RESOURCE_PATH_ENGLISH, bookKorean, chapter, verseNumber);
        return new String[]{verseKorean, verseEnglish};
    }
}