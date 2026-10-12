package util;

import model.Livro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Busca metadados de livros por ISBN em serviços públicos e combina os dados
 * complementares quando mais de uma fonte retorna informações.
 */
public final class GoogleBooksClient {
    private static final String API_URL = "https://www.googleapis.com/books/v1/volumes";
    private static final String OPEN_LIBRARY_URL = "https://openlibrary.org/isbn/";
    private static final int TIMEOUT_MILLIS = 8000;
    private static final int MAX_RESPONSE_CHARS = 1_000_000;
    private static final int MAX_503_ATTEMPTS = 3;

    private GoogleBooksClient() {}

    /**
     * Consulta primeiro o Google Books quando há chave configurada e usa
     * a Open Library como alternativa ou complemento.
     *
     * @param isbn ISBN de entrada, com ou sem pontuação
     * @return livro encontrado, ou {@code null} quando nenhuma fonte o reconhece
     * @throws IOException se as fontes consultadas falharem
     */
    public static Livro buscarPorISBN(String isbn) throws IOException {
        String clean = IsbnUtils.clean(isbn);
        if (clean.isEmpty()) return null;

        String apiKey = configuracao();
        IOException googleBooksFailure = null;
        if (apiKey != null) {
            try {
                Livro book = buscarPorConsulta("isbn:" + clean, clean, apiKey);
                if (book == null) {
                    book = buscarPorConsulta(clean, clean, apiKey);
                }
                if (book != null) {
                    try {
                        return combinarMetadados(book, buscarNaOpenLibrary(clean));
                    } catch (IOException ex) {
                        System.err.println("Não foi possível complementar os metadados com a Open Library: "
                            + ex.getMessage());
                        return book;
                    }
                }
            } catch (IOException ex) {
                googleBooksFailure = ex;
            }
        }

        try {
            Livro book = buscarNaOpenLibrary(clean);
            if (book != null) return book;
        } catch (IOException ex) {
            if (googleBooksFailure != null) ex.addSuppressed(googleBooksFailure);
            throw ex;
        }
        if (googleBooksFailure != null) throw googleBooksFailure;
        return null;
    }

    private static Livro combinarMetadados(Livro principal, Livro complementar) {
        if (principal == null) return complementar;
        if (complementar == null) return principal;
        if (vazio(principal.getTitle())) principal.setTitle(complementar.getTitle());
        if (vazio(principal.getAuthors())) principal.setAuthors(complementar.getAuthors());
        if (vazio(principal.getCover())) principal.setCover(complementar.getCover());
        if (vazio(principal.getIdioma())) principal.setIdioma(complementar.getIdioma());
        if (principal.getPublishYear() == 0) principal.setPublishYear(complementar.getPublishYear());
        if (vazio(principal.getGeneroCatalogo())) {
            principal.setGeneroCatalogo(complementar.getGeneroCatalogo());
        }
        if (vazio(principal.getEditoraCatalogo())) {
            principal.setEditoraCatalogo(complementar.getEditoraCatalogo());
        }
        return principal;
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static Livro buscarNaOpenLibrary(String isbn) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(
            OPEN_LIBRARY_URL + encode(isbn) + ".json").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "ConectaLivros/1.0");

