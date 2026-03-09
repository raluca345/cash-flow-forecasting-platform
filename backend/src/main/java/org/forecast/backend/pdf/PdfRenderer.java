package org.forecast.backend.pdf;

/**
 * Generic contract for rendering domain objects to HTML and PDF bytes.
 *
 * <p>Intended usage:
 * <ul>
 *   <li>Render a Thymeleaf template to HTML</li>
 *   <li>Convert HTML to PDF bytes with an HTML-to-PDF renderer (e.g., OpenHTMLtoPDF)</li>
 * </ul>
 *
 * @param <T> domain model type to render
 */
public interface PdfRenderer<T> {

    /**
     * Render the given model to HTML.
     */
    String renderHtml(T model);

    /**
     * Render the given model to PDF bytes.
     *
     * @param baseUri base URI used to resolve relative URLs in HTML (e.g. "http://localhost:8080" or "file:/...")
     */
    byte[] renderPdfBytes(T model, String baseUri);
}

