package view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Componente para exibir capas de livros com carregamento assíncrono
 * Suporta URLs remotas com fallback para placeholder colorido
 */
public class ImagemCapa extends JPanel {
    
    private static final ConcurrentHashMap<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final int TIMEOUT_MS = 5000;
    
    private String urlCapa;
    private BufferedImage imagem;
    private boolean carregando = false;
    private int raioCantos = 8;
    private Color corFallback;
    private String inicial;
    
    public ImagemCapa(String urlCapa, int id, String titulo) {
        this.urlCapa = urlCapa;
        this.setOpaque(false);
        
        // Define cor de fallback baseada no ID
        Color[] cores = {
            new Color(219, 234, 254), new Color(220, 252, 231),
            new Color(254, 226, 226), new Color(237, 233, 254),
            new Color(254, 243, 199)
        };
        this.corFallback = cores[(id % cores.length + cores.length) % cores.length];
        
        // Define inicial do título
        this.inicial = (titulo != null && !titulo.isEmpty()) 
            ? String.valueOf(titulo.charAt(0)).toUpperCase() 
            : "?";
        
        // Carrega a imagem de forma assíncrona
        if (urlCapa != null && !urlCapa.trim().isEmpty()) {
            carregarImagemAsync();
        }
    }
    
    private void carregarImagemAsync() {
        carregando = true;
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                try {
                    // Verifica cache
                    if (CACHE.containsKey(urlCapa)) {
                        return CACHE.get(urlCapa);
                    }
                    
                    // Carrega da URL
                    URL url = new URL(urlCapa);
                    URLConnection connection = url.openConnection();
                    connection.setConnectTimeout(TIMEOUT_MS);
                    connection.setReadTimeout(TIMEOUT_MS);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    
                    BufferedImage img = javax.imageio.ImageIO.read(connection.getInputStream());
                    
                    if (img != null) {
                        // Redimensiona para proporção de capa de livro (9:12)
                        int largura = getWidth() > 0 ? getWidth() : 120;
                        int altura = (int) (largura * 1.4);
                        
                        BufferedImage redimensionada = new BufferedImage(
                            largura, altura, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2 = redimensionada.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2.drawImage(img, 0, 0, largura, altura, null);
                        g2.dispose();
                        
                        CACHE.put(urlCapa, redimensionada);
                        return redimensionada;
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao carregar capa: " + e.getMessage());
                }
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    imagem = get();
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
        
        if (imagem != null) {
            // Desenha a imagem com cantos arredondados
            Shape clip = new RoundRectangle2D.Float(0, 0, w, h, raioCantos, raioCantos);
            g2.setClip(clip);
            g2.drawImage(imagem, 0, 0, w, h, null);
        } else {
            // Fallback: cor sólida com inicial
            UITheme.fillRounded(g2, corFallback, 0, 0, w, h, raioCantos);
            
            // Desenha a inicial
            Color[] fgCores = {
                UITheme.STAT_BLUE_FG, UITheme.STAT_GREEN_FG,
                UITheme.STAT_RED_FG, UITheme.STAT_PURPLE_FG,
                UITheme.WARNING
            };
            
            int idx = (inicial.hashCode() % fgCores.length + fgCores.length) % fgCores.length;
            g2.setColor(fgCores[idx]);
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(16, w / 3)));
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
