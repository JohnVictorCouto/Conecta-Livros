# Conecta Livros

Aplicação desktop em Java Swing para gestão do acervo bibliográfico de uma biblioteca, escola ou organização de livros. O sistema permite cadastrar livros, exemplares, estantes, listas escolares, acompanhar empréstimos e navegar por um catálogo com filtros, indicadores e interface multilíngue.

## Visão geral do projeto

A aplicação segue uma arquitetura simples em camadas:

- `model/`: classes de domínio e entidades persistidas
- `dao/`: acesso a dados com JDBC e SQL
- `util/`: serviços auxiliares de conexão, criptografia, tradução e integração externa
- `view/`: telas Swing e componentes de interface
- `bundles/`: arquivos de internacionalização
- `images/`: ícones e imagens de apoio
- `lib/`: biblioteca do driver MySQL

O ponto de entrada da aplicação é a classe `view.MainFrame`, que monta a janela principal e alterna entre as telas do sistema usando `CardLayout`.

## Estrutura de pastas e arquivos

### Raiz do projeto

- `Schema.sql`: script de criação do banco de dados MySQL

### `dao/`

Pacote responsável pela persistência e leitura de dados no banco MySQL.

- `LivroDAO.java`: CRUD de livros, consulta com contagem de exemplares disponíveis/emprestados e filtros por gênero.
- `ExemplarDAO.java`: gerenciamento de exemplares físicos, incluindo situação, localização, código e estado de conservação.
- `EstanteDAO.java`: cadastro e consulta de estantes.
- `GeneroDAO.java`: cadastro e listagem de gêneros.
- `EditoraDAO.java`: cadastro e listagem de editoras.
- `ListaEscolarDAO.java`: CRUD de listas escolares e associação de livros a listas.
- `EmprestimoDAO.java`: registro de empréstimos, devoluções e consulta do histórico.

### `model/`

Pacote com as entidades de negócio.

- `Livro.java`: representa uma obra ou título do catálogo, incluindo metadados como autores, ISBN, idioma, gênero e editora.
- `Exemplar.java`: representa uma cópia física do livro, com código, localização, conservação e situação (`DISPONIVEL`, `EMPRESTADO`, etc.).
- `Estante.java`: representa uma estante associada a um gênero.
- `Genero.java`: representa um gênero de classificação.
- `Editora.java`: representa uma editora.
- `ListaEscolar.java`: representa uma lista escolar com nome e descrição.
- `Emprestimo.java`: representa um empréstimo, armazenando tomador, contato, datas e status do prazo.

### `util/`

Pacote com utilidades de infraestrutura e integração.

- `ConnectionFactory.java`: centraliza a conexão JDBC com MySQL e o fechamento de recursos. Carrega configurações a partir de `DB_URL`, `DB_USER`, `DB_PASSWORD` ou propriedades Java.
- `CryptoAES.java`: utilitário para criptografia AES, usado para proteção de dados sensíveis.
- `DadosCache.java`: cache local em memória para acelerar carregamento de listas e dados reutilizados pela interface.
- `GoogleBooksClient.java`: cliente de integração com a API do Google Books para buscar detalhes de livros por ISBN.
- `IsbnUtils.java`: funções utilitárias para validação e normalização de ISBN.
- `Tradutor.java`: responsável pela internacionalização e carregamento de textos dos arquivos de mensagens.

### `view/`

Pacote com tela principal, componentes visuais e operações da interface Java Swing.

- `MainFrame.java`: janela principal da aplicação. Monta a barra de navegação, alterna telas e controla a identidade do idioma.
- `DashboardView.java`: painel inicial com estatísticas do acervo e cards de recomendação.
- `LivroView.java`: galeria de livros com filtros por gênero e disponibilidade.
- `BuscaView.java`: tela de pesquisa com filtros por gênero, editora, localização e ordenação.
- `EstanteView.java`: gestão de estantes em tabela, com busca, edição e exclusão.
- `ListaEscolarView.java`: gestão de listas escolares e associação de livros.
- `EmprestimosView.java`: listagem de empréstimos com filtros por ativos, atrasados e devolvidos.
- `LivroFormDialog.java`: formulário de cadastro/edição de livros.
- `ExemplarDialog.java`: diálogo para gerenciamento de exemplares.
- `EmprestimoDialog.java`: formulário para registrar novos empréstimos e devoluções.
- `CatalogoDialog.java`: diálogo de manutenção dos catálogos de gêneros e editoras.
- `LivroAcoes.java`: helper para ações de livro dentro da interface.
- `ImagemCapa.java`: componente responsável por exibir imagens de capa.
- `ComponenteUI.java`: conjunto de componentes customizados e estilos visuais da aplicação.
- `UITheme.java`: paleta, tipografia e estilos compartilhados da interface.
- `UITheme.java`: define cores e padronização visual da aplicação.

