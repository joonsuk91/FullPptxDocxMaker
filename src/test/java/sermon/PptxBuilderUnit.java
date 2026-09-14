package sermon;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PptxBuilderUnit {

    private static final String TEMPLATE_PATH = "/template.pptx";
    private static final String OUTPUT_PATH = "build-output.pptx";

    private static final String KOREAN_TITLE = "하나가 되라";
    private static final String ENGLISH_TITLE = "Be United";

    private static final String DOXOLOGY_CHAPTER = "5장";
    private static final String RESPONSIVE_READING_NUMBER = "1";
    private static final String PRAISE_CHAPTER = "26장";
    private static final String CONFESSION_PRAY_REFERENCE = "요한일서 1:9";
    private static final String HYMN_CHAPTER = "258장";
    private static final String PRAYER_NAME = "땡땡땡 집사";
    private static final String CLOSING_HYMN_CHAPTER = "218장";

    private static final List<String> SECTION_TITLE_LINES = List.of(
            "1. 겸손으로 하나되라",
            "1. Be United in humility",
            "2. 성령으로 하나 되라",
            "2. Be United in the Holy Spirit",
            "3. 한분 (삼위일체) 하나님을 신뢰하라",
            "3. Trust in God (Trinity)"
    );

    private static final String REFERENCE_RAW = "에베소서4:1-6";

    private static final List<List<String>> BLOCKS = List.of(
            List.of(
                    "1. 겸손으로 하나되라",
                    "1. Be United in humility",
                    "에베소서 4:1-2",
                    "로마서 8:30",
                    "빌립보서 2:6-8",
                    "야고보서 4:6b하나님이 교만한 자를~~",
                    "베드로전서 5:5b(하나님은 교만한자를 대적~~)-6",
                    "마태복음 16:24,27"
            ),
            List.of(
                    "2. 성령으로 하나 되라",
                    "2. Be United in the Holy Spirit",
                    "에베소서 4:3-4",
                    "에베소서 1:21-23",
                    "로마서 8:8-9",
                    "고린도전서 6:19-20",
                    "고린도전서 12:13b 다 한성령으로~~~",
                    "에베소서 2:19한 시민이요~~",
                    "요한일서 2:27",
                    "빌립보서 3:3,8",
                    "요한계시록 2:3-5,7"
            ),
            List.of(
                    "3. 한분 (삼위일체) 하나님을 신뢰하라",
                    "3. Trust in God (Trinity)",
                    "에베소서 4:5-6",
                    "요한복음 10:28-30",
                    "사도행전 10:38",
                    "요한일서 5:20b",
                    "고린도전서 2:10",
                    "고린도전서 11:23-26",
                    "빌립보서 3:21"
            )
    );

    private static final List<String> CITATION_HEADERS = List.of(
            "에베소서 4:1-6",
            "에베소서 4:1-2", "로마서 8:30", "빌립보서 2:6-8", "야고보서 4:6b",
            "베드로전서 5:5b-6", "마태복음 16:24,27",
            "에베소서 4:3-4", "에베소서 1:21-23", "로마서 8:8-9", "고린도전서 6:19-20",
            "고린도전서 12:13b", "에베소서 2:19", "요한일서 2:27", "빌립보서 3:3,8",
            "요한계시록 2:3-5,7",
            "에베소서 4:5-6", "요한복음 10:28-30", "사도행전 10:38", "요한일서 5:20b",
            "고린도전서 2:10", "고린도전서 11:23-26", "빌립보서 3:21"
    );

    private static XMLSlideShow createHymnPptx() {
        XMLSlideShow hymnPptx = new XMLSlideShow();
        hymnPptx.createSlide();
        return hymnPptx;
    }

    private static List<String> collectTexts(XMLSlideShow output) {
        List<String> texts = new ArrayList<>();

        for (XSLFSlide slide : output.getSlides()) {
            StringBuilder result = new StringBuilder();
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    result.append(textShape.getText()).append("\n");
                }
            }
            texts.add(result.toString());
        }

        return texts;
    }

    private static boolean containsAnywhere(List<String> texts, String keyword) {
        for (String text : texts) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void build() throws Exception {
        XMLSlideShow template;
        try (InputStream in = PptxBuilderUnit.class.getResourceAsStream(TEMPLATE_PATH)) {
            template = new XMLSlideShow(in);
        }

        XMLSlideShow doxologyPptx = createHymnPptx();
        XMLSlideShow praisePptx = createHymnPptx();
        XMLSlideShow hymnPptx = createHymnPptx();
        XMLSlideShow closingHymnPptx = createHymnPptx();

        ParsedSermon sermon = new ParsedSermon(
                KOREAN_TITLE, ENGLISH_TITLE, SECTION_TITLE_LINES, REFERENCE_RAW, BLOCKS);

        XMLSlideShow output = PptxBuilder.build(
                template, KOREAN_TITLE, ENGLISH_TITLE, DOXOLOGY_CHAPTER, RESPONSIVE_READING_NUMBER,
                PRAISE_CHAPTER, CONFESSION_PRAY_REFERENCE, HYMN_CHAPTER, PRAYER_NAME,
                sermon, CLOSING_HYMN_CHAPTER,
                doxologyPptx, praisePptx, hymnPptx, closingHymnPptx);

        try (FileOutputStream out = new FileOutputStream(OUTPUT_PATH)) {
            output.write(out);
        }

        List<String> texts = collectTexts(output);

        assertTrue(containsAnywhere(texts, KOREAN_TITLE), KOREAN_TITLE);
        assertTrue(containsAnywhere(texts, ENGLISH_TITLE), ENGLISH_TITLE);
        assertTrue(containsAnywhere(texts, PRAYER_NAME), PRAYER_NAME);
        assertTrue(containsAnywhere(texts, "(" + CONFESSION_PRAY_REFERENCE + ")"), CONFESSION_PRAY_REFERENCE);
        assertTrue(containsAnywhere(texts, DOXOLOGY_CHAPTER), DOXOLOGY_CHAPTER);
        assertTrue(containsAnywhere(texts, PRAISE_CHAPTER), PRAISE_CHAPTER);
        assertTrue(containsAnywhere(texts, HYMN_CHAPTER), HYMN_CHAPTER);
        assertTrue(containsAnywhere(texts, CLOSING_HYMN_CHAPTER), CLOSING_HYMN_CHAPTER);

        assertTrue(containsAnywhere(texts, "복 있는 사람은"), "성시교독 첫 줄");
        assertTrue(containsAnywhere(texts, "무릇 의인들의 길은"), "성시교독 마지막 줄");

        for (String title : SECTION_TITLE_LINES) {
            assertTrue(containsAnywhere(texts, title), title);
        }

        for (String header : CITATION_HEADERS) {
            assertTrue(containsAnywhere(texts, header), header);
        }

        assertFalse(containsAnywhere(texts, "장장"), "장 중복");

        assertEquals(95, output.getSlides().size());
    }
}