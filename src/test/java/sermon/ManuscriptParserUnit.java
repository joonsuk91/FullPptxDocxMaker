package sermon;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.security.KeyException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ManuscriptParserUnit {
    // == 설교 원고 기입 ==
    private static final String 원고 = """
             하나가 되라 (엡4:1-6)
             Be United
             1.겸손으로 하나되라
             1. Be United in humility
             1-2
             롬8:30  빌2:6-8 약4:6b하나님이 교만한 자를~~\s
             벧전5:5b(하나님은 교만한자를 대적~~)-6\s
             마16:24,27
             
             2..성령으로 하나 되라
             2. Be United in the Holy Spirit\s
             3-4
             엡1:21-23  롬8:8-9 고전6:19-20
             고전12:13b 다 한성령으로~~~\s
             엡2b:19한 시민이요~~
             요일2:27
             빌3:3,8 계2:3-5,7
             
             3.한분 (삼위일체) 하나님을 신뢰하라
             3. Trust in God (Trinity)\s
             5-6
             요10:28-30 행10:38 요일5:20b
             우리가 참된자 곧 그의 아들~~
             고전2:10\s
             고전11:23-26
             빌3:21
            """;

    // == 제목 함수 ==
    @Order(1)
    @Test
    void extractTitleSection_말씀차례_전까지_돌려주기() {
        java.util.List<String> lines = java.util.List.of(
                "하나님 자녀", "Child of God", "(롬6:6-11)", "하나가 되라 (엡4:1-6)", "1.새 생명으로", "1. Made righteous ");
        java.util.List<String> result = ManuscriptParser.extractTitleSection(lines);
        assertEquals(java.util.List.of("하나님 자녀", "Child of God", "(롬6:6-11)", "하나가 되라 (엡4:1-6)"), result);
    }

    @Order(2)
    @Test
    void extractTitleSection_말씀차레_없으면_오류() {
        java.util.List<String> lines = java.util.List.of("숫자로 시작하는 문장이 없어용", "설교원글이 잘못되어부렀쓰");
        assertThrows(IllegalArgumentException.class, () -> ManuscriptParser.extractTitleSection(lines));
    }

    @Order(3)
    @Test
    void extractReferenceRaw_괄호안_내용_찾기() {
        java.util.List<String> titleLines = java.util.List.of("하나님 자녀", "Child of God", "(롬6:6-11)");
        assertEquals("로마서6:6-11", ManuscriptParser.extractReferenceRaw(titleLines));
    }

    @Order(4)
    @Test
    void extractReferenceRaw_괄호가_없으면_오류() {
        java.util.List<String> titleLines = java.util.List.of("괄호는 없어용", "속아부렀쓰");
        assertThrows(IllegalArgumentException.class, () -> ManuscriptParser.extractReferenceRaw(titleLines));
    }

    @Order(5)
    @Test
    void cleanTitleLines_괄호를_지운다_빈줄이_되면_그것도_함께() {
        java.util.List<String> titleLines = java.util.List.of("(롬6:6-11)", "하나님 자녀", "Child of God");
        java.util.List<String> result = ManuscriptParser.cleanTitleLines(titleLines);
        assertEquals(java.util.List.of("하나님 자녀", "Child of God"), result);
    }

    @Order(6)
    @Test
    void cleanTitleLines_괄호가_붙어있어도_지우기() {
        java.util.List<String> titleLines = java.util.List.of("하나가 되라 (엡4:1-6)", "Child of God");
        java.util.List<String> result = ManuscriptParser.cleanTitleLines(titleLines);
        assertEquals(java.util.List.of("하나가 되라", "Child of God"), result);
    }

    @Order(7)
    @Test
    void validateTitles_한영문_다_있으면_통과() {
        ManuscriptParser.validateTitles(java.util.List.of("한글", "English"));
    }

    @Order(8)
    @Test
    void validateTitles_제목이_모자라면_오류() {
        assertThrows(IllegalArgumentException.class,
                () -> ManuscriptParser.validateTitles(java.util.List.of("한글제목만")));
    }

    // == 말씀차례 함수 ==
    @Order(9)
    @Test
    void extractSectionTitleLines_숫자줄만_순서대로_찾기() {
        java.util.List<String> lines = java.util.List.of("제목", "한글", "영어", "1.ㅇㄴㄹ", "1.osf", "2.ㄷㅈㄹ" ,"2. ewf", "3. ㄷㅇㅁ","3.abc");
        java.util.List<String> result = ManuscriptParser.extractSectionTitleLines(lines);
        assertEquals(java.util.List.of("1. ㅇㄴㄹ", "1. osf", "2. ㄷㅈㄹ", "2. ewf, 3. ㄷㅇㅁ, 3. abc"), result);
    }

    @Order(10)
    @Test
    void extractSectionTitleLines_짝이안맞으면오류() {
        java.util.List<String> lines = java.util.List.of("1.ㄷㄹㄴ", "2.ㄷㄹㅁ", "3.abc");
        assertThrows(IllegalArgumentException.class, () -> ManuscriptParser.extractSectionTitleLines(lines));
    }

    @Order(11)
    @Test
    void extractSectionTitleLines_점오타_정규화() {
        java.util.List<String> lines = java.util.List.of("1.한글", "1. english", "2..한글", "2. english");
        java.util.List<String> result = ManuscriptParser.extractSectionTitleLines(lines);
        assertEquals(java.util.List.of("1. 한글", "1. english", "2. 한글", "2. english"), result);
    }

    @Order(12)
    @Test
    void insertBlankLineBetweenSections_각대지_뒤에_빈줄추가() {
        java.util.List<String> input = java.util.List.of("1.ㅇㄴㄹ", "1.osf", "2.ㄷㅈㄹ", "2. ewf");
        java.util.List<String> result = ManuscriptParser.insertBlankLineBetweenSections(input);
        assertEquals(java.util.List.of("1.ㅇㄴㄹ", "1.osf", "", "2.ㄷㅈㄹ", "2. ewf", ""), result);
    }

    @Order(13)
    @Test
    void validateSections_빈줄있으면통과() {
        java.util.List<String> input = java.util.List.of("1.ㅇㄴㄹ", "1.osf", "", "2.ㄷㅈㄹ", "2. ewf", "");
        ManuscriptParser.validateSections(input);
    }

    @Order(14)
    @Test
    void validateSections_빈줄없으면컷() {
        java.util.List<String> broken = java.util.List.of("1.ㅇㄴㄹ", "1.osf", "빈줄 아니지롱");
        assertThrows(IllegalArgumentException.class, () -> ManuscriptParser.validateSections(broken));
    }

// == 본문 함수 ==

    // == 제목구간 성경구절 ==
    @Order(15)
    @Test
    void extractMainBookFull_제목구구간성경구절서를완전체로바꾸기() {
        assertEquals("로마서", ManuscriptParser.extractMainBookFull("롬6:6-11"));
    }

    @Order(16)
    @Test
    void extractMainBookFull_완전체가이상하면컷() {
        assertThrows(IllegalArgumentException.class, () -> ManuscriptParser.extractMainBookFull("이상한것"));
    }

    // == 절만 있는 본문 완전체로 바꾸기 ==
    @Order(17)
    @Test
    void extractMainChapter_장뽑기() {
        assertEquals("6", ManuscriptParser.extractMainChapter("롬6:6-11"));
    }

    @Order(18)
    @Test
    void extractVerseOnlyLines_절만있는거순서대로나열() {
        java.util.List<String> lines = java.util.List.of("1.english", "6-7", "인용구절", "2.대지", "8-9", "2. 이러쿵", "10-11");
        java.util.List<String> result = ManuscriptParser.extractOnlyVerseLines(lines, 0);
        assertEquals(java.util.List.of("6-7", "8-9", "10-11"), result);
    }

    @Order(19)
    @Test
    void resolveOnlyVerseLine_서장절을합치기() {
        assertEquals("로마서 6:6-11",
                ManuscriptParser.resolveOnlyVerseLine("로마서", "6", "6-11"));
    }

    @Order(20)
    @Test
    void resolveOnlyVerseLine_숫자가빠지면면오류_근데이코드는별의미없는것같다() {
        assertThrows(IllegalArgumentException.class,
                () -> ManuscriptParser.resolveOnlyVerseLine("로마서", "142", "롬8:20"));
    }

    // == 나머지 본문 처리 ==
    @Order(21)
    @Test
    void extractSectionStartIndexes_한글제목줄_위치만_고른다() {
        java.util.List<String> lines = java.util.List.of(
                "제목", "1. 한글", "1. english", "본문", "2.한글", "2.english"
        );
        java.util.List<Integer> result = ManuscriptParser.extractSectionStartIndexes(lines);
        assertEquals(java.util.List.of(1, 4), result);
    }

    @Order(22)
    @Test
    void resolveCitationsInSection_축약인용을_풀어낸다() {
        java.util.List<String> lines = java.util.List.of("벧전2:22-24", "요19:30 고후5:21");
        java.util.List<String> result = ManuscriptParser.resolveCitationsInSection(lines, 0, 2);
        assertEquals(3, result.size());
        assertTrue(result.get(0).startsWith("베드로전서 2:22-24"));
    }

    @Order(23)
    @Test
    void resolveCitationsInSection_뒤에붙은_내용을_보존한다() {
        java.util.List<String> lines = java.util.List.of("약4:6b하나님이 교만한 자를~~");
        java.util.List<String> result = ManuscriptParser.resolveCitationsInSection(lines, 0, 1);
        assertEquals("야고보서 4:6b하나님이 교만한 자를~~", result.get(0));
    }

    @Order(24)
    @Test
    void parse_실제_설교_원고를_넣으면_전체_결과가_출력된다() throws Exception {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("real_manuscript", ".txt");
        java.nio.file.Files.writeString(tempFile, 원고, java.nio.charset.StandardCharsets.UTF_8);

        System.out.println("========== 전체 결과물 ==========");
        ManuscriptParser.parse(tempFile);

        java.nio.file.Files.deleteIfExists(tempFile);
    }

}