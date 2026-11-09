# MELHORIAS — CONECTA-LIVROS

## Objetivo
Ampliar o sistema desktop de biblioteca utilizando arquitetura Cliente-Servidor com Java Sockets e Threads, uma única conta e instância de Cliente com perfil ADMIN, gestão de acervo e empréstimos para leitores cadastrados, listas escolares administradas, notificações assíncronas, integrações de livros e funcionamento resiliente quando serviços externos estiverem indisponíveis.

A entrega principal terá dois artefatos executáveis: **`Cliente.jar`** e **`Servidor.jar`**. Há uma única conta ADMIN, usada pela única instância Cliente.jar autorizada a operar com o Servidor. Leitores/tomadores são registros administrados pelo ADMIN e não possuem contas nem acesso ao Cliente. O Servidor autentica o ADMIN e autoriza cada operação. O Servidor e o MySQL rodam localmente ou na rede local. “Offline” significa continuar as tarefas que não dependem de serviços externos quando o Servidor e o banco estiverem disponíveis; gravação offline de empréstimos e sincronização posterior não está incluída sem uma fila persistente e regras próprias de conflito.

O escopo atual representa uma única biblioteca por instalação. Não há cadastro ou seleção de múltiplas bibliotecas.


## 1. Conta e permissões

### ADMIN — Administrador / Bibliotecário (única conta do sistema)

- Obras e exemplares: Cadastrar uma obra pelo ISBN. Se houver conexão com a 
  internet, o Servidor consulta Google Books e/ou Open Library para retornar metadados
  (título, autores, editora, sinopse e capa) e registrar os exemplares físicos 
  (BookCopy).
- Gestão administrativa: Realizar operações de CRUD de leitores/tomadores
  (registros sem credenciais de acesso), obras, exemplares, bibliotecas,
  categorias e listas escolares.
- Exclusão Segura: Aplicar soft delete (inativação por flag lógica no MySQL) para 
  registros que possuam histórico transacional atrelado, sinalizando visualmente 
  na tabela do Cliente (JTable). Exclusão física fica restrita a manutenção excepcional
  e não pode apagar histórico pela operação administrativa normal.
- Empréstimos: Registrar diretamente no balcão empréstimos, reservas e
  devoluções para leitores cadastrados, validar disponibilidade e regras e
  manter eventual fila de espera no Servidor. Não há solicitação nem aprovação
  feita por contas de leitores. A retirada física é parte do fluxo atual;
  empréstimo de e-book somente será implementado após definir licença,
  arquivo/conteúdo e regras específicas. Na tela Empréstimos, disponibilizar
  o download do histórico conforme o filtro ativo em arquivo CSV.
- Listas escolares: Criar e publicar coleções de leitura obrigatória associadas 
  a uma turma e período letivo; editar rascunhos, publicar e notificar a turma.
- Inadimplência: Consultar atrasos de leitores, disparar e-mails de cobrança
  manualmente ou por rotina automatizada assíncrona e aplicar bloqueios conforme
  política aprovada.
- Conta de acesso: Provisionar exatamente uma conta ADMIN fora do cadastro
  público, sem credencial padrão no código. O Cliente não cria outras contas,
  papéis ou sessões concorrentes. O ADMIN pode trocar sua própria senha por um
  fluxo seguro, mas nunca consultar a senha armazenada.
- Auditoria: Registrar ator, ação, recurso, data/hora, resultado e motivo quando
  aplicável em alterações sensíveis, sem armazenar senha, token ou dados excessivos.

### Leitores/tomadores (sem conta de acesso)

- O ADMIN mantém os dados necessários para identificar leitores e associá-los a
  empréstimos, devoluções, reservas e notificações.
- Leitores não fazem login, não acessam o Cliente, não mantêm perfil/listas
  pessoais e não enviam solicitações pelo sistema. O ADMIN realiza essas ações
  em nome do leitor quando aplicável.
- Dados de leitores são acessados e exportados apenas para tarefas administrativas
  necessárias, com auditoria das consultas sensíveis.

### Regras de autenticação e autorização
- A interface pode ocultar botões, mas somente o Servidor autoriza e executa
  operações protegidas. Cada requisição verifica a sessão da única conta ADMIN,
  o estado da conta e o estado atual do recurso.
- O Servidor obtém o ID do ator da sessão autenticada; não confia em `userId`,
  `role` ou identidade do leitor enviados pelo Cliente. O leitor beneficiado é
  um registro de negócio distinto do ADMIN que opera o balcão.
- Não há papéis USER, cadastro público, autorização por propriedade de conta ou
  acesso de leitores ao catálogo/histórico pelo Cliente.


## 2. Autenticação e autorização (Arquitetura Cliente-Servidor com Sockets)
- Executar uma única instância de `Cliente.jar`, autenticada pela única conta
  ADMIN, e `Servidor.jar` em máquina local ou da rede local. Apenas o Servidor
  acessa o MySQL.
- Proteger todas as operações no backend; o Servidor só aceita ações da sessão
  ADMIN ativa e considera o estado da conta e da operação.
- O Cliente e o Servidor comunicam-se por TCP/IP usando `Socket` e
  `ServerSocket`, com o endereço IP e a porta do Servidor definidos na
  configuração do Cliente. Combinar entre os dois lados um formato simples de
  mensagem; não é necessário criar protocolo binário versionado nem uma pasta
  `network`. Não desserializar objetos Java arbitrários recebidos da rede.
- **Armazenamento de senha com AES:** o Servidor armazena a senha cifrada com
  AES-256-GCM, sem gravar hash ou derivação de senha em paralelo. AES é cifragem,
  não hash: usa nonce único, tag de autenticação e formato versionado.
  A chave fica fora do código, banco, Git e dos dois JARs, em configuração local
  protegida. Somente o Servidor decifra durante a autenticação; senha e
  ciphertext nunca são exibidos ao ADMIN nem enviados de volta ao Cliente.
  Cifragem reversível de senha é uma escolha específica deste projeto, não uma
  recomendação geral de segurança.
- **Tráfego:** o Socket simples não implementa TLS. Não transmitir credenciais
  por rede não confiável sem proteção adequada; nunca registrar senha, chave,
  nonce, tag ou ciphertext em logs.
