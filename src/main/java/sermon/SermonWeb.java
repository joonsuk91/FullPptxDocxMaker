package sermon;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.apache.poi.hslf.usermodel.HSLFPictureData;
import org.apache.poi.hslf.usermodel.HSLFPictureShape;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class SermonWeb {

    private static final String TEMPLATE_RESOURCE_PATH = "/template.pptx";
    private static final String PAGE_RESOURCE_PATH = "/web/index.html";

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String PPTX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[0-9a-f\\-]+\\.(docx|pptx)$");

    private static Path outputDirectory;

    private SermonWeb() {
    }

    public static void main(String[] args) throws Exception {
        outputDirectory = Files.createTempDirectory("sermon-output");

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        Javalin app = Javalin.create().start(port);

        app.get("/", SermonWeb::readIndex);
        app.post("/build", SermonWeb::build);
        app.get("/download/{name}", SermonWeb::download);
    }

    static void readIndex(Context context) throws Exception {
        try (InputStream in = SermonWeb.class.getResourceAsStream(PAGE_RESOURCE_PATH)) {
            if (in == null) {
                context.status(500).result("화면 파일을 찾지 못했습니다.");
                return;
            }
            context.contentType("text/html; charset=utf-8");
            context.result(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    static void build(Context context) {
        try {
            ParsedSermon sermon = parseManuscript(requireFormParam(context, "manuscript"));

            XMLSlideShow template = openTemplate();

            XWPFDocument document = DocxBuilder.build(sermon);

            XMLSlideShow slideShow = PptxBuilder.build(
                    template,
                    sermon.koreanTitle(),
                    sermon.englishTitle(),
                    requireFormParam(context, "doxologyChapter"),
                    requireFormParam(context, "responsiveReadingNumber"),
                    requireFormParam(context, "praiseChapter"),
                    requireFormParam(context, "confessionPrayReference"),
                    requireFormParam(context, "hymnChapter"),
                    requireFormParam(context, "prayerName"),
                    sermon,
                    requireFormParam(context, "closingHymnChapter"),
                    openHymn(context, "doxologyHymnPptx"),
                    openHymn(context, "praiseHymnPptx"),
                    openHymn(context, "hymnPptx"),
                    openHymn(context, "closingHymnPptx"),
                    context.formParam("churchNews"));

            String id = UUID.randomUUID().toString();
            int docxPages = countPages(document);
            int pptxSlides = slideShow.getSlides().size();

            saveDocx(document, id);
            savePptx(slideShow, id);

            Map<String, Object> answer = new HashMap<>();
            answer.put("docxUrl", "/download/" + id + ".docx");
            answer.put("pptxUrl", "/download/" + id + ".pptx");
            answer.put("docxPages", docxPages);
            answer.put("pptxSlides", pptxSlides);

            context.json(answer);
        } catch (IllegalArgumentException error) {
            error.printStackTrace();
            sendError(context, 400, error.getMessage());
        } catch (Exception error) {
            error.printStackTrace();
            sendError(context, 500, "문서를 만들지 못했습니다: " + error.getMessage());
        }
    }

    static void download(Context context) throws Exception {
        String name = context.pathParam("name");

        if (!SAFE_FILE_NAME.matcher(name).matches()) {
            context.status(400).result("잘못된 파일 이름입니다.");
            return;
        }

        Path file = outputDirectory.resolve(name);
        if (!Files.exists(file)) {
            context.status(404).result("파일을 찾지 못했습니다.");
            return;
        }

        context.contentType(name.endsWith(".docx") ? DOCX_CONTENT_TYPE : PPTX_CONTENT_TYPE);
        context.header("Content-Disposition", "attachment; filename=\"" + name + "\"");
        context.result(Files.readAllBytes(file));
    }

    static ParsedSermon parseManuscript(String manuscript) throws Exception {
        Path temporaryFile = Files.createTempFile("manuscript", ".txt");

        try {
            Files.writeString(temporaryFile, manuscript, StandardCharsets.UTF_8);
            return ManuscriptParser.parse(temporaryFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    static String requireFormParam(Context context, String name) {
        String value = context.formParam(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 값이 비어 있습니다.");
        }
        return value.trim();
    }

    static XMLSlideShow openHymn(Context context, String name) throws Exception {
        UploadedFile file = context.uploadedFile(name);
        if (file == null) {
            throw new IllegalArgumentException(name + " 파일이 없습니다.");
        }

        String fileName = file.filename();
        byte[] bytes;
        try (InputStream in = file.content()) {
            bytes = in.readAllBytes();
        }

        if (fileName != null && fileName.toLowerCase().endsWith(".ppt")) {
            return convertLegacyPpt(bytes);
        }
        return new XMLSlideShow(new ByteArrayInputStream(bytes));
    }

    static XMLSlideShow convertLegacyPpt(byte[] bytes) throws Exception {
        HSLFSlideShow legacy = new HSLFSlideShow(new ByteArrayInputStream(bytes));
        XMLSlideShow converted = new XMLSlideShow();
        converted.setPageSize(new java.awt.Dimension(
                legacy.getPageSize().width, legacy.getPageSize().height));

        for (HSLFSlide legacySlide : legacy.getSlides()) {
            XSLFSlide newSlide = converted.createSlide();

            for (HSLFShape shape : legacySlide.getShapes()) {
                if (shape instanceof HSLFPictureShape pictureShape) {
                    HSLFPictureData pictureData = pictureShape.getPictureData();

                    XSLFPictureData addedPicture = converted.addPicture(
                            pictureData.getData(), pictureData.getType());

                    XSLFPictureShape newPicture = newSlide.createPicture(addedPicture);
                    newPicture.setAnchor(pictureShape.getAnchor());
                }
            }
        }

        legacy.close();
        return converted;
    }

    static XMLSlideShow openTemplate() throws Exception {
        try (InputStream in = SermonWeb.class.getResourceAsStream(TEMPLATE_RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("template.pptx 를 찾지 못했습니다.");
            }
            return new XMLSlideShow(in);
        }
    }

    static void saveDocx(XWPFDocument document, String id) throws Exception {
        try (OutputStream out = Files.newOutputStream(outputDirectory.resolve(id + ".docx"))) {
            document.write(out);
        }
    }

    static void savePptx(XMLSlideShow slideShow, String id) throws Exception {
        fixSlideCountMetadata(slideShow);

        Path pptxFile = outputDirectory.resolve(id + ".pptx");
        try (OutputStream out = Files.newOutputStream(pptxFile)) {
            slideShow.write(out);
        }

        removeOrphanSlideParts(pptxFile);
    }

    static void removeOrphanSlideParts(Path pptxFile) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(pptxFile))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                entries.put(entry.getName(), zipIn.readAllBytes());
            }
        }

        String presentationXml = new String(entries.get("ppt/presentation.xml"), StandardCharsets.UTF_8);
        String presentationRels = new String(entries.get("ppt/_rels/presentation.xml.rels"), StandardCharsets.UTF_8);

        Map<String, String> relationshipIdToSlideFile = new HashMap<>();
        Matcher relationshipMatcher = Pattern.compile("<Relationship [^>]*/>").matcher(presentationRels);
        while (relationshipMatcher.find()) {
            String relationshipTag = relationshipMatcher.group();
            Matcher idMatcher = Pattern.compile("Id=\"(rId\\d+)\"").matcher(relationshipTag);
            Matcher targetMatcher = Pattern.compile("Target=\"slides/(slide\\d+\\.xml)\"").matcher(relationshipTag);
            if (idMatcher.find() && targetMatcher.find()) {
                relationshipIdToSlideFile.put(idMatcher.group(1), targetMatcher.group(1));
            }
        }

        int slideListStart = presentationXml.indexOf("sldIdLst");
        int slideListEnd = presentationXml.indexOf("</p:sldIdLst>");
        String slideListSection = presentationXml.substring(slideListStart, slideListEnd);

        Set<String> registeredSlideFiles = new HashSet<>();
        Matcher slideIdMatcher = Pattern.compile("r:id=\"(rId\\d+)\"").matcher(slideListSection);
        while (slideIdMatcher.find()) {
            String slideFile = relationshipIdToSlideFile.get(slideIdMatcher.group(1));
            if (slideFile != null) {
                registeredSlideFiles.add(slideFile);
            }
        }

        Set<String> allSlideFiles = new HashSet<>();
        for (String name : entries.keySet()) {
            Matcher fileMatcher = Pattern.compile("^ppt/slides/(slide\\d+\\.xml)$").matcher(name);
            if (fileMatcher.find()) {
                allSlideFiles.add(fileMatcher.group(1));
            }
        }

        Set<String> orphanSlideFiles = new HashSet<>(allSlideFiles);
        orphanSlideFiles.removeAll(registeredSlideFiles);

        if (orphanSlideFiles.isEmpty()) {
            return;
        }

        for (String orphan : orphanSlideFiles) {
            entries.remove("ppt/slides/" + orphan);
            entries.remove("ppt/slides/_rels/" + orphan + ".rels");
        }

        Set<String> orphanRelationshipIds = new HashSet<>();
        for (Map.Entry<String, String> mapping : relationshipIdToSlideFile.entrySet()) {
            if (orphanSlideFiles.contains(mapping.getValue())) {
                orphanRelationshipIds.add(mapping.getKey());
            }
        }

        String cleanedPresentationRels = presentationRels;
        for (String relationshipId : orphanRelationshipIds) {
            cleanedPresentationRels = cleanedPresentationRels.replaceAll(
                    "<Relationship Id=\"" + relationshipId + "\"[^>]*/>", "");
        }
        entries.put("ppt/_rels/presentation.xml.rels", cleanedPresentationRels.getBytes(StandardCharsets.UTF_8));

        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(pptxFile))) {
            for (Map.Entry<String, byte[]> mapEntry : entries.entrySet()) {
                zipOut.putNextEntry(new ZipEntry(mapEntry.getKey()));
                zipOut.write(mapEntry.getValue());
                zipOut.closeEntry();
            }
        }
    }

    static void fixSlideCountMetadata(XMLSlideShow slideShow) {
        var extendedProperties = slideShow.getProperties().getExtendedProperties().getUnderlyingProperties();
        extendedProperties.setSlides(slideShow.getSlides().size());
    }

    static int countPages(XWPFDocument document) {
        int pageBreaks = 0;

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                for (CTBr lineBreak : run.getCTR().getBrList()) {
                    if (lineBreak.getType() == STBrType.PAGE) {
                        pageBreaks++;
                    }
                }
            }
        }

        return pageBreaks + 1;
    }

    static void sendError(Context context, int status, String message) {
        Map<String, Object> answer = new HashMap<>();
        answer.put("message", message);
        context.status(status).json(answer);
    }
}