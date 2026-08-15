package sermon;

import java.util.List;
import java.util.SplittableRandom;

public record ParsedSermon(
        String koreanTitle, String englishTitle, List<String> sectionTitleLines, String referenceRaw, List<List<String>> blocks) {

}
