package util;

public class IsbnUtils {

    public static String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9Xx]", "");
    }

    public static boolean isValid(String s) {
        if (s == null) return false;
        String c = clean(s);
        if (c.length() == 10) return isValidIsbn10(c);
        if (c.length() == 13) return isValidIsbn13(c);
        return false;
    }

    public static boolean isValidIsbn10(String s) {
        if (s == null) return false;
        String c = clean(s);
        if (c.length() != 10) return false;
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char ch = c.charAt(i);
            int v;
            if (i == 9 && (ch == 'X' || ch == 'x')) v = 10;
            else if (Character.isDigit(ch)) v = ch - '0';
            else return false;
            sum += (10 - i) * v;
        }
        return sum % 11 == 0;
    }

    public static boolean isValidIsbn13(String s) {
        if (s == null) return false;
        String c = clean(s);
        if (c.length() != 13) return false;
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            char ch = c.charAt(i);
            if (!Character.isDigit(ch)) return false;
            int v = ch - '0';
            sum += (i % 2 == 0) ? v : v * 3;
        }
        return sum % 10 == 0;
    }

    public static String toIsbn13(String isbn10) {
        String c = clean(isbn10);
        if (c.length() != 10) return null;
        String core = c.substring(0, 9);
        String pref = "978" + core;
        int sum = 0;
        for (int i = 0; i < pref.length(); i++) {
            int v = pref.charAt(i) - '0';
            sum += (i % 2 == 0) ? v : v * 3;
        }
        int check = (10 - (sum % 10)) % 10;
        return pref + check;
    }

    public static String toIsbn10(String isbn13) {
        String c = clean(isbn13);
        if (c.length() != 13) return null;
        if (!c.startsWith("978")) return null;
        String core = c.substring(3, 12); // 9 digits
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int v = core.charAt(i) - '0';
            sum += (10 - i) * v;
        }
        int rem = 11 - (sum % 11);
        String check;
        if (rem == 10) check = "X";
        else if (rem == 11) check = "0";
        else check = String.valueOf(rem);
        return core + check;
    }
}
