package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LivroTest {

    @Test
    void constructorPreservesBookDetails() {
        Livro livro = new Livro(
            7, "Dom Casmurro", "Machado de Assis", "capa.jpg",
            1899, 2, 3, "9788535910663", "Português"
        );

        assertAll(
            () -> assertEquals(7, livro.getId()),
            () -> assertEquals("Dom Casmurro", livro.getTitle()),
            () -> assertEquals("Machado de Assis", livro.getAuthors()),
            () -> assertEquals("capa.jpg", livro.getCover()),
            () -> assertEquals(1899, livro.getPublishYear()),
            () -> assertEquals(2, livro.getGenreId()),
            () -> assertEquals(3, livro.getPublisherId()),
            () -> assertEquals("9788535910663", livro.getIsbn()),
            () -> assertEquals("Português", livro.getIdioma())
        );
    }

    @Test
    void availabilityDependsOnAvailableCopies() {
        Livro livro = new Livro();

        assertFalse(livro.isDisponibilidade());
        livro.setExemplaresEmprestados(2);
        assertFalse(livro.isDisponibilidade());

        livro.setExemplaresDisponiveis(1);
        assertTrue(livro.isDisponibilidade());
    }

    @Test
    void catalogMetadataCanBeUpdated() {
        Livro livro = new Livro();
        livro.setGeneroCatalogo("Fantasia");
        livro.setEditoraCatalogo("Editora Exemplo");

        assertEquals("Fantasia", livro.getGeneroCatalogo());
        assertEquals("Editora Exemplo", livro.getEditoraCatalogo());
    }

    @Test
    void stringRepresentationIncludesCoreBookInformation() {
        Livro livro = new Livro(7, "Dom Casmurro", "Machado de Assis", null, 1899, 2, 3);

        assertTrue(livro.toString().contains("id=7"));
        assertTrue(livro.toString().contains("title='Dom Casmurro'"));
        assertTrue(livro.toString().contains("isbn=''"));
    }
}
