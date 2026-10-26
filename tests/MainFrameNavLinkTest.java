package view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainFrameNavLinkTest {

    @Test
    void storesItsLabelAndNavigationKey() {
        MainFrame.NavLink link = new MainFrame.NavLink("Livros", "livros");

        assertEquals("Livros", link.getText());
        assertEquals("livros", link.getChave());
        assertFalse(link.ativo);
    }

    @Test
    void activatingLinkUpdatesItsStateAndStyle() {
        MainFrame.NavLink link = new MainFrame.NavLink("Busca", "busca");

        link.setAtivo(true);

        assertTrue(link.ativo);
        assertEquals(UITheme.ACCENT_TEXT, link.getForeground());
        assertEquals(UITheme.fontBodyBold(), link.getFont());
    }

    @Test
    void deactivatingLinkRestoresItsDefaultStyle() {
        MainFrame.NavLink link = new MainFrame.NavLink("Estantes", "estantes");
        link.setAtivo(true);

        link.setAtivo(false);

        assertFalse(link.ativo);
        assertEquals(UITheme.TEXT_SECONDARY, link.getForeground());
        assertEquals(UITheme.fontNavItem(), link.getFont());
    }
}
