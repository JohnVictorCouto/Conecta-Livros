package model;

import java.time.LocalDateTime;

/**
 * Registra a circulação de um exemplar e os dados do tomador, incluindo
 * as datas necessárias para determinar se o empréstimo segue ativo ou atrasado.
 */
public class Emprestimo {

    private final int id;
    private final int exemplarId;
    private final String tituloLivro;
    private final String codigoExemplar;
    private final String nomeTomador;
    private final String contatoTomador;
    private final LocalDateTime dataEmprestimo;
    private final LocalDateTime dataVencimento;
    private final LocalDateTime dataDevolucao;

    public Emprestimo(int id, int exemplarId, String tituloLivro, String codigoExemplar,
                      String nomeTomador, String contatoTomador, LocalDateTime dataEmprestimo,
                      LocalDateTime dataVencimento, LocalDateTime dataDevolucao) {
        this.id = id;
        this.exemplarId = exemplarId;
        this.tituloLivro = tituloLivro;
        this.codigoExemplar = codigoExemplar;
        this.nomeTomador = nomeTomador;
        this.contatoTomador = contatoTomador;
        this.dataEmprestimo = dataEmprestimo;
        this.dataVencimento = dataVencimento;
        this.dataDevolucao = dataDevolucao;
    }

    public int getId() { return id; }
    public int getExemplarId() { return exemplarId; }
    public String getTituloLivro() { return tituloLivro; }
    public String getCodigoExemplar() { return codigoExemplar; }
    public String getNomeTomador() { return nomeTomador; }
    public String getContatoTomador() { return contatoTomador; }
    public LocalDateTime getDataEmprestimo() { return dataEmprestimo; }
    public LocalDateTime getDataVencimento() { return dataVencimento; }
    public LocalDateTime getDataDevolucao() { return dataDevolucao; }

    /** @return {@code true} enquanto não houver data de devolução registrada */
    public boolean isAtivo() {
        return dataDevolucao == null;
    }

    /** @return {@code true} se ainda ativo e com vencimento anterior ao momento atual */
    public boolean isAtrasado() {
        return isAtivo() && dataVencimento.isBefore(LocalDateTime.now());
    }
}
