package dev.abyssace.kumo.loader;

import dev.abyssace.kumo.domain.DocumentFormat;
import dev.abyssace.kumo.domain.DocumentPage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Patrón: Template Method concreta + Adapter sobre PDFBox.
 *
 * <p>Extrae el texto página a página y conserva el número real de página para
 * que las citas digan "página N". Las páginas en blanco se omiten, pero no se
 * renumeran las siguientes. Un PDF escaneado (solo imágenes) no tiene texto
 * extraíble y se rechaza con un mensaje claro: OCR queda fuera del alcance.</p>
 */
public final class PdfDocumentLoader extends AbstractDocumentLoader {

    @Override
    protected Set<String> extensions() {
        return Set.of("pdf");
    }

    @Override
    protected DocumentFormat format() {
        return DocumentFormat.PDF;
    }

    @Override
    protected List<DocumentPage> readPages(Path path) throws IOException {
        List<DocumentPage> pages = new ArrayList<>();

        try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = pdf.getNumberOfPages();

            for (int number = 1; number <= totalPages; number++) {
                stripper.setStartPage(number);
                stripper.setEndPage(number);
                String text = stripper.getText(pdf);
                if (!text.isBlank()) {
                    pages.add(new DocumentPage(number, text.strip()));
                }
            }
        }

        if (pages.isEmpty()) {
            throw new DocumentLoadException(
                    "El PDF no contiene texto extraíble (¿es un escaneo?): " + path);
        }
        return pages;
    }
}
