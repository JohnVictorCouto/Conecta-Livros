# Conecta Livros

Aplicação Cliente-Servidor em Java Swing para gestão de acervo bibliográfico. O `Cliente.jar` contém somente a interface e fachadas de acesso remoto; o `Servidor.jar` atende as requisições, autentica contas e acessa o MySQL por JDBC.

## Visão geral do projeto

O projeto mantém as telas existentes e separa as responsabilidades:

- `model/`: classes de domínio e entidades persistidas
- `dao/`: fachadas usadas pelo Cliente; encaminham as chamadas ao Servidor por Socket
- `server/`: socket servidor, autenticação ADMIN, validação de operações e auditoria
- `util/`: protocolo de mensagens, tradução, cache e integração externa
- `view/`: telas Swing e componentes de interface
- `bundles/`: arquivos de internacionalização
- `images/`: ícones e imagens de apoio
- `lib/`: biblioteca do driver MySQL

Os pontos de entrada são `view.MainFrame` no Cliente e `server.Server` no Servidor. As mensagens do protocolo são JSON de formato fixo e não usam desserialização Java arbitrária.

## Estrutura de pastas e arquivos

### Raiz do projeto

- `Schema.sql`: esquema inicial do banco MySQL para instalação nova; não altera bancos existentes.

### `dao/`

Fachadas do Cliente que preservam os métodos usados pelas telas e enviam chamadas ao Servidor.

- `LivroDAO.java`: CRUD de livros, consulta com contagem de exemplares disponíveis/emprestados e filtros por gênero.
- `ExemplarDAO.java`: gerenciamento de exemplares físicos, incluindo situação, estante, código e estado de conservação.
- `EstanteDAO.java`: cadastro e consulta de estantes.
- `GeneroDAO.java`: cadastro e listagem de gêneros.
- `EditoraDAO.java`: cadastro e listagem de editoras.
- `ListaEscolarDAO.java`: CRUD de listas escolares e associação de livros a listas.
- `EmprestimoDAO.java`: registro de empréstimos, devoluções e consulta do histórico.

### `model/`

Pacote com as entidades de negócio.

- `Livro.java`: representa uma obra ou título do catálogo, incluindo metadados como autores, ISBN, idioma, gênero e editora.
- `Exemplar.java`: representa uma cópia física do livro, com código, estante, conservação e situação (`DISPONIVEL`, `EMPRESTADO`, etc.).
- `Estante.java`: representa uma estante associada a um gênero.
- `Genero.java`: representa um gênero de classificação.
- `Editora.java`: representa uma editora.
- `ListaEscolar.java`: representa uma lista escolar com nome e descrição.
- `Emprestimo.java`: representa um empréstimo, armazenando tomador, contato, datas e status do prazo.

### `util/`

Pacote com utilidades de infraestrutura e integração.

- `ConnectionFactory.java`: centraliza conexão JDBC no Servidor. Não é empacotada no Cliente.
- `CryptoAES.java`: utilitário AES-GCM usado pelo Servidor para validar a conta ADMIN.
- `RemoteClient.java` e `WireJson.java`: transporte TCP de mensagens JSON restritas a operações e modelos conhecidos; I/O fora da EDT.
- `DadosCache.java`: cache local em memória para acelerar carregamento de listas e dados reutilizados pela interface.
- `GoogleBooksClient.java`: cliente de integração com a API do Google Books para buscar detalhes de livros por ISBN.
- `IsbnUtils.java`: funções utilitárias para validação e normalização de ISBN.
- `Tradutor.java`: responsável pela internacionalização e carregamento de textos dos arquivos de mensagens.

### `view/`

Pacote com tela principal, componentes visuais e operações da interface Java Swing.

- `MainFrame.java`: janela principal da aplicação. Monta a barra de navegação, alterna telas e controla a identidade do idioma.
- `DashboardView.java`: painel inicial com estatísticas do acervo e cards de recomendação.
- `LivroView.java`: galeria de livros com filtros por gênero e disponibilidade.
- `BuscaView.java`: tela de pesquisa com filtros por gênero, editora e ordenação.
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

## Estrutura completa do repositório

Abaixo está o inventário completo dos arquivos relevantes do projeto, incluindo pacotes de fonte, recursos e artefatos de build que normalmente ficam no repositório:

