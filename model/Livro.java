package model;

/**
 * Representa uma obra do catálogo e mantém seus metadados, classificação
 * e contagens de exemplares carregadas pelas consultas de persistência.
 */
public class Livro {
    
    // ===== ATRIBUTOS DA CLASSE =====
    
    /** ID único do livro */
    private int id;
    
    /** Título do livro */
    private String title;
    
    /** Nome do(s) autor(es) */
    private String authors;
    
    /** Caminho da capa do livro */
    private String cover;
    
    /** Ano de publicação */
    private int publishYear;
    
    /** Quantidade de exemplares com cada situação, calculada pelo DAO. */
    private int exemplaresDisponiveis;
    private int exemplaresEmprestados;
    
    /** ID do gênero do livro */
    private int genreId;
    
    /** ID da editora do livro */
    private int publisherId;
    
    /** Código ISBN do livro */
    private String isbn;
    /** Idioma do livro, ex: Português, Inglês */
    private String idioma;

    /** Metadados textuais do catálogo usados ao preencher o formulário. */
    private String generoCatalogo;
    private String editoraCatalogo;
    
    // ===== CONSTRUTORES =====
    
    /**
     * Construtor vazio.
     * Utilizado para criar instâncias vazias que serão preenchidas posteriormente.
     */
    public Livro() {
    }
    
    /**
     * Construtor completo com todos os atributos.
     */
    public Livro(int id, String title, String authors, String cover,
                 int publishYear, int genreId, int publisherId) {
        this(id, title, authors, cover, publishYear, genreId, publisherId,
            null, null);
    }
    
    /**
     * Construtor completo com descrição.
     */
    public Livro(int id, String title, String authors, String cover,
                 int publishYear, int genreId, int publisherId,
                 String isbn, String idioma) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.cover = cover;
        this.publishYear = publishYear;
        this.genreId = genreId;
        this.publisherId = publisherId;
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
    
    public int getPublishYear() { 
        return publishYear; 
    }
    
    public void setPublishYear(int publishYear) { 
        this.publishYear = publishYear; 
    }
    
    /**
     * Indica se a obra pode ser emprestada com base na contagem de cópias
     * disponíveis carregada pelo DAO.
     * @return {@code true} quando há pelo menos um exemplar disponível
     */
    public boolean isDisponibilidade() {
        return exemplaresDisponiveis > 0;
    }

    public int getExemplaresDisponiveis() {
        return exemplaresDisponiveis;
    }

    public void setExemplaresDisponiveis(int exemplaresDisponiveis) {
        this.exemplaresDisponiveis = exemplaresDisponiveis;
    }

    public int getExemplaresEmprestados() {
        return exemplaresEmprestados;
    }

    public void setExemplaresEmprestados(int exemplaresEmprestados) {
        this.exemplaresEmprestados = exemplaresEmprestados;
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

    public String getGeneroCatalogo() {
        return generoCatalogo;
    }

    public void setGeneroCatalogo(String generoCatalogo) {
        this.generoCatalogo = generoCatalogo;
    }

    public String getEditoraCatalogo() {
        return editoraCatalogo;
    }

    public void setEditoraCatalogo(String editoraCatalogo) {
        this.editoraCatalogo = editoraCatalogo;
    }
    
    // ===== MÉTODOS UTILITÁRIOS =====
    
    /**
     * Retorna uma representação em string do livro.
     */
    @Override
    public String toString() {
        return "Livro{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", authors='" + authors + '\'' +
                ", exemplaresDisponiveis=" + exemplaresDisponiveis +
                ", isbn='" + (isbn != null ? isbn : "") + '\'' +
                ", idioma='" + (idioma != null ? idioma : "") + '\'' +
                '}';
    }
}
