package sermon;

import java.util.LinkedHashMap;
import java.util.Map;

public class BookNames {

    private static final Map<String, String> ABBR_TO_FULL = new LinkedHashMap<>();

    static {

        put("창", "창세기");
        put("출", "출애굽기");
        put("레", "레위기");
        put("민", "민수기");
        put("신", "신명기");
        put("수", "여호수아");
        put("삿", "사사기");
        put("룻", "룻기");
        put("삼상", "사무엘상");
        put("삼하", "사무엘하");
        put("왕상", "열왕기상");
        put("왕하", "열왕기하");
        put("대상", "역대상");
        put("대하", "역대하");
        put("스", "에스라");
        put("느", "느헤미야");
        put("에", "에스더");
        put("욥", "욥기");
        put("시", "시편");
        put("잠", "잠언");
        put("전", "전도서");
        put("아", "아가");
        put("사", "이사야");
        put("렘", "예레미야");
        put("애", "예레미야애가");
        put("겔", "에스겔");
        put("단", "다니엘");
        put("호", "호세아");
        put("욜", "요엘");
        put("암", "아모스");
        put("옵", "오바댜");
        put("욘", "요나");
        put("미", "미가");
        put("나", "나훔");
        put("합", "하박국");
        put("습", "스바냐");
        put("학", "학개");
        put("슥", "스가랴");
        put("말", "말라기");

        // 신약 27권
        put("마", "마태복음");
        put("막", "마가복음");
        put("눅", "누가복음");
        put("요", "요한복음");
        put("행", "사도행전");
        put("롬", "로마서");
        put("고전", "고린도전서");
        put("고후", "고린도후서");
        put("갈", "갈라디아서");
        put("엡", "에베소서");
        put("빌", "빌립보서");
        put("골", "골로새서");
        put("살전", "데살로니가전서");
        put("살후", "데살로니가후서");
        put("딤전", "디모데전서");
        put("딤후", "디모데후서");
        put("딛", "디도서");
        put("몬", "빌레몬서");
        put("히", "히브리서");
        put("약", "야고보서");
        put("벧전", "베드로전서");
        put("벧후", "베드로후서");
        put("요일", "요한일서");
        put("요이", "요한이서");
        put("요삼", "요한삼서");
        put("유", "유다서");
        put("계", "요한계시록");

    }

    private BookNames() {
    }

    private static void put(String abbr, String full) {
        ABBR_TO_FULL.put(abbr, full);
    }

    public static String toFullName(String name) {
        if (name==null) {
            return null;
        }

        String trimmed = name.trim();

        if (ABBR_TO_FULL.containsKey(trimmed)) {
            return ABBR_TO_FULL.get(trimmed);
        }

        if (ABBR_TO_FULL.containsValue(trimmed)) {
            return trimmed;
        }

        return trimmed;
    }

    public static boolean isKnownAbbreviation(String name) {
        return name != null && ABBR_TO_FULL.containsKey(name.trim());
    }
}