- O Servidor mantém uma única sessão Cliente ativa. Usar Threads ou executor
  limitado para tarefas internas, notificações e integrações, com limites de
  recursos e tratamento de desconexão; uma nova sessão ADMIN só pode iniciar
  após o encerramento/revogação da anterior.
- O Servidor valida credenciais e estado da conta via serviço de autenticação,
  mantém a sessão no backend e autoriza cada pedido com o ator da sessão.
- O Cliente não pode escolher ou forjar identidade/papel do ator; toda operação
  é associada à sessão ADMIN autenticada. O ADMIN só altera dados de leitores e
  outros registros de acordo com as regras e auditoria do Servidor.
- Aplicar limites de tentativas, expiração e revogação da sessão ADMIN. Não há
  cadastro de conta nem fluxo de verificação de conta de leitor.
- Credenciais MySQL, SMTP e chaves de API ficam apenas na configuração protegida
  do Servidor, fora do código, JARs, Git e logs.


## 3. Notificações por e-mail (Envio em Thread Separada)
O envio é responsabilidade exclusiva do Servidor. Jakarta Mail/JavaMail é uma
dependência externa, não uma API nativa do Java SE; ela deve ser incluída como
dependência aprovada do `Servidor.jar` para atender ao requisito obrigatório de
e-mail. Configurar SMTP por ambiente e executar tarefas em pool limitado/worker
do Servidor, sem bloquear a EDT, a thread que aceita sockets ou a transação que
confirmou a operação principal. A falha de e-mail não desfaz um empréstimo,
devolução ou publicação já confirmados.

- Empréstimo/devolução: Após o ADMIN registrar a operação para um leitor,
  informar por e-mail os detalhes do livro, o prazo de devolução e as normas
  aplicáveis, conforme os dados de contato disponíveis.
- Aviso de vencimento: Rotina diária do Servidor localiza empréstimos que vencem
  em dois dias, envia lembrete e registra o evento para impedir duplicidade.
- Atraso: Enviar cobrança conforme periodicidade e política de bloqueio aprovada;
  evitar envio diário ilimitado. Registrar tentativa/resultado e respeitar
  preferências e regras de contato definidas para o projeto.
- Nova lista escolar: Após publicação confirmada pelo ADMIN, notificar os
  destinatários cadastrados para a turma/período correspondente. Registrar status
  por destinatário e permitir retentativa segura.
- Idempotência e recuperação: A fila de notificações deve sobreviver à reinicialização
  do Servidor ou tratar claramente eventos pendentes; reprocessar não duplica e-mail.
- Não enviar mensagens de verificação de conta. Testes usam SMTP de teste/local,
  como MailHog, sem enviar mensagens reais. Segredos SMTP ficam fora do código e
  dos JARs.


## 4. Funcionamento online e offline (Configuração por Arquivo Texto)
Configuração não secreta pode ser lida de arquivo texto local (por exemplo,
`config.properties`) usando APIs de arquivo do Java. O arquivo real de cada
máquina fica fora do Git e não é embutido nos JARs. Endereço e porta do Servidor
podem ser configurados no Cliente; acesso ao banco, chaves e SMTP ficam somente
na configuração protegida do Servidor. Separar `ambiente=dev/prod` do estado de
conectividade: “dev” não significa automaticamente “offline”.

**Limite do modo offline:** a queda da Internet pode desabilitar Google Books,
Open Library, capas remotas e SMTP sem impedir tarefas locais se `Servidor.jar`
e MySQL estiverem ativos. Se o Servidor ou MySQL estiver indisponível, novas
gravações não são aceitas por padrão. Operar offline e sincronizar depois
empréstimos/reservas exigiria fila local persistente e resolução de conflito,
fora do modo de fallback descrito aqui.


### Google Books API
- Modo conectado: O Servidor consulta Google Books por ISBN e pode consultar
  Open Library como fonte complementar/fallback, com timeout e limites
  configuráveis. Usar a API padrão Java para HTTP se bibliotecas externas não
  estiverem autorizadas.
- Sem rede, timeout, erro ou resultado vazio: O Servidor envia um estado de
  resposta diferente para falha, vazio e ISBN inválido. O Cliente preserva os
  dados digitados e permite preenchimento manual; a UI não bloqueia.
- Metadados: Exibir a origem externa quando útil e permitir correção manual antes
  de salvar. Resultado de API não substitui validação do ISBN, unicidade ou
  autorização do usuário.
- Gestão de capas pendentes: Se a capa não puder ser obtida, exibir imagem
  padrão e gravar `capa_sincronizada=false` somente se a obra foi persistida
  com sucesso no MySQL.
- Sincronização retomável: O Servidor processa capas pendentes em segundo plano,
  marca `PENDENTE`, `EM_PROCESSAMENTO`, `SINCRONIZADA` ou `FALHA`, atualiza a
  interface após confirmação e preserva capa/metadados válidos se uma tentativa
  posterior falhar. Definir tamanho, formato, armazenamento e política de cache.
- Segurança operacional: Não registrar chave de API nem colocar segredos no
  Cliente.jar. Respeitar políticas, limites de requisição e identificação do
  aplicativo exigidos pelos provedores.

### E-mail
- Produção: O Servidor usa SMTP configurado por segredo/ambiente; “localhost” só
  é válido quando há um serviço SMTP real instalado e configurado na máquina.
- Desenvolvimento: O Servidor aponta para SMTP de teste local (por exemplo,
  MailHog). Testes não enviam e-mails reais.
- Sem SMTP: Registrar estado pendente/falha e permitir retentativa controlada;
  não desfazer a ação de negócio confirmada no banco.


## 5. Testes Automatizados
JUnit 5 será usado para testar regras, concorrência e integrações. Testes
unitários não precisam de serviços externos; testes de integração usam banco
descartável/local isolado e servidor SMTP falso. O conjunto automatizado deve
rodar sem internet e nunca apontar para dados reais.

- Testes unitários: Validar domínio de livro/empréstimo, disponibilidade, fila,
  prazo, validações e máquina de estados; não incluir fluxos de contas de leitores,
  listas pessoais ou progresso por conta.
