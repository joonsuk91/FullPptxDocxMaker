package sermon;

import org.apache.poi.sl.usermodel.PaintStyle;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFBackground;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFRelation;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextFont;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackground;
import org.openxmlformats.schemas.presentationml.x2006.main.CTBackgroundProperties;

import java.awt.geom.Rectangle2D;
import java.util.List;

final class PptxCitationParser {

    static org.apache.poi.xslf.usermodel.XSLFSlideLayout findMatchingLayout(XMLSlideShow output, XSLFSlide sourceSlide) {
        String targetName;
        try {
            targetName = sourceSlide.getSlideLayout().getPackagePart().getPartName().getName();
        } catch (Exception e) {
            return null;
        }

        for (org.apache.poi.xslf.usermodel.XSLFSlideMaster master : output.getSlideMasters()) {
            for (org.apache.poi.xslf.usermodel.XSLFSlideLayout layout : master.getSlideLayouts()) {
                if (layout.getPackagePart().getPartName().getName().equals(targetName)) {
                    return layout;
                }
            }
        }
        return null;
    }


    private static final String BIBLE_RESOURCE_PATH_KOREAN = "/data/bible_ko.json";
    private static final String BIBLE_RESOURCE_PATH_ENGLISH = "/data/bible_en.json";

    private PptxCitationParser() {}

    static XSLFSlide copySlideFromTemplate(XMLSlideShow output, XMLSlideShow template, int slideIndex) {
        XSLFSlide sourceSlide = template.getSlides().get(slideIndex);
        return copySlideByValue(output, sourceSlide, true);
    }

    static XSLFSlide copySlideByValue(XMLSlideShow output, XSLFSlide sourceSlide) {
        return copySlideByValue(output, sourceSlide, true);
    }

    static XSLFSlide copySlideByValue(XMLSlideShow output, XSLFSlide sourceSlide, boolean matchLayout) {
        XSLFSlide newSlide = matchLayout
                ? output.createSlide(findMatchingLayout(output, sourceSlide))
                : output.createSlide();

        for (XSLFTextShape placeholder : newSlide.getPlaceholders()) {
            newSlide.removeShape(placeholder);
        }

        copyBackgroundByValue(output, newSlide, sourceSlide);

        for (XSLFShape shape : sourceSlide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                copyTextShapeByValue(newSlide, textShape);
            } else if (shape instanceof XSLFPictureShape pictureShape) {
                copyPictureShapeByValue(output, newSlide, pictureShape);
            }
        }