### Arquivos de configuração e documentação

- `.gitignore`: ignora arquivos locais e artefatos de build.
- `LICENSE.txt`: licença do projeto.
- `PLANO.md`: plano de implementação/execução do projeto.
- `pom.xml`: build do Maven com dependências e empacotamento dos JARs do Cliente e Servidor.
- `Schema.sql`: criação do banco e estrutura inicial da aplicação.
- `config.properties`: configuração local do servidor (não deve ser enviado com segredos em produção).
- `README.md`: documentação do projeto.

### `dao/`

- `CatalogoInativoDAO.java`: acesso às opções de catálogo inativas.
- `EditoraDAO.java`: manutenção de editoras.
- `EmprestimoDAO.java`: operações de empréstimo, devolução e consulta.
- `EstanteDAO.java`: cadastro e busca de estantes.
- `ExemplarDAO.java`: manutenção de exemplares.
- `GeneroDAO.java`: manutenção de gêneros.
- `ListaEscolarDAO.java`: listas escolares e associação com livros.
- `LivroDAO.java`: CRUD de livros e filtros de consulta.

### `model/`

- `Editora.java`: entidade da editora.
- `Emprestimo.java`: entidade de empréstimo.
- `Estante.java`: entidade de estante.
- `Exemplar.java`: entidade de exemplar físico.
- `Genero.java`: entidade de gênero.
- `ListaEscolar.java`: entidade de lista escolar.
- `Livro.java`: entidade de livro.
- `RegistroInativo.java`: registro de itens inativos ou desativados.

### `server/`

- `AdminAuthService.java`: serviço de autenticação administrativa.
- `ClientSocket.java`: socket do cliente para comunicação com o servidor.
- `CoverSyncService.java`: sincronização de capas de livros.
- `EmailNotificationService.java`: envio de notificações por e-mail.
- `EmailVerificationService.java`: verificação de e-mail no cadastro.
- `Server.java`: ponto de entrada do processo servidor.
- `ServerDAO.java`: acesso JDBC e regras de persistência do servidor.
- `ServerRequestHandler.java`: processamento das requisições recebidas via socket.
- `ServerSchemaReadiness.java`: validação do schema do banco antes de operar.

### `util/`

- `ConnectionFactory.java`: criação de conexões JDBC.
- `CryptoAES.java`: cifra AES-GCM para senhas e dados sensíveis.
- `DadosCache.java`: cache em memória dos dados mais usados.
- `EmailAddressValidator.java`: validação de endereços de e-mail.
- `GoogleBooksClient.java`: consulta a APIs externas de livro/ISBN.
- `IsbnUtils.java`: validação e normalização de ISBN.
- `RemoteClient.java`: cliente remoto para comunicação com o servidor.
- `Tradutor.java`: loader de traduções e internacionalização.
- `WireJson.java`: serialização e parsing de mensagens JSON do protocolo interno.

### `view/`

- `BuscaView.java`: tela de busca e filtros.
- `CatalogoDialog.java`: diálogo de cadastro de catálogos.
- `CatalogoInativoDialog.java`: manutenção de itens inativos.
- `ComponenteUI.java`: componentes e estilos visuais personalizados.
- `DashboardView.java`: painel de indicadores e dashboard.
- `EmprestimoDialog.java`: cadastro e fechamento de empréstimos.
- `EmprestimosView.java`: listagem e filtros de empréstimos.
- `EstanteView.java`: gestão de estantes.
- `ExemplarDialog.java`: gestão de exemplares físicos.
- `ImagemCapa.java`: apresentação de capa do livro.
- `ListaEscolarView.java`: gestão de listas escolares.
- `LivroAcoes.java`: ações auxiliares de livro na interface.
- `LivroFormDialog.java`: formulário de cadastro/edição de livro.
- `LivroView.java`: visualização e listagem dos livros.
- `MainFrame.java`: tela principal da aplicação desktop.
- `UITheme.java`: tema visual da aplicação.

### `tests/`