        String response;
        int status;
        try {
            status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return null;
            InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
            response = stream == null ? "" : lerResposta(stream);
            if (status < 200 || status >= 300) {
                throw new IOException("Open Library retornou HTTP " + status
                    + (response.isEmpty() ? "." : ": " + response));
            }
        } finally {
            connection.disconnect();
        }
        return parseOpenLibraryResponse(response, isbn);
    }

    static Livro parseOpenLibraryResponse(String response, String isbn) throws IOException {
        Object parsed;
        try {
            parsed = new JsonParser(response).parse();
        } catch (IllegalArgumentException ex) {
            throw new IOException("Resposta inválida da Open Library.", ex);
        }

        Map<?, ?> details = asObject(parsed);
        String title = string(details.get("title"));
        if (title == null || title.isBlank()) return null;

        Livro book = new Livro();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthors(buscarAutoresOpenLibrary(asArray(details.get("authors"))));
        preencherDataOpenLibrary(book, string(details.get("publish_date")));
        book.setEditoraCatalogo(primeiroTexto(asArray(details.get("publishers"))));
        book.setGeneroCatalogo(buscarGeneroOpenLibrary(details));

        List<?> languages = asArray(details.get("languages"));
        if (languages != null && !languages.isEmpty()) {
            String languageKey = string(asObject(languages.get(0)).get("key"));
            if (languageKey != null) {
                int separator = languageKey.lastIndexOf('/');
                book.setIdioma(mapLanguage(separator >= 0
                    ? languageKey.substring(separator + 1) : languageKey));
            }
        }

        List<?> covers = asArray(details.get("covers"));
        if (covers != null && !covers.isEmpty() && covers.get(0) instanceof Number) {
            long coverId = ((Number) covers.get(0)).longValue();
            if (coverId > 0) {
                book.setCover("https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg");
            }
        }
        return book;
    }

    private static String buscarGeneroOpenLibrary(Map<?, ?> details) {
        String subject = primeiroTexto(asArray(details.get("subjects")));
        if (subject != null) return subject;

        List<?> works = asArray(details.get("works"));
        if (works == null) return null;
        for (Object workValue : works) {
            String key = string(asObject(workValue).get("key"));
            if (key == null || !key.startsWith("/works/")) continue;
            try {
                Map<?, ?> work = carregarObjetoOpenLibrary(key + ".json");
                subject = primeiroTexto(asArray(work.get("subjects")));
                if (subject != null) return subject;
            } catch (IOException ex) {
                System.err.println("Erro ao carregar assuntos da obra na Open Library: "
                    + ex.getMessage());
            }
        }
        return null;
    }

    private static String buscarAutoresOpenLibrary(List<?> authors) {
        if (authors == null || authors.isEmpty()) return null;
        List<String> names = new ArrayList<>();
        for (Object authorValue : authors) {
            String key = string(asObject(authorValue).get("key"));
            if (key == null || !key.startsWith("/authors/")) continue;
            try {
                Map<?, ?> author = carregarObjetoOpenLibrary(key + ".json");
                String name = string(author.get("name"));
                if (name != null && !name.isBlank()) names.add(name);
            } catch (IOException ex) {
                System.err.println("Erro ao carregar autor da Open Library: " + ex.getMessage());
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static Map<?, ?> carregarObjetoOpenLibrary(String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(
            "https://openlibrary.org" + path).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "ConectaLivros/1.0");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Open Library retornou HTTP " + status + " ao carregar autor.");
            }
            try (InputStream stream = connection.getInputStream()) {
                return asObject(new JsonParser(lerResposta(stream)).parse());
            } catch (IllegalArgumentException ex) {
                throw new IOException("Resposta inválida da Open Library ao carregar autor.", ex);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void preencherDataOpenLibrary(Livro book, String date) {
        if (date == null) return;
        java.util.regex.Matcher year = java.util.regex.Pattern.compile("(\\d{4})").matcher(date);
        if (!year.find()) return;
        book.setPublishYear(Integer.parseInt(year.group(1)));
    }

    private static Livro buscarPorConsulta(String query, String isbn, String apiKey) throws IOException {
        for (int tentativa = 0; ; tentativa++) {
            try {
                return buscarPorConsultaUmaVez(query, isbn, apiKey);
            } catch (HttpStatusException ex) {
                if (ex.getStatusCode() != 503 || tentativa + 1 >= MAX_503_ATTEMPTS) {
                    throw ex;
                }
                aguardarNovaTentativa(tentativa + 1);
            }
        }
    }

    private static Livro buscarPorConsultaUmaVez(String query, String isbn, String apiKey) throws IOException {
        String url = API_URL + "?q=" + encode(query)
            + "&maxResults=40&key=" + encode(apiKey);
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("Accept", "application/json");

        String response;
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
            response = stream == null ? "" : lerResposta(stream);
            if (status < 200 || status >= 300) {
                throw new HttpStatusException("Google Books API retornou HTTP " + status
                    + (response.isEmpty() ? "." : ": " + response), status);
            }
        } finally {
            connection.disconnect();
        }

        return parseResponse(response, isbn);
    }

    private static void aguardarNovaTentativa(int tentativa) throws IOException {
        try {
            Thread.sleep(500L * tentativa);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Busca na Google Books interrompida durante a espera para nova tentativa.", ex);
        }
    }

    private static final class HttpStatusException extends IOException {
        private final int statusCode;

        private HttpStatusException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        private int getStatusCode() {
            return statusCode;
        }
    }

    static Livro parseResponse(String response, String isbn) throws IOException {
        Object parsed;
        try {
            parsed = new JsonParser(response).parse();
        } catch (IllegalArgumentException ex) {
            throw new IOException("Resposta inválida da Google Books API.", ex);
        }

        Map<?, ?> root = asObject(parsed);
        List<?> items = asArray(root.get("items"));
        if (items == null || items.isEmpty()) return null;

        for (Object item : items) {
            Map<?, ?> volume = asObject(asObject(item).get("volumeInfo"));
            if (!containsIsbn(asArray(volume.get("industryIdentifiers")), isbn)) continue;
            String title = string(volume.get("title"));
            if (title == null || title.isBlank()) continue;

            Livro book = new Livro();
            book.setIsbn(isbn);
            book.setTitle(title);
            book.setAuthors(joinAuthors(asArray(volume.get("authors"))));
            book.setEditoraCatalogo(string(volume.get("publisher")));
            book.setGeneroCatalogo(primeiroTexto(asArray(volume.get("categories"))));

            String publishedDate = string(volume.get("publishedDate"));
            preencherDataPublicacao(book, publishedDate);
            book.setIdioma(mapLanguage(string(volume.get("language"))));

            Map<?, ?> imageLinks = asObject(volume.get("imageLinks"));
            String cover = firstNonBlank(
                string(imageLinks.get("thumbnail")),
                string(imageLinks.get("smallThumbnail")));
            if (cover != null) book.setCover(cover.replaceFirst("^http://", "https://"));
            return book;
        }
        return null;
    }

    private static boolean containsIsbn(List<?> identifiers, String requestedIsbn) {
        if (identifiers == null) return false;
        String cleanRequested = IsbnUtils.clean(requestedIsbn);
        String requestedIsbn13 = IsbnUtils.toIsbn13(cleanRequested);
        String requestedIsbn10 = IsbnUtils.toIsbn10(cleanRequested);
        for (Object value : identifiers) {
            String identifier = IsbnUtils.clean(string(asObject(value).get("identifier")));
            if (identifier.equals(cleanRequested)
                    || (!identifier.isEmpty() && identifier.equals(requestedIsbn13))
                    || (!identifier.isEmpty() && identifier.equals(requestedIsbn10))) {
                return true;
            }
        }
        return false;
    }

    private static String lerResposta(InputStream stream) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                if (response.length() + count > MAX_RESPONSE_CHARS) {
                    throw new IOException("Resposta da Google Books API excedeu o limite permitido.");
                }
                response.append(buffer, 0, count);
            }
        }
        return response.toString();
    }

    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private static String configuracao() {
        String value = primeiroConfigurado(
            System.getProperty("google.books.apiKey"),
            System.getProperty("GoogleBooksAPI"),
            System.getenv("GOOGLE_BOOKS_API_KEY"),
            System.getenv("GoogleBooksAPI"));
        return value == null ? null : value.trim();
    }

    private static String primeiroConfigurado(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static Map<?, ?> asObject(Object value) {
        return value instanceof Map<?, ?> ? (Map<?, ?>) value : java.util.Collections.emptyMap();
    }

    private static List<?> asArray(Object value) {
        return value instanceof List<?> ? (List<?>) value : null;
    }

    private static String string(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private static String joinAuthors(List<?> authors) {
        if (authors == null || authors.isEmpty()) return null;
        List<String> names = new ArrayList<>();
        for (Object author : authors) {
            if (author instanceof String && !((String) author).isBlank()) {
                names.add((String) author);
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static String primeiroTexto(List<?> values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value instanceof String && !((String) value).isBlank()) {
                return ((String) value).trim();
            }
        }
        return null;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        return second != null && !second.isBlank() ? second : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static void preencherDataPublicacao(Livro book, String date) {
        if (date == null || !date.matches("\\d{4}(-\\d{2})?(-\\d{2})?")) return;
        book.setPublishYear(Integer.parseInt(date.substring(0, 4)));
    }

    private static String mapLanguage(String code) {
        if (code == null || code.isBlank()) return null;
        switch (code.toLowerCase(Locale.ROOT)) {
            case "en": case "eng": return "Inglês";
            case "pt": case "por": return "Português";
            case "es": case "spa": return "Espanhol";
            case "fr": case "fre": case "fra": return "Francês";
            case "ja": case "jpn": return "Japonês";
            case "de": case "deu": case "ger": return "Alemão";
            case "it": case "ita": return "Italiano";
            case "zh": return "Chinês";
            case "ru": return "Russo";
            default: return code;
        }
    }

    private static final class JsonParser {
        private final String input;
        private int position;

        JsonParser(String input) {
            this.input = input;
        }

        Object parse() {
            Object value = readValue();
            skipWhitespace();
            if (position != input.length()) fail();
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (position >= input.length()) return fail();
            char current = input.charAt(position);
            if (current == '{') return readObject();
            if (current == '[') return readArray();
            if (current == '"') return readString();
            if (current == 't') return readLiteral("true", Boolean.TRUE);
            if (current == 'f') return readLiteral("false", Boolean.FALSE);
            if (current == 'n') return readLiteral("null", null);
            if (current == '-' || Character.isDigit(current)) return readNumber();
            return fail();
        }

        private Map<String, Object> readObject() {
            position++;
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) return object;
            do {
                skipWhitespace();
                if (position >= input.length() || input.charAt(position) != '"') return fail();
                String key = readString();
                skipWhitespace();
                if (!consume(':')) return fail();
                object.put(key, readValue());
                skipWhitespace();
                if (consume('}')) return object;
            } while (consume(','));
            return fail();
        }

        private List<Object> readArray() {
            position++;
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) return array;
            do {
                array.add(readValue());
                skipWhitespace();
                if (consume(']')) return array;
            } while (consume(','));
            return fail();
        }

        private String readString() {
            position++;
            StringBuilder value = new StringBuilder();
            while (position < input.length()) {
                char current = input.charAt(position++);
                if (current == '"') return value.toString();
                if (current < 0x20) return fail();
                if (current != '\\') {
                    value.append(current);
                    continue;
                }
                if (position >= input.length()) return fail();
                char escaped = input.charAt(position++);
                switch (escaped) {
                    case '"': value.append('"'); break;
                    case '\\': value.append('\\'); break;
                    case '/': value.append('/'); break;
                    case 'b': value.append('\b'); break;
                    case 'f': value.append('\f'); break;
                    case 'n': value.append('\n'); break;
                    case 'r': value.append('\r'); break;
                    case 't': value.append('\t'); break;
                    case 'u':
                        if (position + 4 > input.length()) return fail();
                        try {
                            value.append((char) Integer.parseInt(input.substring(position, position + 4), 16));
                        } catch (NumberFormatException ex) {
                            return fail();
                        }
                        position += 4;
                        break;
                    default: return fail();
                }
            }
            return fail();
        }

        private Object readNumber() {
            int start = position;
            if (consume('-') && position == input.length()) return fail();
            while (position < input.length() && Character.isDigit(input.charAt(position))) position++;
            if (consume('.')) {
                int fractionStart = position;
                while (position < input.length() && Character.isDigit(input.charAt(position))) position++;
                if (position == fractionStart) return fail();
            }
            if (position < input.length() && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
                position++;
                if (position < input.length() && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                    position++;
                }
                int exponentStart = position;
                while (position < input.length() && Character.isDigit(input.charAt(position))) position++;
                if (position == exponentStart) return fail();
            }
            try {
                return Double.valueOf(input.substring(start, position));
            } catch (NumberFormatException ex) {
                return fail();
            }
        }

        private Object readLiteral(String literal, Object value) {
            if (!input.startsWith(literal, position)) return fail();
            position += literal.length();
            return value;
        }

        private boolean consume(char expected) {
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++;
        }

        private <T> T fail() {
            throw new IllegalArgumentException("JSON inválido na posição " + position);
        }
    }
}
