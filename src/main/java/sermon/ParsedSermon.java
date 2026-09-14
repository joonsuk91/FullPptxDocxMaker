package sermon;

import java.util.List;

public record ParsedSermon(
        String koreanTitle, String englishTitle, List<String> sectionTitleLines, String referenceRaw, List<List<String>> blocks) {

}