- `CryptoAESTest.java`: testes do módulo de criptografia.
- `EmailAddressValidatorTest.java`: testes de validação de e-mail.
- `EmailNotificationServiceTest.java`: testes de notificações por e-mail.
- `EmprestimoTest.java`: testes do modelo de empréstimo.
- `EmprestimosCsvTest.java`: testes da exportação/CSV de empréstimos.
- `EmprestimosEmailTest.java`: testes de e-mail para empréstimos.
- `IsbnUtilsTest.java`: testes de ISBN.
- `LivroTest.java`: testes do modelo de livro.
- `MainFrameNavLinkTest.java`: testes de navegação principal da UI.
- `ServerRequestHandlerTest.java`: testes de requisições do servidor.
- `ServerSchemaReadinessTest.java`: testes de readiness/schema do banco.
- `WireJsonTest.java`: testes do protocolo JSON interno.

### `bundles/`

- `messages_en_US.properties`
- `messages_es_ES.properties`
- `messages_fr_FR.properties`
- `messages_ja_JP.properties`
- `messages_pt_BR.properties`

### `images/`

- `Icon.png`
- `icone.png`

### `lib/`

- `mysql-connector-j-8.4.0.jar`: driver JDBC do MySQL.

### `target/`

A pasta `target/` é gerada por compilação do Maven e contém JARs finais, classes compiladas e relatórios de teste. Não é fonte da aplicação, mas faz parte do ciclo de build:

- `conecta-livros-1.0.0-Cliente.jar`
- `conecta-livros-1.0.0-Servidor.jar`
- `dependency/` com bibliotecas de runtime
- `surefire-reports/` com relatórios de testes
- `classes/` com bytecode compilado

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
- `admin_account` (conta administrativa inicial)
- `app_account` (contas criadas pela tela de cadastro)

Cada instalação representa uma única biblioteca; não há cadastro ou seleção de múltiplas bibliotecas.
Contas criadas publicamente têm as mesmas permissões administrativas para operar essa biblioteca.
As senhas dessas contas são cifradas com AES-GCM por `CryptoAES` e a chave do servidor; não são armazenadas em texto puro.

Para instalações existentes, crie a tabela de contas antes de iniciar a nova versão do Servidor:

