package view;

import dao.GeneroDAO;
import dao.EditoraDAO;
import dao.EstanteDAO;
import model.Genero;
import model.Editora;
import model.Estante;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache em memória para nomes de gêneros, editoras e estantes
 * Evita múltiplas consultas ao banco de dados
 */
public class DadosCache {
    
    private static final Map<Integer, String> GENEROS_CACHE = new HashMap<>();
    private static final Map<Integer, String> EDITORAS_CACHE = new HashMap<>();
    private static final Map<Integer, String> ESTANTES_CACHE = new HashMap<>();
    
    private static boolean carregado = false;
    
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
    
    public static String getNomeGenero(int id) {
        if (!carregado) carregar();
        return GENEROS_CACHE.getOrDefault(id, "Gênero " + id);
    }
    
    public static String getNomeEditora(int id) {
        if (!carregado) carregar();
        return EDITORAS_CACHE.getOrDefault(id, "Editora " + id);
    }
    
    public static String getNomeEstante(int id) {
        if (!carregado) carregar();
        return ESTANTES_CACHE.getOrDefault(id, "Estante " + id);
    }
    
    public static void limpar() {
        GENEROS_CACHE.clear();
        EDITORAS_CACHE.clear();
        ESTANTES_CACHE.clear();
        carregado = false;
    }
}
