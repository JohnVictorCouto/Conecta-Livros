package model;

import java.time.LocalDate;

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
    private final LocalDate dataEmprestimo;
    private final LocalDate dataVencimento;
    private final LocalDate dataDevolucao;

    public Emprestimo(int id, int exemplarId, String tituloLivro, String codigoExemplar,
                      String nomeTomador, String contatoTomador, LocalDate dataEmprestimo,
                      LocalDate dataVencimento, LocalDate dataDevolucao) {
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
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }

    /** @return {@code true} enquanto não houver data de devolução registrada */
    public boolean isAtivo() {
        return dataDevolucao == null;
    }

    /** @return {@code true} se ainda ativo e com vencimento anterior à data atual */
    public boolean isAtrasado() {
        return isAtivo() && dataVencimento.isBefore(LocalDate.now());
    }
}