- Testes de autorização: Cobrir as ações permitidas à única conta ADMIN e estados
  da conta. Adulterar `userId`, `role` e identificadores no pedido; confirmar que
  a sessão é a única fonte da identidade do ator, que contas/roles adicionais não
  podem ser criados e que uma conta ADMIN inativa não opera.
- Testes de concorrência: Confirmar que apenas uma sessão Cliente é aceita;
  desconexão/expiração libera uma nova sessão; testar requisições duplicadas,
  disputa pelo último exemplar e recuperação da fila/notificações após reinício.
- Testes de integração: DAOs, transações e rollback em banco de teste.
  Nunca executar testes contra o banco real de usuários.
- Testes de AES-GCM para o campo de senha: round-trip exclusivamente dentro do
  Servidor; nonce único; rejeição de ciphertext/tag adulterado; erro controlado
  para chave ausente/inválida; confirmação de que o login usa a senha cifrada
  somente no Servidor e que não há hash/derivação de senha paralelo; chave ausente dos JARs,
  banco e logs.
- Simulação de serviços externos: Dublês para Google Books, Open Library e SMTP
  cobrindo sucesso, resultado vazio, ISBN inválido, falha, timeout, repetição e
  modo sem Internet.
- Testes de interface: Verificar atualizações na EDT, acessibilidade por teclado,
  foco, mensagens, idiomas, estados vazios, loading, cancelamento e confirmação.
- Testes de entrega: Gerar os dois JARs em ambiente limpo, confirmar entrada
  executável, ausência de credenciais no pacote e conexão da única instância
  Cliente ADMIN.


## 6. Tecnologias Utilizadas (Escopo Estrito da Ementa da Mauá)
- Java SE (JDK): Linguagem base dos dois aplicativos (Cliente e Servidor).
- Java Swing: Biblioteca para construção manual, componente a componente, das interfaces 
  do Cliente (sem uso de editores WYSIWYG).
- Java Sockets (java.net.Socket / java.net.ServerSocket): comunicação
  bidirecional Cliente-Servidor por TCP/IP, configurando IP/host e porta do
  Servidor no Cliente e usando mensagens simples acordadas entre ambos.
- Java Threads/Executors: Executar tarefas internas do Servidor com limites e sem
  bloquear o Cliente; não há requisito de atendimento a múltiplos clientes.
- MySQL/JDBC: Acesso exclusivo do Servidor; nunca conectar diretamente pelo Cliente.

**Implantação:** sem AWS; execução local ou em rede local. Um único Cliente ADMIN
acessa o Servidor e o banco centraliza os dados da biblioteca.

### Tecnologias previstas e dependências
- Google Books API — consulta de metadados por ISBN.
- Open Library Search/Covers APIs — busca complementar de metadados/capas,
  respeitando limites e políticas publicadas.
- JUnit 5 — testes automatizados.
- Jakarta Mail/JavaMail — dependência externa necessária às notificações
  obrigatórias; incluir no pacote do Servidor conforme o build aprovado.
- Senhas: armazenar somente a senha cifrada com AES-256-GCM no Servidor; não
  armazenar hash nem outra derivação em paralelo. AES é cifragem,
  não hash; documentar a decisão de usar cifragem reversível para autenticação.
- AES-GCM: implementar no Servidor, nunca no Cliente; usar nonce único, tag e
  formato versionado. A chave fica em configuração protegida externa aos JARs e
  ao banco, com plano de rotação. Não registrar senha, ciphertext ou chave.
- O Socket simples não implementa TLS; não enviar credenciais por rede não
  confiável sem proteção adequada. Não implementar criptografia própria.
- Dependências de execução ficam no JAR correspondente ou no pacote documentado:
  o Cliente não leva driver MySQL; o Servidor leva driver JDBC e bibliotecas
  de servidor aprovadas. O build deve gerar somente `Cliente.jar` e `Servidor.jar`
  como artefatos executáveis exigidos.

## 7. Requisitos gerais
- Servir uma única instância Cliente ADMIN e compartilhar estado persistente no
  MySQL; rejeitar sessões ADMIN concorrentes.
- Funcionar localmente ou em rede local por configuração de host/porta, com uma
  única estação Cliente autorizada.
- Manter os fluxos locais disponíveis quando APIs externas/e-mail falharem e o
  Servidor e MySQL estiverem ativos; não simular gravação offline com o banco fora.
- Preservar e adaptar a estrutura atual: reaproveitar as telas, CRUDs, DAOs e
  validações que já existem; mover o acesso JDBC e regras protegidas ao Servidor,
  sem reimplementar funções básicas já verificadas.
- Todos os itens deste documento, incluindo os 13 grupos de IHC, são obrigatórios.
- Configurações não secretas podem ser lidas de arquivo texto. Senhas, tokens,
  credenciais SMTP e chaves não ficam no código, nos JARs, no Git ou nos logs.

## 8. Aplicações IHC

As 13 melhorias abaixo são obrigatórias. As recomendações devem ser adaptadas aos
fluxos reais de biblioteca operados por uma única conta ADMIN em uma única
instância Cliente.
Onde o projeto já possui parte do recurso (por exemplo, placeholders, SwingWorker,
renderer zebrado, ISBN ou traduções), reutilizar e completar a cobertura em vez de
reimplementar a base.

### 1. Prevenção de erros e feedback de status

Usar ISO 9241-11 como referência para avaliar usabilidade no contexto de uso e
aplicar as heurísticas de prevenção de erros, visibilidade de estado e recuperação.

- **Validação em tempo real:** ISBN, ano, e-mail, telefone e campos obrigatórios
  devem indicar erro antes do envio, sem remover a validação definitiva no Servidor.
- **Mensagens de falha:** traduzir timeout, indisponibilidade do Servidor, falta
  de permissão, falha de banco, ISBN inválido e resultado vazio em mensagens que
  expliquem o problema e o próximo passo.
- **Segurança:** não exibir stack trace, SQL, segredo ou detalhe interno. Registrar
  diagnóstico técnico no log do Servidor, sem dados sensíveis.
- **Aceite:** falhas de rede/banco não congelam Swing; o texto informa se o pedido
  não foi enviado, foi recusado ou ainda aguarda confirmação do Servidor.

### 2. Organização visual, design minimalista e carga cognitiva

Organizar informação segundo a tarefa, limitar ruído visual e mostrar progresso
em vez de deixar a tela aparentemente travada.

