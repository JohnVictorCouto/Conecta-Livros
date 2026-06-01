package model;

// Classe Estante - modelo de dados para estantes
public class Estante {
    
    private int id;
    private String name;
    private int genreId;
    
    public Estante() {
    }
    
    public Estante(int id, String name, int genreId) {
        this.id = id;
        this.name = name;
        this.genreId = genreId;
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
    
    public int getGenreId() {
        return genreId;
    }
    
    public void setGenreId(int genreId) {
        this.genreId = genreId;
    }
    
    @Override
    public String toString() {
        return name;
    }
}



