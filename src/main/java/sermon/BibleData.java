package sermon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class BibleData {

    private BibleData() {}

    // parse
    public static String lookupVerse(String resourcePath, String bookFull, String chapter, String verse) throws java.io.IOException {
        JsonNode root = loadJson(resourcePath);
        JsonNode bookNode = findBook(root, bookFull);
        JsonNode chapterNode = findChapter(bookNode, chapter);
        return findVerseText(chapterNode, verse);
    }

    // 세부코드
    static JsonNode loadJson(String resourcePath) throws java.io.IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (java.io.InputStream in = BibleData.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new java.io.IOException("파일이 존재하지 않습니다: " + resourcePath);
            }
            return mapper.readTree(in);
        }
    }

    static JsonNode findBook(JsonNode root, String bookFull) {
        JsonNode bookNode = root.get(bookFull);
        if (bookNode == null) {
            throw new IllegalArgumentException("서를 찾지 못했습니다: " + bookFull);
        }
        return bookNode;
    }

    static JsonNode findChapter(JsonNode bookNode, String chapter) {
        JsonNode chapterNode = bookNode.get(chapter);
        if (chapterNode == null) {
            throw new IllegalArgumentException("장을 찾지 못했습니다: " + chapter);
        }
        return chapterNode;
    }

    static String findVerseText(JsonNode chapterNode, String verse) {
        JsonNode verseNode = chapterNode.get(verse);
        if (verseNode == null) {
            throw new IllegalArgumentException("절을 찾지 못했습니다: " + verse);
        }
        return verseNode.asText();
    }
}