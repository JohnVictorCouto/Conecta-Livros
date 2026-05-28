package view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Componente para exibir capas de livros com carregamento assíncrono.
 * A imagem original é armazenada no cache e redimensionada em paintComponent
 * para sempre preencher exatamente o tamanho do componente sem distorção.
 */
public class ImagemCapa extends JPanel {

    private static final ConcurrentHashMap<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final int TIMEOUT_MS = 5000;

    private final String urlCapa;
    private BufferedImage imagemOriginal;
    private boolean carregando = false;
    private int raioCantos = 8;
    private final Color corFallback;
    private final String inicial;

    public ImagemCapa(String urlCapa, int id, String titulo) {
        this.urlCapa = urlCapa;
        setOpaque(false);

        // Cor de fallback baseada no ID
        Color[] cores = {
            new Color(219, 234, 254), new Color(220, 252, 231),
            new Color(254, 226, 226), new Color(237, 233, 254),
            new Color(254, 243, 199)
        };
        this.corFallback = cores[Math.abs(id % cores.length)];

        // Inicial do título para o placeholder
        this.inicial = (titulo != null && !titulo.isEmpty())
            ? String.valueOf(titulo.charAt(0)).toUpperCase()
            : "?";

        if (urlCapa != null && !urlCapa.trim().isEmpty()) {
            carregarImagemAsync();
        }
    }

    private void carregarImagemAsync() {
        // Verifica cache antes de criar o worker
        if (CACHE.containsKey(urlCapa)) {
            imagemOriginal = CACHE.get(urlCapa);
            return;
        }

        carregando = true;
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                try {
                    if (CACHE.containsKey(urlCapa)) {
                        return CACHE.get(urlCapa);
                    }
                    URL url = URI.create(urlCapa).toURL();
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(TIMEOUT_MS);
                    connection.setReadTimeout(TIMEOUT_MS);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    BufferedImage img = javax.imageio.ImageIO.read(connection.getInputStream());
                    if (img != null) {
                        CACHE.put(urlCapa, img);
                    }
                    return img;
                } catch (Exception e) {
                    System.err.println("Erro ao carregar capa: " + e.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    imagemOriginal = get();
                } catch (Exception ignored) {}
                carregando = false;
                repaint();
            }
        }.execute();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.enableAA(g2);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) { g2.dispose(); return; }

        if (imagemOriginal != null) {
            // Ajuste "cover fit": preenche todo o espaço mantendo a proporção
            int imgW = imagemOriginal.getWidth();
            int imgH = imagemOriginal.getHeight();

            double scaleX = (double) w / imgW;
            double scaleY = (double) h / imgH;
            double scale  = Math.max(scaleX, scaleY);

            int drawW = (int) Math.round(imgW * scale);
            int drawH = (int) Math.round(imgH * scale);
            int offX  = (w - drawW) / 2;
            int offY  = (h - drawH) / 2;

            Shape clip = new RoundRectangle2D.Float(0, 0, w, h, raioCantos, raioCantos);
            g2.setClip(clip);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(imagemOriginal, offX, offY, drawW, drawH, null);
        } else {
            // Placeholder colorido com inicial
            UITheme.fillRounded(g2, corFallback, 0, 0, w, h, raioCantos);

            Color[] fgCores = {
                UITheme.STAT_BLUE_FG, UITheme.STAT_GREEN_FG,
                UITheme.STAT_RED_FG,  UITheme.STAT_PURPLE_FG,
                UITheme.WARNING
            };
            int idx = Math.abs(inicial.hashCode() % fgCores.length);
            g2.setColor(fgCores[idx]);
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(14, w / 3)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(inicial,
                (w - fm.stringWidth(inicial)) / 2,
                (h + fm.getAscent() - fm.getDescent()) / 2);
        }

        g2.dispose();
    }

    public void setRaioCantos(int raio) {
        this.raioCantos = raio;
        repaint();
    }
}