### `bundles/`

Arquivos de recursos usados para internacionalização:

- `messages_pt_BR.properties`: português (Brasil)
- `messages_en_US.properties`: inglês
- `messages_es_ES.properties`: espanhol
- `messages_fr_FR.properties`: francês
- `messages_ja_JP.properties`: japonês

Esses arquivos alimentam a classe `Tradutor`, que seleciona o idioma do sistema e troca os textos da interface dinamicamente.

## Modelo de dados e banco

O banco é criado pelo script `Schema.sql` e possui as tabelas principais:

- `genero`
- `editora`
- `estante`
- `lista_escolar`
- `livro`
- `exemplar`
- `emprestimo`
- `lista_livro` (associação entre listas e livros)

Principais regras do modelo:

- Cada `livro` pertence a um `genero` e a uma `editora`.
- Cada `exemplar` pertence a um `livro` e pode estar em uma `estante`.
- A disponibilidade é calculada pela situação do exemplar (`DISPONIVEL`, `EMPRESTADO`, etc.).
- A tabela `emprestimo` registra o tomador, o contato, a data de empréstimo, a data de vencimento e a data de devolução.

## Fluxo de funcionamento

1. A aplicação inicia em `view.MainFrame`.
2. A janela principal monta a navegação e carrega os módulos de tela.
3. Cada tela usa classes DAO para consultar ou persistir dados no MySQL.
4. O banco retorna instâncias das classes do pacote `model`.
5. A interface exibe os dados em tabelas, cards, formulários e galerias.

## Dependências e requisitos

- JDK 11 ou superior
- MySQL 8
- Driver JDBC no caminho `lib/mysql-connector-j-8.4.0.jar`
- Configuração de credenciais do banco via variáveis de ambiente ou propriedades Java

## Configuração do banco

O projeto espera que o banco seja configurado antes da execução. A aplicação lê as variáveis ou propriedades abaixo:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- ou equivalentes `db.url`, `db.user`, `db.password`

Exemplo em Linux/macOS:

```bash
export DB_URL='jdbc:mysql://localhost:3306/conecta_livros?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC'
export DB_USER='seu_usuario'
export DB_PASSWORD='sua_senha'
```

Exemplo em PowerShell:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/conecta_livros?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC'
$env:DB_USER='seu_usuario'
$env:DB_PASSWORD='sua_senha'
```

Se `DB_URL` não for definido, o sistema utiliza o valor padrão do banco `conecta_livros`.

## Integração com Google Books API

A busca por livro por ISBN consulta a Open Library e também pode usar a API do Google Books quando a chave estiver configurada com a variável:

- `GOOGLE_BOOKS_API_KEY`

ou pela propriedade Java:

- `google.books.apiKey`

A busca via Open Library funciona sem chave.

## Compilar e executar

### Maven

O Maven e as extensões Java da IDE usam o driver MySQL diretamente de
`lib/mysql-connector-j-8.4.0.jar`. Para compilar e empacotar:

```powershell
mvn package
```

O pacote de dependências resultante fica em `target/dependency`, incluindo o
conector MySQL. Para iniciar a aplicação no Windows pelo PowerShell:

```powershell
$classpath = "target\classes;target\dependency\*"
java -cp $classpath view.MainFrame
```

### Linux/macOS

```bash
mkdir -p out
javac -encoding UTF-8 -cp 'lib/mysql-connector-j-8.4.0.jar' \
  -d out $(find dao model util view -name '*.java')
cp -R images out/
cp -R bundles out/
java -cp 'out:lib/mysql-connector-j-8.4.0.jar' view.MainFrame
```

### Windows (PowerShell)

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$sources = Get-ChildItem dao, model, util, view -Recurse -Filter *.java |
  Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -cp "lib\mysql-connector-j-8.4.0.jar" -d out $sources
Copy-Item -Recurse -Force images, bundles -Destination out
java -cp "out;lib\mysql-connector-j-8.4.0.jar" view.MainFrame
```

Importante: se a pasta `bundles` não for copiada para `out`, a aplicação pode não localizar os arquivos de tradução ao iniciar.

## Resumo da arquitetura

O projeto é uma aplicação desktop Java Swing com persistência em MySQL, organizada em quatro blocos principais:

- `model/`: entidades e regras de negócio
- `dao/`: acesso ao banco e consultas SQL
- `util/`: infraestrutura, cache, tradução e integrações
- `view/`: interface gráfica e navegação entre telas

Esse padrão torna a aplicação simples de manter, facilita a extensão de novas telas e mantém o código de acesso ao banco separado da camada visual.
