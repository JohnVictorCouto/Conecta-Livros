package util;

public class IsbnUtils {

    // Remove hífens, espaços e caracteres especiais, mantendo apenas números e X
    public static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9Xx]", "");
    }

    // Identifica o tipo de ISBN pelo tamanho e chama a validação correta
    public static boolean isValid(String s) {
        if (s == null) return false;
        String c = clean(s);
        if (c.length() == 10) return isValidIsbn10(c);
        if (c.length() == 13) return isValidIsbn13(c);
        return false; // Retorna falso se não tiver 10 nem 13 dígitos
    }

    // Valida a estrutura e o dígito verificador de um ISBN de 10 dígitos
    public static boolean isValidIsbn10(String s) {
        if (s == null) return false;
        String c = clean(s);
        if (c.length() != 10) return false;
        
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char ch = c.charAt(i);
            int v;
            
            // O caractere 'X' ou 'x' representa o valor 10 e só vale na última posição
            if (i == 9 && (ch == 'X' || ch == 'x')) {
                v = 10;
            } else if (Character.isDigit(ch)) {
                v = ch - '0'; // Converte o caractere numérico para valor inteiro
            } else {
                return false; // Contém caractere inválido ou 'X' no lugar errado
            }
            
            // Soma o dígito multiplicado por pesos decrescentes de 10 a 1
            sum += (10 - i) * v;
        }
        return sum % 11 == 0; // O resultado final da soma deve ser múltiplo de 11
    }

    // Valida a estrutura e o dígito verificador de um ISBN de 13 dígitos
    public static boolean isValidIsbn13(String s) {
        if (s == null) return false;
        String c = clean(s);
        if (c.length() != 13) return false;
        
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            char ch = c.charAt(i);
            if (!Character.isDigit(ch)) return false; // ISBN-13 só aceita números
            
            int v = ch - '0';
            // Alterna o peso da multiplicação: peso 1 para índice par, peso 3 para ímpar
            sum += (i % 2 == 0) ? v : v * 3;
        }
        return sum % 10 == 0; // O resultado final da soma deve ser múltiplo de 10
    }

    // Converte um ISBN-10 existente para o formato atualizado de 13 dígitos
    public static String toIsbn13(String isbn10) {
        String c = clean(isbn10);
        if (c.length() != 10) return null;
        
        String core = c.substring(0, 9); // Remove o dígito verificador antigo do ISBN-10
        String pref = "978" + core;      // Adiciona o prefixo internacional de livros
        
        // Inicia o cálculo do novo dígito verificador para o formato de 13 dígitos
        int sum = 0;
        for (int i = 0; i < pref.length(); i++) {
            int v = pref.charAt(i) - '0';
            sum += (i % 2 == 0) ? v : v * 3;
        }
        
        // Regra matemática para obter o dígito complementar final (0 a 9)
        int check = (10 - (sum % 10)) % 10;
        return pref + check;
    }

    // Converte um ISBN-13 de volta para o formato antigo de 10 dígitos
    public static String toIsbn10(String isbn13) {
        String c = clean(isbn13);
        if (c.length() != 13) return null;
        if (!c.startsWith("978")) return null; // Apenas o prefixo 978 possui correspondente antigo
        
        String core = c.substring(3, 12); // Remove o prefixo "978" e o dígito verificador final
        
        // Inicia o cálculo do novo dígito verificador para o formato de 10 dígitos
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int v = core.charAt(i) - '0';
            sum += (10 - i) * v;
        }
        
        // Regra matemática do módulo 11 para definir o dígito verificador
        int rem = 11 - (sum % 11);
        String check;
        if (rem == 10) check = "X";       // Resto 10 vira o caractere romano X
        else if (rem == 11) check = "0";  // Resto 11 zera o dígito
        else check = String.valueOf(rem); // Restos de 1 a 9 mantêm o próprio número
        
        return core + check;
    }
}
