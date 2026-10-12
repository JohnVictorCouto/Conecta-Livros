package model;

/**
 * Uma cópia física de uma obra do catálogo.
 */
public class Exemplar {

    /** Estados que descrevem a disponibilidade operacional de uma cópia. */
    public enum Situacao {
        DISPONIVEL,
        RESERVADO,
        EMPRESTADO,
        MANUTENCAO,
        PERDIDO,
        INDISPONIVEL
    }

    private int id;
    private int livroId;
    private int estanteId;
    private String codigo;
    private String localizacao;
    private String conservacao;
    private Situacao situacao;

    public Exemplar() {
    }

    public Exemplar(int id, int livroId, int estanteId, String codigo, String localizacao,
                    String conservacao, Situacao situacao) {
        this.id = id;
        this.livroId = livroId;
        this.estanteId = estanteId;
        this.codigo = codigo;
        this.localizacao = localizacao;
        this.conservacao = conservacao;
        this.situacao = situacao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLivroId() {
        return livroId;
    }

    public void setLivroId(int livroId) {
        this.livroId = livroId;
    }

    public int getEstanteId() {
        return estanteId;
    }

    public void setEstanteId(int estanteId) {
        this.estanteId = estanteId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getConservacao() {
        return conservacao;
    }

    public void setConservacao(String conservacao) {
        this.conservacao = conservacao;
    }

    public Situacao getSituacao() {
        return situacao;
    }

    public void setSituacao(Situacao situacao) {
        this.situacao = situacao;
    }
}
