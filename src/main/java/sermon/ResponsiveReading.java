package sermon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class ResponsiveReading {

    private static final String RESOURCE_PATH = "/data/responsive_reading_1_137.json";

    private ResponsiveReading() {}

    public static final class Line {
        public final String role;
        public final String korean;
        public final String english;

        public Line(String role, String korean, String english) {
            this.role = role;
            this.korean = korean;
            this.english = english;
        }
    }

    public static List<Line> lookup(String number) throws java.io.IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try (java.io.InputStream in = ResponsiveReading.class.getResourceAsStream(RESOURCE_PATH)) {
            root = mapper.readTree(in);
        }

        JsonNode entry = root.get(number);
        if (entry == null) {
            throw new IllegalArgumentException("색인에 없는 성시교독 번호입니다: " + number);
        }

        List<Line> lines = new ArrayList<>();
        for (JsonNode lineNode : entry) {
            lines.add(new Line(
                    lineNode.get("role").asText(),
                    lineNode.get("ko").asText(),
                    lineNode.get("en").asText()
            ));
        }
        return lines;
    }
}
