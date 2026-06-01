package model;

// Classe Livro - modelo de dados para livros
public class Livro {
    
    // ===== ATRIBUTOS DA CLASSE =====
    
    // ID unico do livro
    private int id;
    
    // Titulo do livro
    private String title;
    
    // Nome do(s) autor(es)
    private String authors;
    
    // Caminho da capa do livro
    private String cover;
    
    // Mes de publicacao (1-12)
    private int publishMonth;
    
    // Ano de publicacao
    private int publishYear;
    
    // Status de disponibilidade do livro
    private boolean disponibilidade;
    
    // ID do genero do livro
    private int genreId;
    
    // ID da editora do livro
    private int publisherId;
    
    // ID da estante onde o livro esta localizado
    private int estanteId;
    
    // Codigo ISBN do livro
    private String isbn;
    // Idioma do livro, ex: Portugues, Ingles
    private String idioma;
    
    // ===== CONSTRUTORES =====
    
    // Construtor vazio.
    // Utilizado para criar objetos que serao preenchidos depois.
    public Livro() {
    }
    
    // Construtor completo com todos os atributos.
    public Livro(int id, String title, String authors, String cover,
                 int publishMonth, int publishYear, boolean disponibilidade,
                 int genreId, int publisherId, int estanteId) {
        this(id, title, authors, cover, publishMonth, publishYear, disponibilidade,
            genreId, publisherId, estanteId, null, null);
    }
    
    // Construtor completo com ISBN e idioma.
    public Livro(int id, String title, String authors, String cover,
                 int publishMonth, int publishYear, boolean disponibilidade,
                 int genreId, int publisherId, int estanteId, String isbn, String idioma) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.cover = cover;
        this.publishMonth = publishMonth;
        this.publishYear = publishYear;
        this.disponibilidade = disponibilidade;
        this.genreId = genreId;
        this.publisherId = publisherId;
        this.estanteId = estanteId;
        this.isbn = isbn;
        this.idioma = idioma;
    }
    
    // ===== GETTERS E SETTERS =====

    
    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }
    
    public String getTitle() { 
        return title; 
    }
    
    public void setTitle(String title) { 
        this.title = title; 
    }
    
    public String getAuthors() { 
        return authors; 
    }
    
    public void setAuthors(String authors) { 
        this.authors = authors; 
    }
    
    public String getCover() { 
        return cover; 
    }
    
    public void setCover(String cover) { 
        this.cover = cover; 
    }
    
    public int getPublishMonth() { 
        return publishMonth; 
    }
    
    public void setPublishMonth(int publishMonth) { 
        this.publishMonth = publishMonth; 
    }
    
    public int getPublishYear() { 
        return publishYear; 
    }
    
    public void setPublishYear(int publishYear) { 
        this.publishYear = publishYear; 
    }
    
    public boolean isDisponibilidade() { 
        return disponibilidade; 
    }
    
    public void setDisponibilidade(boolean disponibilidade) { 
        this.disponibilidade = disponibilidade; 
    }
    
    public int getGenreId() { 
        return genreId; 
    }
    
    public void setGenreId(int genreId) { 
        this.genreId = genreId; 
    }
    
    public int getPublisherId() { 
        return publisherId; 
    }
    
    public void setPublisherId(int publisherId) { 
        this.publisherId = publisherId; 
    }
    
    public int getEstanteId() { 
        return estanteId; 
    }
    
    public void setEstanteId(int estanteId) { 
        this.estanteId = estanteId; 
    }
    
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }
    
    // ===== METODOS UTILITARIOS =====
    
    // Retorna uma representacao em texto do livro.
    @Override
    public String toString() {
        return "Livro{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", authors='" + authors + '\'' +
                ", disponibilidade=" + disponibilidade +
                ", isbn='" + (isbn != null ? isbn : "") + '\'' +
                ", idioma='" + (idioma != null ? idioma : "") + '\'' +
                '}';
    }
}




