CREATE DATABASE biblioteca;

USE biblioteca;

CREATE TABLE livro (

    id INT AUTO_INCREMENT PRIMARY KEY,

    book_id VARCHAR(150),

    titulo VARCHAR(255),

    serie VARCHAR(255),

    autor TEXT,

    avaliacao DOUBLE,

    descricao LONGTEXT,

    idioma VARCHAR(50),

    isbn VARCHAR(50),

    generos LONGTEXT,

    personagens LONGTEXT,

    formato VARCHAR(100),

    edicao VARCHAR(150),

    paginas INT,

    editora VARCHAR(255),

    data_publicacao VARCHAR(50),

    primeira_publicacao VARCHAR(50),

    premios LONGTEXT,

    numero_avaliacoes BIGINT,

    avaliacoes_estrelas LONGTEXT,

    percentual_curtidas INT,

    configuracoes LONGTEXT,

    capa_url TEXT,

    bbe_score BIGINT,

    bbe_votes BIGINT,

    preco DOUBLE

);