package model;

/**
 * Classe Genero - Modelo de Dados para Gêneros
 */
public class Genero {
    
    private int id;
    private String name;
    
    public Genero() {
    }
    
    public Genero(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
