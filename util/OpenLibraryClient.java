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

    private static final Pattern TITLE_P = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern AUTHORS_P = Pattern.compile("\"authors\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern AUTHOR_NAME_P = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern COVER_P = Pattern.compile("\"cover\"\\s*:\\s*\\{(.*?)\\}", Pattern.DOTALL);
    private static final Pattern COVER_URL_P = Pattern.compile("\"(large|medium|small)\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern PUBLISH_DATE_P = Pattern.compile("\"publish_date\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LANGUAGES_P = Pattern.compile("\"languages\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern LANG_KEY_P = Pattern.compile("/languages/([a-zA-Z_]+)");

    public static Livro buscarPorISBN(String isbn) throws Exception {
        if (isbn == null) return null;
        String clean = isbn.replaceAll("[^0-9Xx]", "");
        if (clean.isEmpty()) return null;

        String urlStr = "https://openlibrary.org/api/books?bibkeys=ISBN:" + clean + "&format=json&jscmd=data";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int code = conn.getResponseCode();
        if (code != 200) return null;

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line).append('\n');
        in.close();

        String body = sb.toString().trim();
        if (body.equals("{}") || body.isEmpty()) return null;

        // extract object for key ISBN:xxxx
        String key = "\"ISBN:" + clean + "\"";
        int idx = body.indexOf(key);
        if (idx < 0) {
            // try with uppercase X
            key = "\"ISBN:" + clean.toUpperCase() + "\"";
            idx = body.indexOf(key);
            if (idx < 0) return null;
        }
        int start = body.indexOf('{', idx + key.length());
        if (start < 0) return null;
        // find matching closing brace — naive approach: find the first closing brace followed by '}' balancing is complex; instead take from start to last '}' before next key or end
        int end = body.indexOf("}\n}", start); // try common pattern
        if (end < 0) end = body.indexOf("}\n}", start);
        if (end < 0) end = body.lastIndexOf('}');
        if (end < start) end = body.length() - 1;
        String obj = body.substring(start, end + 1);

        Livro l = new Livro();
        l.setIsbn(clean);

        // title
        Matcher m = TITLE_P.matcher(obj);
        if (m.find()) l.setTitle(unescape(m.group(1)));

        // authors
        m = AUTHORS_P.matcher(obj);
        if (m.find()) {
            String authorsBlock = m.group(1);
            Matcher ma = AUTHOR_NAME_P.matcher(authorsBlock);
            List<String> names = new ArrayList<>();
            while (ma.find()) names.add(unescape(ma.group(1)));
            if (!names.isEmpty()) l.setAuthors(String.join(", ", names));
        }

        // cover
        m = COVER_P.matcher(obj);
        if (m.find()) {
            String coverBlock = m.group(1);
            Matcher mc = COVER_URL_P.matcher(coverBlock);
            String best = null;
            while (mc.find()) {
                String size = mc.group(1);
                String urlVal = mc.group(2);
                if ("large".equals(size)) { best = urlVal; break; }
                if (best == null) best = urlVal;
            }
            if (best != null) {
                if (best.startsWith("//")) best = "https:" + best;
                l.setCover(best);
            }
        }

        // publish year
        m = PUBLISH_DATE_P.matcher(obj);
        if (m.find()) {
            String pd = m.group(1);
            Matcher y = Pattern.compile("(\\d{4})").matcher(pd);
            if (y.find()) l.setPublishYear(Integer.parseInt(y.group(1)));
        }

        // language
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

    private static String unescape(String s) {
        return s.replaceAll("\\\\/", "/").replaceAll("\\\\\"", "\"");
    }

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
            default: return code;
        }
    }
}