- **Carregamento:** busca de ISBN, relatórios e consultas longas mostram progresso
  indeterminado ou mensagem de carregamento; o pedido ocorre em segundo plano.
- **Dashboard:** destacar métricas úteis à rotina (atrasos, empréstimos ativos,
  disponibilidade/capacidade verificadas); detalhes ficam em abas ou telas
  secundárias. Falha na consulta nunca aparece como zero.
- **Consistência visual:** agrupar controles relacionados, usar rótulos objetivos,
  hierarquia tipográfica e espaçamento consistentes e evitar telas sobrecarregadas.
- **Aceite:** o ADMIN consegue localizar a tarefa principal sem navegar por
  indicadores irrelevantes; consultas demoradas não bloqueiam a interface.

### 3. Consistência e compatibilidade com o mundo real

Seguir as heurísticas de consistência, padrões e correspondência entre sistema e
mundo real.

- **Status:** definir vocabulário único de estados de exemplar, solicitação e
  empréstimo. Exibir texto junto da cor semântica; não depender apenas de verde,
  amarelo ou vermelho, que também devem ser testados em escala de cinza.
- **Idiomas:** completar os bundles para todos os textos visíveis: botões,
  cabeçalhos de tabela, tooltips, placeholders, confirmações, estados vazios e erros.
- **Terminologia:** evitar misturar termos em inglês com o idioma escolhido;
  distinguir obra, exemplar, solicitação, reserva e empréstimo.
- **Aceite:** nenhuma tela crítica mistura idiomas; o estado permanece entendível
  sem cor; nomes de status são iguais no Cliente e nas respostas do Servidor.

### 4. Flexibilidade, atalhos e acessibilidade

Aplicar princípios de acessibilidade de software e critérios adaptados de WCAG
2.2 à interface Swing, sem declarar conformidade Web formal para uma aplicação
desktop.

- **Teclado:** busca rápida com Ctrl+F; setas para navegar resultados; Enter para
  selecionar; ordem de Tab lógica; atalho contextual que não captura texto quando
  o foco está em campo incompatível.
- **Foco e contraste:** foco visual claro em botões, campos, listas, tabelas e
  diálogos; contraste e tamanho legível; foco não depende apenas da cor.
- **Nomes e estados acessíveis:** componentes customizados devem expor rótulo,
  estado e propósito quando suportado; testar leitor de tela disponível no ambiente.
- **Aceite:** fluxos essenciais de busca, empréstimo e devolução podem ser concluídos
  com teclado e mouse; foco é sempre identificável.

### 5. Flexibilidade e eficiência para o operador ADMIN

Manter o aprendizado simples para iniciantes e reduzir passos para operadores
frequentes, sem comprometer confirmações ou validações.

- **Preenchimento por ISBN:** ao digitar ou ler código de barras, buscar metadados
  em segundo plano e preencher apenas campos ainda não alterados pelo operador.
  Busca automática não sobrescreve edição manual nem impede salvar sem Internet.
- **Fluxo de balcão:** permitir localizar leitor, localizar exemplar, adicionar ao
  carrinho e confirmar por teclado (Enter/Ctrl+S conforme contexto); mostrar resumo
  e confirmação antes de criar empréstimo.
- **Aceite:** fluxo respeita disponibilidade e permissão validadas pelo Servidor;
  repetir a requisição não cria empréstimo duplicado.

### 6. Estados vazios e tratamento eficaz de erros

Aplicar princípios de diálogo da ISO 9241-110, incluindo adequação à tarefa,
autoexplicabilidade e tolerância a erros.

- **Estados vazios:** em catálogo, estantes, listas, histórico e resultados de
  busca, explicar que não há registros e oferecer ação adequada quando permitida.
- **Estados distintos:** diferenciar “sem dados”, “falha ao carregar”, “sem
  conexão”, “sem permissão” e “carregando”; erro não pode parecer tabela vazia.
- **Erros técnicos:** converter exceções de rede/banco em texto útil sem stack
  trace; oferecer tentar novamente quando a operação puder ser repetida com segurança.
- **Aceite:** o usuário sabe por que não há conteúdo e o que pode fazer; o Cliente
  não afirma falha ou sucesso quando a confirmação do Servidor é desconhecida.

### 7. Reconhecimento em vez de memorização e cache

Reduzir a necessidade de lembrar dados entre telas, mantendo informações de
leitores corretamente associadas aos respectivos registros.

- **Capas no histórico:** mostrar miniatura da obra ao lado do título; disponibilizar
  alternativa textual e fallback local quando a imagem não carregar.
- **Cache:** reutilizar `DadosCache` para dados de catálogo não sensíveis, com
  validade/invalidação após mudanças; não cachear permissões nem decisões do Servidor.
- **Isolamento:** cache e dados de sessão são associados à única conta ADMIN e
  limpos ao encerrar a sessão. Não há cache ou dados privados de contas de leitores.
- **Aceite:** atualização aparece sem reiniciar o Cliente; cache expirado não
  mostra disponibilidade ou autorização obsoleta como se fosse verdade atual.

### 8. Controle de diálogo, cancelamento e ações destrutivas

Dar controle ao usuário sobre tarefas demoradas e deixar claro quando a operação
já foi recebida ou confirmada pelo Servidor.

- **Timeout e cancelamento:** configurar conexão/leitura; oferecer cancelar
  pesquisa/relatório quando seguro. Cancelar a espera do Cliente não significa
  necessariamente cancelar uma transação já confirmada no Servidor.
- **Feedback:** distinguir “cancelado antes do envio”, “pedido enviado; confirmação
  pendente” e “pedido cancelado/recusado pelo Servidor”.
- **Confirmação:** confirmar exclusão lógica, publicação, devolução, limpeza de
  carrinho e outras ações destrutivas com texto localizado e item afetado.
- **Aceite:** cancelar consulta não derruba o Cliente; operação sensível sempre
  informa seu resultado real ou que ainda não foi confirmado.

### 9. Sinalização de sincronismo de capas e estado da rede

Exibir onde reside cada dado: o catálogo oficial fica no banco do Servidor; capa
externa pode estar pendente porque uma API não respondeu.

