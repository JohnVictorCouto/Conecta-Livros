CREATE DATABASE conecta_livros;
USE conecta_livros;

-- Execute este arquivo somente em uma instalação nova; ele não altera bancos existentes.

CREATE TABLE admin_account (
    id TINYINT PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    password_ciphertext VARBINARY(512) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_admin_account_singleton CHECK (id = 1)
);

CREATE TABLE app_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_ciphertext VARBINARY(512) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    resource_id VARCHAR(120) NOT NULL,
    success BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_log_created_at (created_at),
    INDEX idx_audit_log_action (action)
);

-- Classificações usadas para organizar livros e associar estantes.
CREATE TABLE genero (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Editoras referenciadas pelos registros de livros.
CREATE TABLE editora (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Locais físicos de armazenamento associados a um gênero do catálogo.
CREATE TABLE estante (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    genre_id INT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_estante_genero
    FOREIGN KEY (genre_id)
    REFERENCES genero(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

-- Listas nomeadas que podem reunir livros para uma turma ou atividade escolar.
CREATE TABLE lista_escolar (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT
);

-- Dados bibliográficos das obras; exemplares físicos são registrados separadamente.
CREATE TABLE livro (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    authors VARCHAR(255),
    cover VARCHAR(255),
    isbn VARCHAR(32),
    idioma VARCHAR(50),

    publish_year YEAR,

    genre_id INT NOT NULL,
    publisher_id INT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_livro_genero
    FOREIGN KEY (genre_id)
    REFERENCES genero(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT fk_livro_editora
    FOREIGN KEY (publisher_id)
    REFERENCES editora(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

-- Cada registro representa uma cópia física; a estante indica seu armazenamento.
CREATE TABLE exemplar (
    id INT AUTO_INCREMENT PRIMARY KEY,
    livro_id INT NOT NULL,
    estante_id INT,
    codigo VARCHAR(64) NOT NULL UNIQUE,
    conservacao VARCHAR(100),
    situacao ENUM(
        'DISPONIVEL',
        'RESERVADO',
        'EMPRESTADO',
        'MANUTENCAO',
        'PERDIDO',
        'INDISPONIVEL'
    ) NOT NULL DEFAULT 'DISPONIVEL',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_exemplar_livro_situacao (livro_id, situacao),

    CONSTRAINT fk_exemplar_estante
    FOREIGN KEY (estante_id)
    REFERENCES estante(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT fk_exemplar_livro
    FOREIGN KEY (livro_id)
    REFERENCES livro(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- Histórico de circulação: cada empréstimo vincula um tomador a um exemplar.
CREATE TABLE emprestimo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    exemplar_id INT NOT NULL,
    tomador_nome VARCHAR(150) NOT NULL,
    tomador_contato VARCHAR(255) NOT NULL,
    data_emprestimo DATETIME NOT NULL,
    data_vencimento DATETIME NOT NULL,
    data_devolucao DATETIME,
    INDEX idx_emprestimo_ativo_vencimento (data_devolucao, data_vencimento),
    INDEX idx_emprestimo_exemplar (exemplar_id),

    CONSTRAINT fk_emprestimo_exemplar
    FOREIGN KEY (exemplar_id)
    REFERENCES exemplar(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

-- Relação muitos-para-muitos entre listas escolares e livros do catálogo.
CREATE TABLE lista_livro (
    lista_id INT,
    livro_id INT,

    PRIMARY KEY (lista_id, livro_id),

    CONSTRAINT fk_lista_livro_lista
    FOREIGN KEY (lista_id)
    REFERENCES lista_escolar(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_lista_livro_livro
    FOREIGN KEY (livro_id)
    REFERENCES livro(id)
    ON DELETE CASCADE
);