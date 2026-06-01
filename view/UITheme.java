package view;

import java.awt.*;

// Classe central com cores, fontes e utilitarios visuais da interface
public class UITheme {

    // Cores principais do tema
    public static final Color BG_APP        = new Color(248, 249, 252);
    public static final Color BG_WHITE      = new Color(255, 255, 255);
    public static final Color BG_CARD       = new Color(255, 255, 255);

    public static final Color NAV_BG        = new Color(255, 255, 255);
    public static final Color NAV_BORDER    = new Color(229, 231, 235);

    public static final Color ACCENT        = new Color(37, 99, 235);
    public static final Color ACCENT_HOVER  = new Color(29, 78, 216);
    public static final Color ACCENT_LIGHT  = new Color(239, 246, 255);
    public static final Color ACCENT_TEXT   = new Color(37, 99, 235);

    // Cores de status
    public static final Color SUCCESS       = new Color(22, 163, 74);
    public static final Color SUCCESS_BG    = new Color(220, 252, 231);
    public static final Color DANGER        = new Color(220, 38, 38);
    public static final Color DANGER_BG     = new Color(254, 226, 226);
    public static final Color WARNING       = new Color(217, 119, 6);
    public static final Color WARNING_BG    = new Color(254, 243, 199);

    // Cores dos badges de genero
    public static final Color BADGE_FANTASIA_BG   = new Color(237, 233, 254);
    public static final Color BADGE_FANTASIA_FG   = new Color(109, 40, 217);
    public static final Color BADGE_ROMANCE_BG    = new Color(252, 231, 243);
    public static final Color BADGE_ROMANCE_FG    = new Color(157, 23, 77);
    public static final Color BADGE_FICCAO_BG     = new Color(220, 252, 231);
    public static final Color BADGE_FICCAO_FG     = new Color(21, 128, 61);
    public static final Color BADGE_DEFAULT_BG    = new Color(243, 244, 246);
    public static final Color BADGE_DEFAULT_FG    = new Color(55, 65, 81);

    // Cartoes de estatisticas
    public static final Color STAT_BLUE_BG  = new Color(239, 246, 255);
    public static final Color STAT_GREEN_BG = new Color(220, 252, 231);
    public static final Color STAT_RED_BG   = new Color(254, 226, 226);
    public static final Color STAT_PURPLE_BG= new Color(237, 233, 254);
    public static final Color STAT_BLUE_FG  = new Color(37, 99, 235);
    public static final Color STAT_GREEN_FG = new Color(22, 163, 74);
    public static final Color STAT_RED_FG   = new Color(220, 38, 38);
    public static final Color STAT_PURPLE_FG= new Color(109, 40, 217);

    // Cores de texto
    public static final Color TEXT_PRIMARY   = new Color(17, 24, 39);
    public static final Color TEXT_SECONDARY = new Color(75, 85, 99);
    public static final Color TEXT_MUTED     = new Color(156, 163, 175);
    public static final Color TEXT_WHITE     = Color.WHITE;

    // Cores de bordas
    public static final Color BORDER        = new Color(229, 231, 235);
    public static final Color BORDER_FOCUS  = new Color(37, 99, 235);
    public static final Color DIVIDER       = new Color(243, 244, 246);

    // Fontes usadas pela interface
    public static Font fontAppTitle()    { return new Font("Segoe UI", Font.BOLD,   20); }
    public static Font fontPageTitle()   { return new Font("Segoe UI", Font.BOLD,   24); }
    public static Font fontSubtitle()    { return new Font("Segoe UI", Font.PLAIN,  13); }
    public static Font fontBody()        { return new Font("Segoe UI", Font.PLAIN,  13); }
    public static Font fontBodyBold()    { return new Font("Segoe UI", Font.BOLD,   13); }
    public static Font fontSmall()       { return new Font("Segoe UI", Font.PLAIN,  11); }
    public static Font fontSmallBold()   { return new Font("Segoe UI", Font.BOLD,   11); }
    public static Font fontButton()      { return new Font("Segoe UI", Font.BOLD,   13); }
    public static Font fontNavItem()     { return new Font("Segoe UI", Font.PLAIN,  13); }
    public static Font fontStat()        { return new Font("Segoe UI", Font.BOLD,   28); }
    public static Font fontStatLabel()   { return new Font("Segoe UI", Font.PLAIN,  12); }
    public static Font fontBookTitle()   { return new Font("Segoe UI", Font.BOLD,   14); }
    public static Font fontBookAuthor()  { return new Font("Segoe UI", Font.PLAIN,  12); }
    public static Font fontBadge()       { return new Font("Segoe UI", Font.BOLD,   11); }
    public static Font fontTableHeader() { return new Font("Segoe UI", Font.BOLD,   12); }
    public static Font fontTableCell()   { return new Font("Segoe UI", Font.PLAIN,  13); }

    // Utilitarios de renderizacao
    public static void enableAA(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static void fillRounded(Graphics2D g2, Color c, int x, int y, int w, int h, int r) {
        g2.setColor(c);
        g2.fillRoundRect(x, y, w, h, r, r);
    }

    public static void drawRounded(Graphics2D g2, Color c, float stroke, int x, int y, int w, int h, int r) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(stroke));
        g2.drawRoundRect(x, y, w - 1, h - 1, r, r);
    }

    public static Color badgeBg(String genero) {
        if (genero == null) return BADGE_DEFAULT_BG;
        String g = genero.toLowerCase();
        if (g.contains("fanta"))  return BADGE_FANTASIA_BG;
        if (g.contains("roman"))  return BADGE_ROMANCE_BG;
        if (g.contains("fic"))    return BADGE_FICCAO_BG;
        return BADGE_DEFAULT_BG;
    }

    public static Color badgeFg(String genero) {
        if (genero == null) return BADGE_DEFAULT_FG;
        String g = genero.toLowerCase();
        if (g.contains("fanta"))  return BADGE_FANTASIA_FG;
        if (g.contains("roman"))  return BADGE_ROMANCE_FG;
        if (g.contains("fic"))    return BADGE_FICCAO_FG;
        return BADGE_DEFAULT_FG;
    }
}