- **Estados de capa:** `PENDENTE`, `EM_PROCESSAMENTO`, `SINCRONIZADA` e `FALHA`,
  com imagem padrão e rótulo acessível.
- **Atualização:** o Servidor processa capas em segundo plano e retorna a
  atualização ao Cliente ADMIN; preservar metadados e capa válidos em falha futura.
- **Sem rede externa:** cadastro manual persiste normalmente no MySQL se Servidor
  e banco estão disponíveis; gravação offline no Cliente não faz parte do escopo
  sem fila local e resolução de conflitos.
- **Aceite:** estado visual corresponde ao banco do Servidor e não sugere que um
  dado foi sincronizado quando ainda está pendente.

### 10. Redução da fadiga visual e densidade de informação

- **Tabelas:** aplicar renderer zebrado consistente com contraste; alinhar texto
  à esquerda, IDs/ISBN conforme legibilidade e valores numéricos à direita.
  Manter ordenação, seleção e foco acessíveis.
- **Abas:** agrupar conteúdo distinto no Dashboard via `JTabbedPane` somente quando
  cada aba representar tarefa clara (por exemplo, visão geral e alertas de atraso).
- **Aceite:** status e colunas continuam legíveis em escala de cinza, com teclado
  e em diferentes idiomas; abas não escondem alertas prioritários.

### 11. Aprendizado e autoexplicabilidade

- **Tooltips:** controles críticos têm dicas curtas, localizadas via `ResourceBundle`.
  Tooltip complementa rótulo/instrução e nunca é o único canal para regra essencial.
- **Placeholders:** usar exemplos de formato em campos; texto desaparece ao digitar
  e nunca é interpretado/salvo como valor.
- **Aceite:** usuário novo consegue identificar finalidade e formato de controles
  principais; todas as chaves existem nos idiomas mantidos.

### 12. Histórico de versão, logs e auditoria

Separar notas de versão, diagnóstico técnico e auditoria de ações:

- **Novidades:** tela de ajuda apresenta arquivo versionado de release notes
  (por exemplo, `CHANGELOG.md`), lido de caminho conhecido e somente para leitura.
- **Log técnico:** fica no Servidor, com rotação e sem senhas, tokens, conteúdo de
  e-mail ou dados pessoais desnecessários. Não expor arquivos de log arbitrários
  pela interface.
- **Auditoria:** ações sensíveis registram ator, ação, entidade, data/hora,
  resultado e motivo quando aplicável; consulta/exportação segue permissão ADMIN.
- **Aceite:** notas ausentes têm fallback amigável; auditoria não pode ser alterada
  ou apagada pela interface comum; log ajuda suporte sem vazar segredo.

### 13. Senha mascarada e indicador de força

Proteger senha com AES-256-GCM no banco, sem confundir mascaramento visual,
cifragem em trânsito e armazenamento persistente.

- **Interface:** usar `JPasswordField` no login e na troca de senha ADMIN;
  mascarar por padrão. Indicador de força pode ser educativo e calculado
  localmente enquanto digita, sem registrar, enviar ou imprimir a senha.
- **Servidor:** ao provisionar ou trocar a senha ADMIN, cifrar e armazenar a
  senha usando AES-256-GCM.
  Durante o login, somente o Servidor decifra a senha armazenada para validá-la.
  Não armazenar hash nem qualquer outra derivação em paralelo.
- **Uso de AES:** `CryptoAES`/AES-256-GCM fica exclusivamente no Servidor. A
  chave fica fora do Cliente, banco, Git e JARs. Usar nonce único, tag de
  autenticação e formato versionado; não exibir nem devolver senha/ciphertext ao
  ADMIN ou Cliente. AES é cifragem reversível, não hash.
- **Proteção adicional:** AES na coluna do banco não protege a senha em trânsito.
  O Socket simples não implementa TLS; não transmitir credenciais em rede não
  confiável sem proteção adequada.
- **Sessões:** troca/reset de senha segue fluxo próprio e invalida ou atualiza
  sessões conforme a política aprovada.
- **Aceite:** senha nunca aparece em texto puro em banco, logs, erros ou arquivos;
  o banco guarda somente ciphertext AES-GCM e metadados; somente o Servidor,
  usando a chave externa, pode decifrá-lo para autenticar. Não existe hash ou
  derivação paralelo, e a escolha de cifragem reversível fica documentada.

## 9. Arquitetura e entrega dos dois JARs

### 9.1 Distribuição de responsabilidades

| Componente | Responsabilidades | Não deve conter |
|---|---|---|
| `Cliente.jar` | Interface Swing exclusiva do ADMIN; validações visuais; mensagens simples por Socket com IP/host e porta configuráveis; chamadas Socket fora da EDT; idiomas e preferências locais | DAOs JDBC, driver MySQL, senha do banco, segredo SMTP ou autoridade final sobre autenticação/autorização |
| `Servidor.jar` | Socket de escuta em IP/porta configurados; uma sessão ADMIN; autenticação; autorização ADMIN; regras de negócio; transações; DAOs/JDBC; MySQL; AES-GCM e gerenciamento da chave; auditoria; tarefas de e-mail e integração com APIs | Senhas em texto puro, chave AES/credenciais embutidas no código ou operação confiada apenas à UI |

Somente o ADMIN usa o `Cliente.jar`; leitores não têm login nem acesso ao
aplicativo. O Servidor autentica a única conta e valida cada requisição. O
requisito de entrega é um JAR de Cliente e um JAR de Servidor, com apenas uma
instância Cliente ADMIN ativa por vez.

#### Responsabilidades exclusivas do Servidor

- **Rede:** abrir `ServerSocket` no IP e porta configurados, aceitar uma única
  sessão Cliente ADMIN ativa, trocar mensagens simples e encerrar conexões.
- **Identidade e segurança:** autenticar a conta ADMIN única; autorizar cada
  pedido; verificar estado da conta; impedir sessão concorrente e invalidar a
  sessão quando necessário.
- **Negócio:** ser a autoridade para catálogo, exemplares, empréstimos,
  disponibilidade, reservas, fila, atrasos, listas escolares administradas,
  exportações e auditoria.
- **Persistência:** manter DAOs/JDBC, driver, o `Schema.sql` inicial, transações, conexão e
  credenciais do MySQL. Nenhum Cliente usa JDBC para ler ou gravar dados.
