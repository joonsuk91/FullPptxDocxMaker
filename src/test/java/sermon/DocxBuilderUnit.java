package sermon;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxBuilderUnit {

    private static final String OUTPUT_PATH = "build-output.docx";

    private static final String KOREAN_TITLE = "하나가 되라";
    private static final String ENGLISH_TITLE = "Be United";

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

    @Test
    void build() throws Exception {
        ParsedSermon sermon = new ParsedSermon(
                KOREAN_TITLE, ENGLISH_TITLE, SECTION_TITLE_LINES, REFERENCE_RAW, BLOCKS);

        XWPFDocument document = DocxBuilder.build(sermon, null);

        try (FileOutputStream out = new FileOutputStream(OUTPUT_PATH)) {
            document.write(out);
        }

        List<String> lines = document.getParagraphs().stream()
                .map(paragraph -> paragraph.getText())
                .toList();

        assertEquals(KOREAN_TITLE, lines.get(0));
        assertEquals(ENGLISH_TITLE, lines.get(1));
        assertEquals("", lines.get(2));

        assertEquals("<말씀 차례>", lines.get(3));
        assertEquals("1. 겸손으로 하나되라", lines.get(4));
        assertEquals("3. Trust in God (Trinity)", lines.get(9));

        assertEquals(REFERENCE_RAW, lines.get(10));
        assertTrue(lines.get(11).startsWith("1. "));
        assertTrue(lines.get(22).startsWith("6. "));

        assertEquals("1. 겸손으로 하나되라", lines.get(24));
        assertEquals("1. Be United in humility", lines.get(25));

        assertEquals("에베소서 4:1-2", lines.get(26));
        assertEquals("로마서 8:30", lines.get(31));
        assertEquals("빌립보서 2:6-8", lines.get(34));
        assertEquals("야고보서 4:6b", lines.get(41));
        assertEquals("베드로전서 5:5b-6", lines.get(44));

        assertEquals("마태복음 16:24,27", lines.get(49));
        assertTrue(lines.get(50).startsWith("24. "));
        assertTrue(lines.get(52).startsWith("27. "));

        assertEquals("2. 성령으로 하나 되라", lines.get(55));
        assertEquals("에베소서 4:3-4", lines.get(57));
        assertEquals("요한계시록 2:3-5,7", lines.get(93));

        assertEquals("3. 한분 (삼위일체) 하나님을 신뢰하라", lines.get(103));
        assertEquals("에베소서 4:5-6", lines.get(105));
        assertEquals("빌립보서 3:21", lines.get(135));

        assertEquals(139, lines.size());
    }
}