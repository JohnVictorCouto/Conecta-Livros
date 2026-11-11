package util;

import model.Emprestimo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Codificador e interpretador JSON dos formatos definidos para o protocolo
 * entre Cliente e Servidor; restringe a conversão aos tipos esperados.
 */
public final class WireJson {
    private WireJson() {}

    public static String stringify(Object value) {
        StringBuilder json = new StringBuilder();
        write(value, json);
        return json.toString();
    }

    public static Object parse(String json) {
        Parser parser = new Parser(json);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.finished()) throw new IllegalArgumentException("Dados após a mensagem JSON.");
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<T> type) {
        return (T) convertValue(value, type);
    }

    private static Object convertValue(Object value, Class<?> type) {
        if (value == null) return null;
        if (type == Object.class || type.isInstance(value)) return value;
        if (type == String.class) return String.valueOf(value);
        if (type == int.class || type == Integer.class) return ((Number) value).intValue();
        if (type == long.class || type == Long.class) return ((Number) value).longValue();
        if (type == boolean.class || type == Boolean.class) return value;
        if (type == LocalDate.class) return LocalDate.parse((String) value);
        if (type == LocalDateTime.class) return LocalDateTime.parse((String) value);
        if (type.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<? extends Enum>) type, (String) value);
            return enumValue;
        }
        // Limita a reflexão aos modelos do protocolo, sem permitir instanciar tipos arbitrários.
        if (!type.getName().startsWith("model.") || !(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Tipo não permitido no protocolo: " + type.getName());
        }

        Map<?, ?> fields = (Map<?, ?>) value;
        // Empréstimos usam nomes de campos legados que não correspondem diretamente ao modelo.
        if (type == Emprestimo.class) {
            return new Emprestimo(
                integer(fields, "id"),
                integer(fields, "exemplarId"),
                string(fields, "tituloLivro"),
                string(fields, "codigoExemplar"),
                string(fields, "nomeTomador"),
                string(fields, "contatoTomador"),
                dateTime(fields, "dataEmprestimo"),
                dateTime(fields, "dataVencimento"),
                dateTime(fields, "dataDevolucao"));
        }

        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                Object fieldValue = fields.get(field.getName());
                if (fieldValue == null && field.getType().isPrimitive()) continue;
                field.setAccessible(true);
                field.set(instance, convertValue(fieldValue, field.getType()));
            }
            return instance;
        } catch (ReflectiveOperationException | SecurityException ex) {
            throw new IllegalArgumentException("Não foi possível converter o modelo " + type.getName(), ex);
        }
    }

    private static int integer(Map<?, ?> fields, String name) {
        return ((Number) fields.get(name)).intValue();
    }

    private static String string(Map<?, ?> fields, String name) {
        Object value = fields.get(name);
        return value == null ? null : (String) value;
    }

    private static LocalDateTime dateTime(Map<?, ?> fields, String name) {
        Object value = fields.get(name);
        return value == null ? null : LocalDateTime.parse((String) value);
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String || value instanceof Character) {
            quote(String.valueOf(value), out);
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Enum<?> || value instanceof LocalDate
                || value instanceof LocalDateTime) {
            quote(value.toString(), out);
        } else if (value instanceof Iterable<?>) {
            out.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) out.append(',');
                write(item, out);
                first = false;
            }
            out.append(']');
        } else if (value instanceof Map<?, ?>) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) out.append(',');
                quote(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out);
                first = false;
            }
            out.append('}');
        } else if (value.getClass().getName().startsWith("model.")) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (Field field : value.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    fields.put(field.getName(), field.get(value));
                } catch (IllegalAccessException | SecurityException ex) {
                    throw new IllegalArgumentException("Não foi possível serializar o modelo.", ex);
                }
            }
            write(fields, out);
        } else {
            throw new IllegalArgumentException("Tipo não permitido no protocolo: " + value.getClass().getName());
        }
    }

    private static void quote(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(character);
                    }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            if (input == null) throw new IllegalArgumentException("Mensagem JSON ausente.");
            this.input = input;
        }

        private Object value() {
            whitespace();
            if (finished()) throw error("Valor JSON ausente.");
            char c = input.charAt(index);
            if (c == '"') return string();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == 't' && literal("true")) return Boolean.TRUE;
            if (c == 'f' && literal("false")) return Boolean.FALSE;
            if (c == 'n' && literal("null")) return null;
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new LinkedHashMap<>();
            index++;
            whitespace();
            if (take('}')) return result;
            do {
                whitespace();
                if (finished() || input.charAt(index) != '"') throw error("Chave JSON inválida.");
                String key = string();
                whitespace();
                expect(':');
                result.put(key, value());
                whitespace();
                if (take('}')) return result;
                expect(',');
            } while (true);
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<>();
            index++;
            whitespace();
            if (take(']')) return result;
            do {
                result.add(value());
                whitespace();
                if (take(']')) return result;
                expect(',');
            } while (true);
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (!finished()) {
                char c = input.charAt(index++);
                if (c == '"') return result.toString();
                if (c != '\\') {
                    result.append(c);
                    continue;
                }
                if (finished()) throw error("Escape JSON incompleto.");
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u':
                        if (index + 4 > input.length()) throw error("Escape unicode incompleto.");
                        result.append((char) Integer.parseInt(input.substring(index, index + 4), 16));
                        index += 4;
                        break;
                    default: throw error("Escape JSON inválido.");
                }
            }
            throw error("String JSON não terminada.");
        }

        private Number number() {
            int start = index;
            if (take('-')) {}
            while (!finished() && Character.isDigit(input.charAt(index))) index++;
            boolean decimal = false;
            if (take('.')) {
                decimal = true;
                while (!finished() && Character.isDigit(input.charAt(index))) index++;
            }
            if (!finished() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (!finished() && (input.charAt(index) == '+' || input.charAt(index) == '-')) index++;
                while (!finished() && Character.isDigit(input.charAt(index))) index++;
            }
            if (start == index) throw error("Número JSON inválido.");
            String value = input.substring(start, index);
            try {
                return decimal ? Double.valueOf(value) : Long.valueOf(value);
            } catch (NumberFormatException ex) {
                throw error("Número JSON inválido.");
            }
        }

        private boolean literal(String value) {
            if (!input.startsWith(value, index)) return false;
            index += value.length();
            return true;
        }

        private void whitespace() {
            while (!finished() && Character.isWhitespace(input.charAt(index))) index++;
        }

        private boolean take(char expected) {
            if (!finished() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) throw error("Esperado '" + expected + "'.");
        }

        private boolean finished() {
            return index >= input.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " Posição " + index + ".");
        }
    }
}
