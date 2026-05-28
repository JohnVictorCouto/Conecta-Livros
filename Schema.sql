CREATE DATABASE conecta_livros;
USE conecta_livros;

-- Tabela Gênero
CREATE TABLE genero (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Tabela Editora
CREATE TABLE editora (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL
);

-- Tabela Estante
CREATE TABLE estante (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    genre_id INT NOT NULL,

    CONSTRAINT fk_estante_genero
    FOREIGN KEY (genre_id)
    REFERENCES genero(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

-- Tabela Lista Escolar
CREATE TABLE lista_escolar (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT
);

-- Tabela Livro
CREATE TABLE livro (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    authors VARCHAR(255),
    cover VARCHAR(255),
    isbn VARCHAR(32),
    idioma VARCHAR(50),

    publish_month TINYINT,
    publish_year YEAR,

    disponibilidade BOOLEAN DEFAULT TRUE,

    genre_id INT NOT NULL,
    publisher_id INT NOT NULL,
    estante_id INT,

    CONSTRAINT fk_livro_genero
    FOREIGN KEY (genre_id)
    REFERENCES genero(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT fk_livro_editora
    FOREIGN KEY (publisher_id)
    REFERENCES editora(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT fk_livro_estante
    FOREIGN KEY (estante_id)
    REFERENCES estante(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

-- Tabela intermediária Lista_Livro
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