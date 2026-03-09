package org.forecast.backend.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import lombok.RequiredArgsConstructor;
import org.forecast.backend.model.Invoice;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class InvoicePdfRenderer implements PdfRenderer<Invoice> {
    private final SpringTemplateEngine engine;

    @Override
    public String renderHtml(Invoice invoice) {
        Context ctx = new Context();
        ctx.setVariable("company", invoice.getCompany());
        ctx.setVariable("invoice", invoice);
        ctx.setVariable("items", invoice.getItems());
        ctx.setVariable("client", invoice.getClient());

        return engine.process("invoice", ctx);
    }

    /**
     * Renders an invoice to PDF bytes using OpenHTMLtoPDF.
     *
     * @param invoice      fully-initialized invoice (company/client/items fetched)
     * @param baseUri      base URI used to resolve relative URLs in HTML (e.g., "http://localhost:8080" or "file:/...")
     */
    @Override
    public byte[] renderPdfBytes(Invoice invoice, String baseUri) {
        String html = renderHtml(invoice);

        // Parse HTML5 with Jsoup and convert to a W3C Document
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
        Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.withW3cDocument(w3cDoc, baseUri);
            builder.toStream(out);
            builder.useFastMode();
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render invoice PDF", e);
        }
    }
}
