package util;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Resolve textos da interface conforme o idioma ativo e formata mensagens
 * parametrizadas a partir do catálogo de traduções.
 */
public final class Tradutor {
    private static final String BUNDLE_BASE = "bundles.messages";
    private static final String PREFERENCE_KEY = "language";
    private static final List<Locale> IDIOMAS = Arrays.asList(
        criarLocale("pt", "BR"),
        criarLocale("en", "US"),
        criarLocale("es", "ES"),
        criarLocale("fr", "FR"),
        criarLocale("ja", "JP")
    );

    private static volatile Locale idioma = detectarIdioma();

    private Tradutor() {}

    /** @return idioma atualmente usado para resolver as mensagens */
    public static Locale getIdioma() {
        return idioma;
    }

    /** @return idiomas suportados pela interface */
    public static List<Locale> getIdiomasDisponiveis() {
        return IDIOMAS;
    }

    /** @param locale idioma a apresentar no seletor
     * @return nome localizado do idioma
     */
    public static String getNomeIdioma(Locale locale) {
        switch (locale.getLanguage()) {
            case "en": return "English";
            case "es": return "Español";
            case "fr": return "Français";
            case "ja": return "日本語";
            default: return "Português";
        }
    }

    /** Altera e persiste a preferência apenas se o idioma for suportado.
     * @param novoIdioma idioma escolhido
     */
    public static void definirIdioma(Locale novoIdioma) {
        if (novoIdioma == null || !idiomaSuportado(novoIdioma)) return;
        idioma = localeSuportado(novoIdioma);
        try {
            Preferences.userNodeForPackage(Tradutor.class)
                .put(PREFERENCE_KEY, idioma.toLanguageTag());
        } catch (RuntimeException ex) {
            System.err.println("Não foi possível salvar a preferência de idioma: " + ex.getMessage());
        }
    }

    /** Procura a mensagem no idioma ativo e recorre ao português como alternativa.
     * @param chave identificador presente nos arquivos de mensagens
     * @return texto traduzido, ou a própria chave se não houver tradução
     */
    public static String t(String chave) {
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE, idioma).getString(chave);
        } catch (MissingResourceException ex) {
            Locale fallback = criarLocale("pt", "BR");
            try {
                return ResourceBundle.getBundle(BUNDLE_BASE, fallback).getString(chave);
            } catch (MissingResourceException fallbackEx) {
                System.err.println("Tradução ausente: " + chave + " (" + idioma + ")");
                return chave;
            }
        }
    }

    /** Formata uma mensagem traduzida com os argumentos fornecidos.
     * @param chave identificador da mensagem
     * @param argumentos valores usados pelos marcadores da mensagem
     * @return mensagem formatada no idioma ativo
     */
    public static String formatar(String chave, Object... argumentos) {
        return new MessageFormat(t(chave), idioma).format(argumentos);
    }

    private static Locale detectarIdioma() {
        String salvo = null;
        try {
            salvo = Preferences.userNodeForPackage(Tradutor.class).get(PREFERENCE_KEY, null);
        } catch (RuntimeException ignored) {}

        if (salvo != null) {
            Locale escolhido = Locale.forLanguageTag(salvo);
            if (idiomaSuportado(escolhido)) return localeSuportado(escolhido);
        }

        Locale sistema = Locale.getDefault();
        if ("en".equals(sistema.getLanguage())) return criarLocale("en", "US");
        if ("es".equals(sistema.getLanguage())) return criarLocale("es", "ES");
        if ("fr".equals(sistema.getLanguage())) return criarLocale("fr", "FR");
        if ("ja".equals(sistema.getLanguage())) return criarLocale("ja", "JP");
        return criarLocale("pt", "BR");
    }

    private static Locale criarLocale(String idioma, String regiao) {
        return new Locale.Builder().setLanguage(idioma).setRegion(regiao).build();
    }

    private static boolean idiomaSuportado(Locale locale) {
        for (Locale suportado : IDIOMAS) {
            if (suportado.getLanguage().equals(locale.getLanguage())) return true;
        }
        return false;
    }

    private static Locale localeSuportado(Locale locale) {
        for (Locale suportado : IDIOMAS) {
            if (suportado.getLanguage().equals(locale.getLanguage())) return suportado;
        }
        return IDIOMAS.get(0);
    }
}
