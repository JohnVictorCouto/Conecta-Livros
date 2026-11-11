package util;

import model.Emprestimo;
import model.Livro;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WireJsonTest {

    @Test
    void roundTripsAWhitelistModelWithUnicodeAndDates() {
        Livro livro = new Livro(12, "Ação e ficção", "Ana", null, 2024, 3, 5, "9781234567890", "pt-BR");
        livro.setExemplaresDisponiveis(2);
        livro.setExemplaresEmprestados(1);

        Object parsed = WireJson.parse(WireJson.stringify(List.of(livro)));
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) parsed;
        Livro restored = WireJson.convert(values.get(0), Livro.class);

        assertEquals(livro.getTitle(), restored.getTitle());
        assertEquals(livro.getGenreId(), restored.getGenreId());
        assertEquals(2, restored.getExemplaresDisponiveis());
    }

    @Test
    void roundTripsImmutableLoanAndNullableReturnDate() {
        Emprestimo emprestimo = new Emprestimo(4, 9, "Livro", "EX-9", "Leitor", "contato",
            LocalDateTime.of(2026, 11, 1, 9, 30),
            LocalDateTime.of(2026, 11, 15, 17, 45), null);

        Object parsed = WireJson.parse(WireJson.stringify(emprestimo));
        Emprestimo restored = WireJson.convert(parsed, Emprestimo.class);

        assertEquals(emprestimo.getDataEmprestimo(), restored.getDataEmprestimo());
        assertEquals(emprestimo.getDataVencimento(), restored.getDataVencimento());
        assertEquals(null, restored.getDataDevolucao());
    }

    @Test
    void rejectsUnlistedJavaObjectsAndTrailingInput() {
        assertThrows(IllegalArgumentException.class, () -> WireJson.stringify(new Object()));
        assertThrows(IllegalArgumentException.class, () -> WireJson.parse("{\"ok\":true} {}"));
        assertThrows(IllegalArgumentException.class,
            () -> WireJson.convert(Map.of("value", "x"), java.io.File.class));
    }
}