        fixPunctuationSpacing(newSlide);
        return newSlide;
    }

    static void copyBackgroundByValue(XMLSlideShow output, XSLFSlide targetSlide, XSLFSlide sourceSlide) {
        try {
            XSLFBackground background = sourceSlide.getBackground();
            if (background == null) {
                return;
            }

            PaintStyle paint = background.getFillStyle().getPaint();
            if (!(paint instanceof PaintStyle.TexturePaint texturePaint)) {
                return;
            }

            byte[] imageBytes = texturePaint.getImageData().readAllBytes();
            String contentType = texturePaint.getContentType();
            PictureData.PictureType pictureType = (contentType != null && contentType.contains("png"))
                    ? PictureData.PictureType.PNG
                    : PictureData.PictureType.JPEG;

            XSLFPictureData addedPicture = output.addPicture(imageBytes, pictureType);
            XSLFRelation relationType = pictureType == PictureData.PictureType.PNG
                    ? XSLFRelation.IMAGE_PNG
                    : XSLFRelation.IMAGE_JPEG;
            String relationId = targetSlide.addRelation(null, relationType, addedPicture).getRelationship().getId();

            CTBackground ctBackground = targetSlide.getXmlObject().getCSld().addNewBg();
            CTBackgroundProperties backgroundProperties = ctBackground.addNewBgPr();
            CTBlipFillProperties blipFill = backgroundProperties.addNewBlipFill();
            CTBlip blip = blipFill.addNewBlip();
            blip.setEmbed(relationId);
            blipFill.addNewStretch().addNewFillRect();
            backgroundProperties.addNewEffectLst();
        } catch (Exception e) {
            throw new RuntimeException("배경 이미지를 복사하지 못했습니다.", e);
        }
    }

    static void copyTextShapeByValue(XSLFSlide targetSlide, XSLFTextShape sourceShape) {
        XSLFTextBox newTextBox = targetSlide.createTextBox();
        newTextBox.setAnchor(sourceShape.getAnchor());
        newTextBox.setTextAutofit(sourceShape.getTextAutofit());
        newTextBox.setWordWrap(sourceShape.getWordWrap());

        XSLFTextParagraph firstExistingParagraph = newTextBox.getTextParagraphs().get(0);
        for (int i = firstExistingParagraph.getTextRuns().size() - 1; i >= 0; i--) {
            firstExistingParagraph.removeTextRun(firstExistingParagraph.getTextRuns().get(i));
        }

        boolean firstParagraph = true;
        for (XSLFTextParagraph sourceParagraph : sourceShape.getTextParagraphs()) {
            XSLFTextParagraph newParagraph = firstParagraph
                    ? firstExistingParagraph
                    : newTextBox.addNewTextParagraph();
            firstParagraph = false;

            if (sourceParagraph.getTextAlign() != null) {
                newParagraph.setTextAlign(sourceParagraph.getTextAlign());
            }
            if (sourceParagraph.getLineSpacing() != null) {
                newParagraph.setLineSpacing(sourceParagraph.getLineSpacing());
            }

            List<XSLFTextRun> sourceRuns = sourceParagraph.getTextRuns();

            if (sourceRuns.isEmpty()) {
                newParagraph.addNewTextRun().setText("");
                continue;
            }

            for (XSLFTextRun sourceRun : sourceRuns) {
                if (sourceRun.getXmlObject() instanceof org.openxmlformats.schemas.drawingml.x2006.main.CTTextLineBreak) {
                    newParagraph.addLineBreak();
                    continue;
                }

                XSLFTextRun newRun = newParagraph.addNewTextRun();
                String text = sourceRun.getRawText();
                newRun.setText(text == null ? "" : text);

                if (sourceRun.getFontSize() != null) {
                    newRun.setFontSize(sourceRun.getFontSize());
                }
                newRun.setBold(sourceRun.isBold());
                newRun.setItalic(sourceRun.isItalic());
                newRun.setUnderlined(sourceRun.isUnderlined());

                if (sourceRun.getFontColor() instanceof PaintStyle.SolidPaint solidPaint) {
                    newRun.setFontColor(solidPaint.getSolidColor().getColor());
                }

                copyFontTypefaces(sourceRun, newRun);
            }
        }
    }

    private static final String SAFE_KOREAN_FONT = "Apple SD Gothic Neo";

    static void copyFontTypefaces(XSLFTextRun sourceRun, XSLFTextRun targetRun) {
        CTTextCharacterProperties sourceProps = sourceRun.getRPr(false);
        if (sourceProps == null) {
            return;
        }

        CTTextCharacterProperties targetProps = targetRun.getRPr(true);

        if (sourceProps.isSetLatin()) {
            CTTextFont targetFont = targetProps.isSetLatin() ? targetProps.getLatin() : targetProps.addNewLatin();
            targetFont.setTypeface(SAFE_KOREAN_FONT);
        }

        if (sourceProps.isSetEa()) {
            CTTextFont targetFont = targetProps.isSetEa() ? targetProps.getEa() : targetProps.addNewEa();
            targetFont.setTypeface(SAFE_KOREAN_FONT);
        }
    }

    static void copyPictureShapeByValue(XMLSlideShow output, XSLFSlide targetSlide, XSLFPictureShape sourceShape) {
        try {
            XSLFPictureData sourceData = sourceShape.getPictureData();
            XSLFPictureData addedPicture = output.addPicture(sourceData.getData(), sourceData.getType());
            XSLFPictureShape newPicture = targetSlide.createPicture(addedPicture);
            newPicture.setAnchor(sourceShape.getAnchor());
        } catch (Exception e) {
            throw new RuntimeException("그림을 복사하지 못했습니다.", e);
        }
    }

    static void fixPunctuationSpacing(XSLFSlide slide) {
        for (XSLFShape shape : slide.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }

            for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                List<XSLFTextRun> runs = paragraph.getTextRuns();
                if (runs.isEmpty()) {
                    continue;
                }

                boolean containsLineBreak = false;
                for (XSLFTextRun run : runs) {
                    if (run.getXmlObject() instanceof org.openxmlformats.schemas.drawingml.x2006.main.CTTextLineBreak) {
                        containsLineBreak = true;
                        break;
                    }
                }
                if (containsLineBreak) {
                    continue;
                }

                StringBuilder merged = new StringBuilder();
                for (XSLFTextRun run : runs) {
                    String text = run.getRawText();
                    if (text != null) {
                        merged.append(text);
                    }
                }

                String fixed = merged.toString()
                        .replaceAll("\\s+([,.)])", "$1")
                        .replaceAll("\\(\\s+", "(");

                runs.get(0).setText(fixed);
                for (int i = runs.size() - 1; i >= 1; i--) {
                    paragraph.removeTextRun(runs.get(i));
                }
            }
        }
    }

    static void insertText(XSLFSlide slide, int shapeIndex, int paragraphIndex, String text) {
        XSLFTextShape textShape = (XSLFTextShape) slide.getShapes().get(shapeIndex);
        XSLFTextParagraph paragraph = textShape.getTextParagraphs().get(paragraphIndex);
        List<XSLFTextRun> runs = paragraph.getTextRuns();

        runs.get(0).setText(text);

        for (int i = runs.size() - 1; i >= 1; i--) {
            paragraph.removeTextRun(runs.get(i));
        }
    }

    static void insertExtraPptx(XMLSlideShow output, XMLSlideShow hymnPptx) {
        for (XSLFSlide sourceSlide : hymnPptx.getSlides()) {
            copySlideByValue(output, sourceSlide);
        }
    }

    static String[] lookupVersePair(String bookKorean, String chapter, String verseNumber) throws Exception {
        String verseKorean = BibleData.lookupVerse(BIBLE_RESOURCE_PATH_KOREAN, bookKorean, chapter, verseNumber);
        String verseEnglish = BibleData.lookupVerse(BIBLE_RESOURCE_PATH_ENGLISH, bookKorean, chapter, verseNumber);
        return new String[]{verseKorean, verseEnglish};
    }
}