```sql
USE conecta_livros;

CREATE TABLE app_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_ciphertext VARBINARY(512) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Principais regras do modelo:

- Cada `livro` pertence a um `genero` e a uma `editora`.
- Cada `exemplar` pertence a um `livro` e pode estar em uma `estante`.
- A disponibilidade é calculada pela situação do exemplar (`DISPONIVEL`, `EMPRESTADO`, etc.).
- A tabela `emprestimo` registra o tomador, o contato, a data de empréstimo, a data de vencimento e a data de devolução.

## Fluxo de funcionamento

1. A aplicação inicia em `view.MainFrame`.
2. A janela principal monta a navegação e carrega os módulos de tela.
3. Cada tela chama uma fachada do pacote `dao`; o Cliente envia a operação ao Servidor autenticado.
4. O Servidor valida a sessão, executa o DAO JDBC em `server/ServerDAO.java` e devolve o resultado.
5. A interface exibe os dados em tabelas, cards, formulários e galerias.

## Dependências e requisitos

- JDK 25 (o Maven compila com `--release 25`)
- MySQL 8
- Driver JDBC em `lib/mysql-connector-j-8.4.0.jar`, carregado pelo Servidor
- Jakarta Mail/Angus Mail, incluído nas dependências de execução do Servidor
- Configuração do banco somente no ambiente do Servidor

## Configuração do banco

O Servidor lê as variáveis de ambiente, propriedades Java ou `config.properties` local para conectar ao banco. O Cliente não carrega nem lê as configurações de banco.

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- ou equivalentes `db.url`, `db.user`, `db.password`

Exemplo de variáveis do Servidor em Linux/macOS:

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

`DB_URL` pode omitir-se para usar a URL local padrão do banco `conecta_livros`; `DB_USER` e `DB_PASSWORD` são obrigatórios. Não mantenha `config.properties` com segredos no controle de versão. O arquivo está ignorado pelo `.gitignore`.

### Verificação de e-mail no cadastro

Antes de criar uma conta, o Servidor envia um código aleatório de 8 dígitos ao endereço informado. A conta só é gravada após a confirmação do código, que expira em 10 minutos e permite até cinco tentativas. Configure no ambiente, em propriedades Java (`email.host`, `email.password`, `email.smtp.host`, `email.smtp.port`) ou no `config.properties` do Servidor:

- `EMAIL_HOST`: endereço de e-mail remetente e usuário SMTP
- `EMAIL_PASSWORD`: senha de aplicativo do remetente (não a senha normal da conta)
- `EMAIL_SMTP_HOST`: servidor SMTP (padrão `smtp.gmail.com`)
- `EMAIL_SMTP_PORT`: porta SMTP STARTTLS (padrão `587`)

Com Gmail, crie uma senha de aplicativo e mantenha essas credenciais apenas no Servidor. O Cliente não recebe nem carrega as configurações de e-mail.
As mesmas configurações SMTP são usadas pelo botão **Enviar lembrete** na tela de empréstimos; o Servidor envia a mensagem diretamente ao endereço de contato registrado no empréstimo.

### Cadastro de contas e chave AES

Não é necessária uma conta ADMIN pré-criada nem credenciais `ADMIN_USERNAME` ou `ADMIN_INITIAL_PASSWORD`. Na primeira inicialização, o Servidor cria a chave AES e aguarda conexões. No Cliente, escolha **Cadastrar-se** e confirme o código enviado ao e-mail; a conta será criada no banco e poderá ser usada para entrar. As contas cadastradas têm permissões administrativas para operar a biblioteca.

A chave AES fica fora dos JARs em `%USERPROFILE%\.conecta-livros\admin.key` (ou no caminho configurado em `ADMIN_AES_KEY_FILE`). Ela cifra as senhas das contas. Proteja e faça backup seguro da chave: se ela for perdida, as senhas já gravadas não poderão ser validadas.

O Servidor e o Cliente usam a porta padrão `3305`. Se `SERVER_PORT` estiver definido, configure o mesmo valor nos dois processos.

Após cinco tentativas inválidas de login, novas tentativas ficam bloqueadas por cinco minutos. Conexões inativas comuns são encerradas após 30 segundos; durante a confirmação do cadastro, a conexão é mantida até o código expirar (10 minutos).

### Endereço do Servidor

O Cliente lê `SERVER_HOST` e `SERVER_PORT` de variável de ambiente, propriedade Java ou `config.properties` (padrão: `127.0.0.1:3305`). O Servidor lê `SERVER_BIND_ADDRESS` e `SERVER_PORT` (padrão: `127.0.0.1:3305`). Para uma rede local, configure explicitamente o endereço de escuta e o host do Cliente. O protocolo TCP não usa TLS nesta fase; não exponha a porta em redes não confiáveis.

## Integração com Google Books API

A busca por livro por ISBN consulta a Open Library e também pode usar a API do Google Books quando a chave estiver configurada com a variável:

- `GOOGLE_BOOKS_API_KEY`

ou pela propriedade Java:

- `google.books.apiKey`

A busca via Open Library funciona sem chave.

## Compilar e executar

### Maven e dois JARs

O Maven usa o driver MySQL a partir de
`lib/mysql-connector-j-8.4.0.jar`. Para compilar, testar e empacotar:

```powershell
mvn package
```

Os dois artefatos são `target/conecta-livros-1.0.0-Cliente.jar` e `target/conecta-livros-1.0.0-Servidor.jar`. O Cliente não contém JDBC nem o driver MySQL. O Servidor usa o conector em `target/dependency`; deixe essa pasta junto aos JARs. Execute o Servidor primeiro e depois o Cliente:

```powershell
java -jar target\conecta-livros-1.0.0-Servidor.jar
java -jar target\conecta-livros-1.0.0-Cliente.jar
```

Execute `Schema.sql` somente para criar uma instalação nova. Esse script inclui as tabelas de conta ADMIN, auditoria e os indicadores de inativação lógica. Ele não migra nem modifica um banco que já exista.

## Resumo da arquitetura

O projeto é uma aplicação desktop Java Swing com persistência em MySQL, organizada em quatro blocos principais:

- `model/`: entidades e regras de negócio
- `dao/`: fachadas remotas no Cliente
- `server/ServerDAO.java`: acesso ao MySQL/JDBC no Servidor
- `util/`: infraestrutura, cache, tradução e integrações
- `view/`: interface gráfica e navegação entre telas

Esse padrão torna a aplicação simples de manter, facilita a extensão de novas telas e mantém o código de acesso ao banco separado da camada visual.
