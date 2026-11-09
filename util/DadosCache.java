package util;

import dao.GeneroDAO;
import dao.EditoraDAO;
import dao.EstanteDAO;
import model.Genero;
import model.Editora;
import model.Estante;

import java.util.HashMap;
import java.util.Map;

/**
 * Mantém em memória os nomes de gêneros, editoras e estantes para resolver
 * identificadores sem repetir consultas ao banco durante a exibição.
 */
public class DadosCache {
    
    private static final Map<Integer, String> GENEROS_CACHE = new HashMap<>();
    private static final Map<Integer, String> EDITORAS_CACHE = new HashMap<>();
    private static final Map<Integer, String> ESTANTES_CACHE = new HashMap<>();
    
    private static boolean carregado = false;
    
    /** Carrega os três cadastros somente uma vez até que o cache seja limpo. */
    public static synchronized void carregar() {
        if (carregado) return;
        
        try {
            for (Genero g : GeneroDAO.obterTodos()) {
                GENEROS_CACHE.put(g.getId(), g.getName());
            }
            for (Editora e : EditoraDAO.obterTodos()) {
                EDITORAS_CACHE.put(e.getId(), e.getName());
            }
            for (Estante s : EstanteDAO.obterTodos()) {
                ESTANTES_CACHE.put(s.getId(), s.getName());
            }
            carregado = true;
        } catch (Exception e) {
            System.err.println("Erro ao carregar cache: " + e.getMessage());
        }
    }
    
    /** @param id identificador do gênero
     * @return nome armazenado ou rótulo padrão com o identificador
     */
    public static String getNomeGenero(int id) {
        if (!carregado) carregar();
        return GENEROS_CACHE.getOrDefault(id, "Gênero " + id);
    }
    
    /** @param id identificador da editora
     * @return nome armazenado ou rótulo padrão com o identificador
     */
    public static String getNomeEditora(int id) {
        if (!carregado) carregar();
        return EDITORAS_CACHE.getOrDefault(id, "Editora " + id);
    }
    
    /** @param id identificador da estante
     * @return nome armazenado ou rótulo padrão com o identificador
     */
    public static String getNomeEstante(int id) {
        if (!carregado) carregar();
        return ESTANTES_CACHE.getOrDefault(id, "Estante " + id);
    }
    
    /** Esvazia os mapas e permite que a próxima consulta recarregue os dados. */
    public static void limpar() {
        GENEROS_CACHE.clear();
        EDITORAS_CACHE.clear();
        ESTANTES_CACHE.clear();
        carregado = false;
    }
}
