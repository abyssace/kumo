package dev.abyssace.kumo.loader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Patrón: Test Fixture.
 *
 * <p>Genera PDF de prueba en el propio test con PDFBox. Nada de binarios
 * versionados: cada prueba crea exactamente el documento que necesita.</p>
 */
final class PdfFixtures {

    private PdfFixtures() {
    }

    /**
     * Crea un PDF con una página por cada texto. Un {@code null} produce una
     * página en blanco, útil para comprobar que no se renumeran las siguientes.
     */
    static Path create(Path directory, String name, List<String> pages) throws IOException {
        Path file = directory.resolve(name);

        try (PDDocument pdf = new PDDocument()) {
            for (String text : pages) {
                PDPage page = new PDPage(PDRectangle.A4);
                pdf.addPage(page);
                if (text != null) {
                    try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                        content.beginText();
                        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        content.newLineAtOffset(50, 700);
                        content.showText(text);
                        content.endText();
                    }
                }
            }
            pdf.save(file.toFile());
        }

        return file;
    }
}