- **Senha e AES:** conter `CryptoAES`/AES-256-GCM para cifrar e decifrar a senha
  somente no Servidor, com chave protegida externa aos JARs; não armazenar hash
  hash nem outra derivação. Nunca devolver a chave, senha ou ciphertext em
  respostas ao Cliente. O Socket simples não protege senha em trânsito; não
  utilizar em rede não confiável sem proteção adequada.
- **Integrações e rotinas:** chamar Google Books/Open Library, obter/sincronizar
  capas, conectar ao SMTP, enviar e-mails e executar agendamentos/retentativas.
- **Operação:** ler configuração segura, escrever logs técnicos sem segredos,
  executar health check, gerenciar encerramento e registrar auditoria.

#### Responsabilidades do Cliente ADMIN

- Apresentar a interface Swing exclusiva do ADMIN autenticado.
- Coletar dados, fazer validações visuais não autoritativas e comunicar-se com
  o Servidor por Socket, usando IP/host e porta configuráveis.
- Apresentar loading, sucesso, erro, vazio, status de rede/capa e confirmação.
- Manter idioma e preferências locais não sensíveis; não manter dados oficiais,
  senhas do banco, chave AES, DAOs JDBC nem lógica de autorização confiável.

### 9.2 Comportamento com cliente único

- Uma única instância do Cliente, autenticada como ADMIN, pode manter sessão ativa
  no Servidor; novas conexões são recusadas enquanto essa sessão estiver válida.
- O Servidor define timeout, encerramento, limites de recursos e recuperação de
  sessão após queda de conexão. Threads continuam sendo usadas para tarefas
  internas sem abrir acesso a outras contas Cliente.
- Operações concorrentes de reserva/empréstimo decorrentes de tarefas internas
  são serializadas/transacionadas onde necessário; nunca emprestar duas vezes o
  último exemplar.
- Mudanças confirmadas pelo Servidor são a fonte oficial e aparecem na única
  interface Cliente por resposta ou consulta posterior.

### 9.3 Estado já implementado a reaproveitar

O levantamento anterior do código indicou interface Swing em camadas, DAOs
JDBC, CRUD básico de livros/exemplares/estantes/gêneros/editoras, listas escolares
básicas, empréstimo e devolução de balcão, ISBN e busca de metadados, uso parcial
de `SwingWorker`, cache de catálogos, placeholders, renderer zebrado em algumas
tabelas, idiomas e testes existentes. Reaproveitar e adaptar essas partes ao
Servidor; não recomeçar cada tela do zero. A execução atual dos testes deve ser
verificada antes de alegar aprovação.

O código revisado anteriormente ainda não demonstrava uma camada Socket
separada, autenticação/autorização ADMIN do lado do Servidor, sessão única,
filas/reservas persistentes, ou serviço de envio de e-mail completo.
Esses itens continuam como trabalho novo. A conexão MySQL deve sair do Cliente;
qualquer credencial padrão embutida deve ser removida sem divulgar seu valor.

### 9.4 Conteúdo e execução

- Gerar dois executáveis distintos, com ponto de entrada e comando de execução
  documentados: `Cliente.jar` e `Servidor.jar`.
- Distribuir o `Schema.sql` inicial, configuração de exemplo e manual como arquivos
  auxiliares; nunca incluir segredos reais.
- Iniciar/configurar o Servidor e MySQL primeiro; depois abrir uma única
  instância Cliente ADMIN com host e porta corretos.
- Testar os JARs fora da IDE e em máquina limpa. Confirmar dependências permitidas
  e que o JAR do Cliente não carrega driver nem configuração de banco.
- O projeto roda localmente ou em LAN; não depende de AWS.

## 10. Plano de implementação faseado

**P0** bloqueia segurança/fluxo essencial; **P1** é requisito operacional de alto
valor; **P2** é produtividade/acabamento. Todas as melhorias deste documento são
obrigatórias. A prioridade organiza a sequência, não permite remover itens.

### Fase 0 — Diagnóstico, regras e linha de base

- Confirmar versão dos fontes, dependências, esquema, módulos existentes e
  requisitos acadêmicos; executar e registrar os testes-base.
- Registrar uma única instância Cliente ADMIN e uma única conta ADMIN; definir
  estação autorizada, rede de implantação, responsável pelo Servidor e
  endereços/portas.
- Aprovar o provisionamento da conta ADMIN, estado da conta, política de senha,
  empréstimo físico/digital, cadastro de leitores sem login, prazo, renovação,
  atraso, reserva/fila, turma/período e CSV.
- Registrar que o banco guardará a senha cifrada com AES-256-GCM, sem hash
  paralelo; definir provisionamento/rotação da chave do Servidor e documentar
  que AES é cifragem reversível, não hash.
- Definir idiomas obrigatórios, SMTP de teste, limites/timeouts para Google Books
  e Open Library e quais dados de capa serão guardados (URL/arquivo/BLOB).
- Definir o que “offline” significa: fallback externo funciona se Servidor e
  MySQL estiverem ativos; sincronização offline de operações transacionais é
  trabalho separado e não presumido.
- **Saída:** backlog de todas as melhorias, mapa de dependências, decisões
  registradas, plano de instalação/backup e critérios de aceite.

### Fase 1 — Esqueleto Cliente-Servidor e dois JARs

- Criar os pontos de entrada independentes e build do Cliente.jar e Servidor.jar.
- Configurar IP (`ADDRESS`) e porta (`PORT`) do Servidor no Cliente; trocar
  mensagens simples por TCP/IP usando `Socket`/`ServerSocket`, sem protocolo
  binário versionado ou pasta `network`. O Servidor deve vincular o
  `ServerSocket` ao endereço configurado; `new ServerSocket(PORT)` sozinho
  escuta em todas as interfaces disponíveis.
- Criar servidor Socket que aceite uma única sessão Cliente ADMIN ativa, com
  limites de recurso, estado de saúde, logs sem segredo e encerramento controlado.
- Criar cliente Swing com configuração de host/porta e I/O fora da EDT.
- **Aceite:** a única instância Cliente ADMIN conecta ao Servidor; uma segunda
  sessão é recusada; desconexão permite reconexão segura; mensagens inválidas e
  timeouts são tratados.

