package sermon;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BookNamesEnglish {

    private static final Map<String, String> FULL_TO_ENGLISH = new LinkedHashMap<>() {{
        // ===== 구약 39권 =====
        put("창세기", "Genesis");        put("출애굽기", "Exodus");
        put("레위기", "Leviticus");      put("민수기", "Numbers");
        put("신명기", "Deuteronomy");    put("여호수아", "Joshua");
        put("사사기", "Judges");         put("룻기", "Ruth");
        put("사무엘상", "FirstSamuel");  put("사무엘하", "SecondSamuel");
        put("열왕기상", "FirstKings");   put("열왕기하", "SecondKings");
        put("역대상", "FirstChronicles"); put("역대하", "SecondChronicles");
        put("에스라", "Ezra");           put("느헤미야", "Nehemiah");
        put("에스더", "Esther");         put("욥기", "Job");
        put("시편", "Psalms");           put("잠언", "Proverbs");
        put("전도서", "Ecclesiastes");   put("아가", "SongOfSolomon");
        put("이사야", "Isaiah");         put("예레미야", "Jeremiah");
        put("예레미야애가", "Lamentations"); put("에스겔", "Ezekiel");
        put("다니엘", "Daniel");         put("호세아", "Hosea");
        put("요엘", "Joel");             put("아모스", "Amos");
        put("오바댜", "Obadiah");        put("요나", "Jonah");
        put("미가", "Micah");            put("나훔", "Nahum");
        put("하박국", "Habakkuk");       put("스바냐", "Zephaniah");
        put("학개", "Haggai");           put("스가랴", "Zechariah");
        put("말라기", "Malachi");
        // ===== 신약 27권 =====
        put("마태복음", "Matthew");      put("마가복음", "Mark");
        put("누가복음", "Luke");         put("요한복음", "John");
        put("사도행전", "Acts");         put("로마서", "Romans");
        put("고린도전서", "FirstCorinthians"); put("고린도후서", "SecondCorinthians");
        put("갈라디아서", "Galatians");  put("에베소서", "Ephesians");
        put("빌립보서", "Philippians");  put("골로새서", "Colossians");
        put("데살로니가전서", "FirstThessalonians");
        put("데살로니가후서", "SecondThessalonians");
        put("디모데전서", "FirstTimothy"); put("디모데후서", "SecondTimothy");
        put("디도서", "Titus");          put("빌레몬서", "Philemon");
        put("히브리서", "Hebrews");      put("야고보서", "James");
        put("베드로전서", "FirstPeter"); put("베드로후서", "SecondPeter");
        put("요한일서", "FirstJohn");    put("요한이서", "SecondJohn");
        put("요한삼서", "ThirdJohn");    put("유다서", "Jude");
        put("요한계시록", "Revelation");
    }};

    private BookNamesEnglish() {}

    public static String toEnglish(String koreanFullName) {

        if (koreanFullName == null) return null;
        String result = FULL_TO_ENGLISH.get(koreanFullName.trim());
        return result != null ? result : koreanFullName;

    }
}
