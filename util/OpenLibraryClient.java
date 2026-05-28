package util;

import model.Livro;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenLibraryClient {

    // Expressões regulares para capturar propriedades específicas do JSON retornado pela API
    private static final Pattern TITLE_P = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern AUTHORS_P = Pattern.compile("\"authors\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern AUTHOR_NAME_P = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern COVER_P = Pattern.compile("\"cover\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
    private static final Pattern COVER_URL_P = Pattern.compile("\"(large|medium|small)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PUBLISH_DATE_P = Pattern.compile("\"publish_date\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LANGUAGES_P = Pattern.compile("\"languages\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern LANG_KEY_P = Pattern.compile("/languages/([a-zA-Z_]+)");

    // Método principal que consome a API da Open Library para buscar os dados de um livro pelo ISBN
    public static Livro buscarPorISBN(String isbn) throws Exception {
        if (isbn == null) return null;
        
        // Limpa o ISBN mantendo apenas números e letras X/x
        String clean = isbn.replaceAll("[^0-9Xx]", "");
        if (clean.isEmpty()) return null;

        // Monta a URL de consulta da API no formato JSON com os dados detalhados
        String urlStr = "https://openlibrary.org/api/books?bibkeys=ISBN:" + clean + "&format=json&jscmd=data";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000); // Define tempo limite de 5 segundos para conexão
        conn.setReadTimeout(5000);    // Define tempo limite de 5 segundos para leitura dos dados

        // Cancela a operação se a resposta do servidor não for sucesso (HTTP 200)
        int code = conn.getResponseCode();
        if (code != 200) return null;

        // Lê a resposta do servidor linha por linha utilizando codificação UTF-8
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line).append('\n');
        in.close();

        // Verifica se o corpo da resposta está vazio ou retornou um JSON sem dados ({})
        String body = sb.toString().trim();
        if (body.equals("{}") || body.isEmpty()) return null;

        // Localiza a chave do ISBN no JSON para isolar os dados deste livro específico
        String key = "\"ISBN:" + clean + "\"";
        int idx = body.indexOf(key);
        if (idx < 0) {
            // Se não encontrar com minúsculas, tenta buscar o ISBN com a letra X em maiúscula
            key = "\"ISBN:" + clean.toUpperCase() + "\"";
            idx = body.indexOf(key);
            if (idx < 0) return null; // Retorna nulo se o ISBN não constar na resposta
        }
        
        // Encontra o início do objeto JSON do livro logo após a chave do ISBN
        int start = body.indexOf('{', idx + key.length());
        if (start < 0) return null;
        
        // Estratégia para encontrar o fechamento do objeto JSON sem usar um parser formal
        int end = body.indexOf("}\n}", start); 
        if (end < 0) end = body.indexOf("}\n}", start);
        if (end < 0) end = body.lastIndexOf('}');
        if (end < start) end = body.length() - 1;
        
        // Recorta apenas o bloco de texto contendo as informações do livro
        String obj = body.substring(start, end + 1);

        // Instancia o objeto Livro que será preenchido
        Livro l = new Livro();
        l.setIsbn(clean);

        // Extrai o título do livro e remove caracteres de escape
        Matcher m = TITLE_P.matcher(obj);
        if (m.find()) l.setTitle(unescape(m.group(1)));

        // Extrai o bloco de autores e mapeia o nome de cada um deles
        m = AUTHORS_P.matcher(obj);
        if (m.find()) {
            String authorsBlock = m.group(1);
            Matcher ma = AUTHOR_NAME_P.matcher(authorsBlock);
            List<String> names = new ArrayList<>();
            while (ma.find()) names.add(unescape(ma.group(1)));
            // Agrupa os nomes dos autores separando-os por vírgula
            if (!names.isEmpty()) l.setAuthors(String.join(", ", names));
        }

        // Extrai o bloco de capas de imagem e seleciona a de melhor qualidade disponível
        m = COVER_P.matcher(obj);
        if (m.find()) {
            String coverBlock = m.group(1);
            Matcher mc = COVER_URL_P.matcher(coverBlock);
            String best = null;
            while (mc.find()) {
                String size = mc.group(1);
                String urlVal = mc.group(2);
                // Prioriza o tamanho grande ('large'), senão aceita o primeiro tamanho encontrado
                if ("large".equals(size)) { best = urlVal; break; }
                if (best == null) best = urlVal;
            }
            if (best != null) {
                // Corrige URLs relativas que começam com "//" injetando o protocolo HTTPS
                if (best.startsWith("//")) best = "https:" + best;
                l.setCover(best);
            }
        }

        // Extrai o ano de publicação procurando por uma sequência de 4 dígitos numéricos
        m = PUBLISH_DATE_P.matcher(obj);
        if (m.find()) {
            String pd = m.group(1);
            Matcher y = Pattern.compile("(\\d{4})").matcher(pd);
            if (y.find()) l.setPublishYear(Integer.parseInt(y.group(1)));
        }

        // Extrai o idioma e faz o mapeamento para o nome legível em português
        m = LANGUAGES_P.matcher(obj);
        if (m.find()) {
            String langBlock = m.group(1);
            Matcher lk = LANG_KEY_P.matcher(langBlock);
            if (lk.find()) {
                String code1 = lk.group(1).toLowerCase();
                l.setIdioma(mapLanguage(code1));
            }
        }

        return l;
    }

    // Remove barras invertidas de escape comuns em textos vindos diretamente do JSON bruto
    private static String unescape(String s) {
        return s.replaceAll("\\\\/", "/").replaceAll("\\\\\"", "\"");
    }

    // Mapeia as siglas de três letras de idiomas da API para seus respectivos nomes em português
    private static String mapLanguage(String code) {
        if (code == null) return null;
        code = code.toLowerCase();
        switch (code) {
            case "eng": return "Inglês";
            case "por": return "Português";
            case "spa": return "Espanhol";
            case "fre": case "fra": return "Francês";
            case "deu": case "ger": return "Alemão";
            case "ita": return "Italiano";
            default: return code; // Retorna a própria sigla caso não esteja na lista de mapeamento
        }
    }
}
