package view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmprestimosCsvTest {

    @Test
    void csvRowsQuoteFieldsAndEscapeQuotesAndSeparators() {
        assertEquals("\"12\";\"Livro; especial\";\"Autor \"\"A\"\"\"",
            EmprestimosView.formatarLinhaCsv(new Object[] {12, "Livro; especial", "Autor \"A\""}));
    }

    @Test
    void csvFieldsProtectSpreadsheetFormulasInTextValues() {
        assertEquals("\"'=HYPERLINK(\"\"https://example.com\"\")\"",
            EmprestimosView.escaparCampoCsv("=HYPERLINK(\"https://example.com\")"));
        assertEquals("\"'-123\"", EmprestimosView.escaparCampoCsv("-123"));
        assertEquals("\"123\"", EmprestimosView.escaparCampoCsv(123));
    }

    @Test
    void csvFieldsRepresentNullAsAnEmptyField() {
        assertEquals("\"\"", EmprestimosView.escaparCampoCsv(null));
    }
}
