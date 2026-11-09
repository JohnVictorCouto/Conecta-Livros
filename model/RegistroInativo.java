package model;

/**
 * Resumo somente de leitura de um registro inativado logicamente no catálogo.
 * Os dados são exibidos para consulta sem apagar o registro nem seu histórico.
 */
public class RegistroInativo {
    private String tipo;
    private int id;
    private String descricao;

    public RegistroInativo() {}

    public RegistroInativo(String tipo, int id, String descricao) {
        this.tipo = tipo;
        this.id = id;
        this.descricao = descricao;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