### Fase 2 — Persistência segura, contas e autorização

- Mover acesso JDBC/DAOs/driver para o Servidor; remover credencial embutida e
  usar configuração local protegida.
- Atualizar somente `Schema.sql` para uma instalação nova. Não criar nem executar
  migrations e não alterar bancos existentes; qualquer adoção em banco com dados
  depende de backup e adaptação manual aprovada pelo responsável.
- Criar/provisionar a única conta ADMIN, estado de conta e sessão; implementar
  autenticação, logout, expiração, revogação e limite de tentativas. Não criar
  cadastro público, papéis USER ou contas de acesso para leitores.
- Armazenar a senha cifrada com AES-256-GCM para validação do login; não guardar
  hash nem outra derivação em paralelo. Limitar cifra/decifra ao
  Servidor, proteger a chave fora dos artefatos, versionar o formato e prever
  rotação. Não persistir a chave junto do ciphertext nem disponibilizar
  senha/ciphertext em telas ou respostas.
- Registrar no README/documentação que armazenar senha reversivelmente é uma
  exceção acadêmica, não recomendação para uma implantação de produção.
- Implementar autorização ADMIN em cada operação e auditoria de ações sensíveis;
  leitores são registros de negócio e nunca atores de sessão.
- **Aceite:** somente a sessão ADMIN ativa executa operações; alterar `role`/ID
  no pedido não cria outra identidade nem eleva privilégio; o Servidor grava
  somente ciphertext AES-GCM, rejeita adulteração e mantém a chave fora dos
  JARs. O login decifra e valida a senha somente no Servidor; a senha nunca é
  exibida nem devolvida.

### Fase 3 — Catálogo e exemplares pelo Servidor

- Migrar consultas e CRUD de livros, exemplares, gêneros, editoras e estantes
  para serviços do Servidor. A biblioteca é única e implícita na instalação existente.
- Manter as telas Swing existentes quando possível; trocar JDBC direto por
  mensagens Socket no formato combinado entre Cliente e Servidor.
- Validar ISBN, duplicidade e disponibilidade; inativar logicamente registros
  com histórico; transacionar gravações relacionadas.
- **Aceite:** Cliente não conecta ao MySQL; ADMIN consulta e altera catálogo;
  histórico permanece após inativação; não há gestão multi-biblioteca.

### Fase 4 — Reservas, empréstimos, devolução e fila persistente

- Integrar tomador com cadastro de leitor sem criar contas de acesso nem atribuir
  registros históricos antigos a pessoas sem validação.
- Implementar cadastro administrativo de reservas/fila e registro ADMIN de
  empréstimo, devolução, renovação se aprovada e bloqueio por atraso conforme
  política; leitores não fazem pedidos pelo Cliente.
- Implementar concorrência/atomicidade para o último exemplar e registrar
  o leitor beneficiado separadamente do operador ADMIN.
- **Aceite:** fila sobrevive a reinício; o último exemplar não gera empréstimo
  duplicado; apenas o ADMIN pode criar, alterar ou cancelar reservas.

### Fase 5 — Leitores, listas escolares e exportação

- Implementar edição administrativa dos dados de contato dos leitores, sem
  perfil de login, preferências privadas ou listas pessoais.
- Implementar listas escolares por turma/período sob controle ADMIN, com
  rascunho/publicação e notificações aos destinatários cadastrados.
- Disponibilizar na tela Empréstimos o download do histórico filtrado para CSV,
  incluindo as colunas e linhas atualmente exibidas, em UTF-8 e com escaping correto.
- **Aceite:** não há dados de perfil/listas privadas por conta; listas escolares,
  histórico e CSV são acessíveis somente pela sessão ADMIN.

### Fase 6 — E-mails e rotinas agendadas

- Implementar confirmação de empréstimo/devolução, lembrete de vencimento,
  cobrança conforme política e notificação de lista publicada; não implementar
  verificação de cadastro de leitor.
- Persistir estados/eventos e idempotência; retentativas não duplicam mensagem.
- Configurar SMTP falso/local para testes e SMTP real fora do código.
- **Aceite:** todas as rotinas de notificação são testáveis sem envio real; falha de e-mail
  não desfaz operação confirmada nem bloqueia o Servidor.

### Fase 7 — APIs, capas e resiliência

- Implementar Google Books e Open Library com timeout, limites, tratamento de
  resposta vazia/falha e cadastro manual.
- Persistir estados de sincronização de capa e retomar trabalho após retorno de
  Internet; preservar dados válidos.
- Revisar cache e isolamento de sessão.
- **Aceite:** sem Internet externa ainda é possível cadastrar manualmente com
  Servidor/MySQL ativos; falha não apaga metadados/capa; retries não duplicam.

### Fase 8 — Concluir os 13 grupos de IHC

- Aplicar todas as melhorias da seção 8 aos módulos reais, incluindo validação,
  mensagens, dashboard, status, idiomas, teclado, acessibilidade, fluxo rápido,
  estados vazios, capas/cache, cancelamento, sincronismo, tabelas, tooltips,
  placeholders, novidades, logs, auditoria e senha.
- Não deixar grupo como opcional por prioridade P1/P2; aproveitar somente as
  partes já implementadas e completar lacunas.
- **Aceite:** checklist de cada um dos 13 grupos assinado com evidência de teste
  visual/funcional nos idiomas para a única interface ADMIN.

### Fase 9 — Teste integrado e entrega acadêmica

- Executar testes unitários, autorização ADMIN, persistência, concorrência
  interna, sockets, integrações simuladas, UI, instalação limpa e roteiro ponta
  a ponta.
- Validar criação do esquema inicial e backup/restauração em cópia do banco.
- Gerar versões finais de `Cliente.jar` e `Servidor.jar`; testar fora da IDE com
  uma única instância Cliente ADMIN e rejeição de sessões concorrentes.
- Entregar manual de instalação/execução, configuração de exemplo sem segredo,
  `Schema.sql` e roteiro de demonstração.
- **Aceite final:** os dois JARs executam em ambiente limpo; somente a instância
  Cliente ADMIN autorizada opera; todas as melhorias obrigatórias têm evidência
  de conclusão; Cliente não acessa o banco diretamente e nenhum segredo está nos
  artefatos.

## 11. Critérios gerais de pronto

Uma melhoria só é considerada concluída quando:

- Regra de negócio e autorização são validadas no Servidor.
- Formato das mensagens Socket e alteração de banco estão documentados.
- Dados de bancos existentes não são alterados; somente o esquema inicial é entregue.
- Sucesso, vazio, falha, falta de permissão, timeout e cancelamento têm feedback
  explícito.
- Operação lenta não bloqueia EDT nem thread principal do Servidor.
- Há teste automatizado e evidência do fluxo integrado correspondente.
- Os textos aparecem nos bundles de todos os idiomas obrigatórios.
- Não há senha, token ou credencial em código, JAR, log ou relatório.
- Todos os requisitos deste plano foram implementados; não basta compilar
  ou apresentar telas sem backend autorizado e persistência.

## 12. Referências

As páginas de API são dinâmicas. Conferir novamente endpoints, limites, políticas
e condições de uso durante a implementação. Acesso às páginas: **27 out. 2026**.

### IHC, usabilidade e acessibilidade

1. **INTERNATIONAL ORGANIZATION FOR STANDARDIZATION (ISO).** *ISO 9241-11:2018 —
   Ergonomics of human-system interaction — Part 11: Usability: Definitions and
   concepts*. 2018. Disponível em:
   <https://www.iso.org/standard/63500.html>.
2. **INTERNATIONAL ORGANIZATION FOR STANDARDIZATION (ISO).** *ISO 9241-110:2020 —
   Ergonomics of human-system interaction — Part 110: Interaction principles*.
   2020. Disponível em: <https://www.iso.org/standard/75258.html>.
3. **INTERNATIONAL ORGANIZATION FOR STANDARDIZATION (ISO).** *ISO 9241-112:2025 —
   Ergonomics of human-system interaction — Part 112: Principles for the
   presentation of information*. 2025. Referência atual para apresentação visual
   da informação; ISO 9241-12:1998 foi retirada. Disponível em:
   <https://www.iso.org/standard/87518.html>.
4. **INTERNATIONAL ORGANIZATION FOR STANDARDIZATION (ISO).** *ISO 9241-171:2025 —
   Ergonomics of human-system interaction — Part 171: Software accessibility*.
   2025. Disponível em: <https://www.iso.org/standard/86308.html>.
5. **NIELSEN, Jakob.** *10 Usability Heuristics for User Interface Design*.
   Nielsen Norman Group, publicado originalmente em 24 abr. 1994; revisado em
   30 jan. 2024. Disponível em:
   <https://www.nngroup.com/articles/ten-usability-heuristics/>.
6. **WORLD WIDE WEB CONSORTIUM (W3C).** *Web Content Accessibility Guidelines
   (WCAG) 2.2*. W3C Recommendation, 12 dez. 2024. Orientação adaptada à interface
   Swing; não declarar conformidade Web formal para aplicativo desktop. Disponível
   em: <https://www.w3.org/TR/WCAG22/>.
7. **ORACLE.** *Concurrency in Swing — The Java Tutorials*. Disponível em:
   <https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html>.

### APIs e integração de livros/capas

8. **GOOGLE FOR DEVELOPERS.** *Google Books APIs — Volumes: list*. Referência da
   busca de volumes por termos/ISBN. Disponível em:
   <https://developers.google.com/books/docs/v1/reference/volumes/list>.
9. **OPEN LIBRARY.** *Search API*. Disponível em:
   <https://openlibrary.org/dev/docs/api/search>.
10. **OPEN LIBRARY.** *Covers API*. Disponível em:
    <https://openlibrary.org/dev/docs/api/covers>.
11. **OPEN LIBRARY.** *APIs — Usage Guidelines and Rate Limits*. Conferir
    identificação da aplicação e limites vigentes. Disponível em:
    <https://openlibrary.org/developers/api>.

### Cliente-Servidor e segurança

12. **ORACLE.** *Java SE 11 API — `ServerSocket`*. Disponível em:
    <https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/ServerSocket.html>.
13. **ORACLE.** *Java SE 11 API — `Socket`*. Disponível em:
    <https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/net/Socket.html>.
14. **OWASP FOUNDATION.** *Authorization Cheat Sheet*. Disponível em:
    <https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html>.
15. **OWASP FOUNDATION.** *Password Storage Cheat Sheet*. Disponível em:
    <https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html>.
16. **NATIONAL INSTITUTE OF STANDARDS AND TECHNOLOGY (NIST).** *SP 800-38D:
    Recommendation for Block Cipher Modes of Operation: Galois/Counter Mode
    (GCM) and GMAC*. 2007. Referência normativa para AES-GCM e autenticação do
    ciphertext. Disponível em: <https://csrc.nist.gov/pubs/sp/800/38/d/final>.
17. **ORACLE.** *Java SE 11 API — `GCMParameterSpec`*. Parâmetros para o modo
    GCM com `Cipher`. Disponível em:
    <https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/crypto/spec/GCMParameterSpec.html>.
18. **OWASP FOUNDATION.** *Cryptographic Storage Cheat Sheet*. Disponível em:
    <https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html>.

### Como aplicar as referências

- ISO 9241-11 apoia a avaliação de eficácia, eficiência e satisfação no contexto
  real de leitores e bibliotecários.
- ISO 9241-110, ISO 9241-112, ISO 9241-171 e Nielsen apoiam diálogo, prevenção
  de erros, apresentação, consistência, controle, feedback e acessibilidade.
- WCAG 2.2 oferece critérios de referência para teclado, foco, contraste e
  identificação de erros; adaptar e testar esses princípios em Swing.
- Google Books e Open Library fundamentam busca/capas; a resposta externa não é
  autoridade para sobrescrever um cadastro confirmado e falha nunca impede
  entrada manual.
- Oracle fundamenta concorrência Swing e Sockets; OWASP recomenda hash de senha
  e autorização por requisição no Servidor. Este projeto opta por cifrar a senha
  com AES-GCM conforme requisito definido, sem hash paralelo; por ser
  reversível, a decifragem fica restrita ao Servidor e o risco deve ser
  documentado.
  NIST SP 800-38D e Java `GCMParameterSpec` fundamentam o uso de AES-GCM.