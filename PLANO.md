# Conecta-Livros — Plano de Implementação Faseado
**Objetivo:** transformar a lista de melhorias em uma sequência executável, com dependências, entregas e critérios de aceite. O plano cobre as funcionalidades administrativas e de leitor, a arquitetura local cliente-servidor e os 13 grupos de melhorias de IHC descritos no material recebido.
## 1. Leitura da situação atual
O README descreve uma aplicação desktop Java Swing organizada em views, DAOs, models e utilitários, com acesso direto ao MySQL via JDBC. A lista de melhorias propõe migrar para Cliente-Servidor com Sockets, autenticação e permissões por perfil, operação local sem AWS, integrações externas opcionais e vários aprimoramentos de IHC. Portanto, a primeira entrega deve ser uma decisão e uma fundação técnica — não começar adicionando telas isoladas.
O ZIP também contém código de testes e relatórios Maven antigos com 24 testes sem falhas registradas. Isso é apenas uma referência do pacote recebido: a Fase 0 deve executar os testes novamente no código-fonte atual e estabelecer uma linha de base confiável. No inventário do pacote não aparece uma camada de servidor/socket claramente separada; confirmar isso na inspeção inicial antes de estimar a migração.
## 2. Decisões que evitam retrabalho
1. **Execução local:** servidor e MySQL rodam na máquina da biblioteca ou numa máquina da rede local. Não há dependência de AWS ou hospedagem externa. Documentar o cenário de uso: um computador ou vários clientes na mesma LAN.
2. **Significado de “offline”:** o sistema continua operando localmente enquanto o MySQL local e o servidor estiverem disponíveis; Google Books e e-mail podem ficar indisponíveis, com entrada manual e envio suspenso. Não prometer gravação offline com sincronização posterior de empréstimos enquanto o servidor/banco estiver fora do ar — isso seria outro recurso, com conflitos e fila persistente próprios.
3. **Segurança de senha:** senha não deve ser armazenada com criptografia reversível AES. Armazenar hash com salt e algoritmo de derivação apropriado disponível no JDK, como PBKDF2; AES fica reservado a dados que realmente precisem ser recuperados. A chave criptográfica nunca deve ser embutida no cliente.
4. **Autorização:** a interface pode ocultar ações, mas a validação obrigatória acontece no servidor em cada requisição. O cliente nunca define como confiáveis o próprio perfil, o ID do usuário autenticado ou o dono de um registro.
5. **Concorrência:** nenhum acesso a banco, socket, e-mail ou API externa deve bloquear a Event Dispatch Thread do Swing. Atualizações visuais retornam à EDT; cancelamento interrompe a operação sem encerrar a aplicação.
6. **Fila de espera:** persistir posição e estado no banco; uma lista em memória pode servir à execução, mas não pode ser a única fonte, pois reiniciar o servidor não deve apagar a fila.
7. **Configuração e segredos:** arquivo local de configuração pode escolher ambiente, timeouts e endpoints não secretos. Senhas de banco, chaves de API e SMTP ficam fora do repositório, em variáveis de ambiente ou configuração local ignorada pelo Git.
8. **Escopo a confirmar na Fase 0:** definir se “empréstimo digital” significa empréstimo de e-book ou apenas solicitação online para retirada física; definir regras de bloqueio por atraso, multas, prazo, turmas e período letivo. O modelo atual descrito no README é centrado em exemplares físicos.
## 3. Fases gerais e marcos de saída
| Fase | Foco | Entrega verificável | Só avançar quando… |
|---|---|---|---|
| **0 — Diagnóstico e desenho** | Inventário do que já existe, regras, decisões de arquitetura, banco e segurança | Backlog priorizado, diagrama Cliente → Servidor → MySQL, protocolo esboçado, plano de migração do esquema e testes-base | O cenário local, os papéis, a política de empréstimos e o significado de offline estiverem escritos e aprovados |
| **1 — Fundação segura** | Processo servidor, protocolo Socket, autenticação, autorização, configuração, erros e timeouts | Cliente conecta ao servidor local; login cria sessão no servidor; requisições inválidas são recusadas; operação de teste não trava a interface | Testes provarem autenticação, isolamento por usuário e bloqueio de ações administrativas no servidor |
| **2 — Fluxos essenciais** | Catálogo, exemplares, usuários, listas, empréstimos, fila, progresso e CSV | Fluxos prioritários de bibliotecário e leitor concluídos ponta a ponta no banco | Cada fluxo tiver validação, mensagens claras e testes de regra e autorização |
| **3 — Comunicação e notificações** | Verificação de e-mail, avisos, cobranças e listas escolares | E-mails de teste entregues ao SMTP local e agendamentos idempotentes | Falhas de SMTP não interromperem operações principais nem gerarem envio duplicado |
| **4 — Integrações e resiliência** | Google Books, fallback manual, capas pendentes, sinalização e sincronização | Cadastro manual funciona sem internet; capas pendentes são retomadas quando houver conexão | Indisponibilidade externa não apagar dados existentes nem bloquear o cadastro |
| **5 — IHC e produtividade** | Acessibilidade, atalhos, estados vazios, carregamento, tabelas, consistência e histórico de versão | Fluxos testados com mouse e teclado, feedback coerente nos idiomas suportados | Os critérios de aceite de cada melhoria abaixo forem cumpridos nas telas reais |
| **6 — Estabilização e entrega local** | Testes integrados, backup/migração, empacotamento, instalação e manual | Pacote reproduzível para máquina local, roteiro de instalação e restauração | Uma instalação limpa passar pelo roteiro completo sem depender da IDE |
**Prioridade:** P0 = bloqueia segurança ou fluxo essencial; P1 = valor operacional alto; P2 = produtividade e acabamento. O número da fase indica ordem de dependência, não prazo. Não atribuo semanas sem saber disponibilidade, tamanho da equipe e exigência de entrega acadêmica.
## 4. Plano por funcionalidade de negócio
### 4.1 Regras de usuário, administrador e contas — P0 — Fases 0–2
#### Princípio de autorização
Cada requisição protegida deve ser autorizada no servidor usando quatro informações: **identidade autenticada + papel/permissão + estado da conta + propriedade/estado do recurso**. A tela pode esconder botões, mas isso é apenas apresentação; não substitui a validação no servidor. O servidor obtém o ID do ator da sessão autenticada, nunca de um `userId` enviado pelo cliente.
#### Matriz de permissões
| Ação | USER (leitor/aluno) | ADMIN (bibliotecário) |
|---|---|---|
| Consultar catálogo e disponibilidade | Sim, conforme acesso à biblioteca | Sim |
| Cadastrar/editar/inativar obras, exemplares, editoras, gêneros, estantes e bibliotecas | Não | Sim |
| Ver ou editar perfil | Consultar e editar somente os campos permitidos da própria conta | Administrar contas para suporte operacional; não visualizar nem definir senhas |
| Alterar papel, estado ou permissões de uma conta | Não | Sim, exceto as proteções contra autoelevação e bloqueio do último administrador |
| Criar/editar listas pessoais | Somente listas próprias e privadas | Não altera lista pessoal em nome do leitor sem ação administrativa registrada |
| Criar/publicar lista escolar e associá-la a turma/período | Não | Sim |
| Consultar lista escolar obrigatória | Apenas listas publicadas associadas à própria turma/período | Sim, para as turmas que administra |
| Solicitar reserva ou entrar na fila | Somente em nome próprio e se a conta puder tomar empréstimos | Pode registrar atendimento de balcão para um leitor identificado |
| Aprovar solicitação, efetivar empréstimo, registrar devolução e tratar atraso | Não | Sim |
| Cancelar solicitação | Somente solicitação própria ainda pendente | Sim, com motivo registrado quando cancelar em nome do leitor |
| Consultar histórico de empréstimos | Somente o próprio histórico | Histórico necessário à operação da biblioteca; consultas e exportações administrativas são auditadas |
| Exportar histórico CSV | Não | Sim, respeitando filtros e minimizando dados pessoais exportados |
| Consultar progresso escolar | Somente o próprio progresso | Progresso das turmas que administra; preferir visão agregada quando o detalhe individual não for necessário |
| Consultar ou alterar preferências de recomendação | Somente as próprias preferências | Não altera preferências do leitor |
#### Regras do USER
1. O usuário só pode consultar dados públicos do catálogo e os próprios dados privados: perfil, preferências, listas privadas, solicitações, empréstimos e progresso escolar.
2. Pode alterar apenas uma lista explícita de campos de perfil, inicialmente nome de exibição, e-mail, telefone e preferências. Não pode alterar ID, papel, turma atribuída, estado da conta, data de verificação, bloqueio ou histórico.
3. A troca de e-mail inicia nova verificação; até a confirmação, o endereço novo não é tratado como verificado. Senha é alterada por fluxo próprio, nunca por edição de perfil comum.
4. Pode criar, editar e excluir as próprias listas pessoais. Não pode editar listas escolares. Solicitação pendente pode ser cancelada pelo próprio autor; solicitação aprovada ou convertida em empréstimo exige tratamento administrativo.
5. Não pode aprovar a própria solicitação, alterar datas/status de empréstimo, marcar devolução, editar exemplares, remover histórico ou consultar dados de outro leitor — mesmo que conheça IDs válidos.
6. Conta suspensa por atraso mantém acesso de leitura ao catálogo, perfil e histórico, mas não pode criar reserva, entrar na fila, renovar ou solicitar novo empréstimo. O desbloqueio segue a política da biblioteca, executada por ADMIN ou automaticamente quando as condições forem satisfeitas.
#### Regras do ADMIN
1. Pode administrar catálogo, exemplares, bibliotecas, listas escolares, contas e operações de empréstimo. Operações administrativas devem ser limitadas às tarefas necessárias à biblioteca e registradas em auditoria.
2. Pode criar, editar, suspender, reativar e inativar contas, além de atribuir papel conforme a política. Não pode consultar senhas, obter senha em texto puro nem registrar senha em log.
3. Não pode remover o próprio privilégio de administrador nem inativar/rebaixar o último administrador ativo. A primeira conta ADMIN deve ser provisionada por configuração local segura ou fluxo de instalação, sem credencial padrão embutida.
4. Ao agir em nome de um leitor no balcão, precisa selecionar o leitor real; o registro guarda separadamente o leitor beneficiado e o administrador operador. Não deve transformar a ação em empréstimo pessoal do operador.
5. Inativação de obra, exemplar ou conta é lógica quando houver histórico relacionado. Exclusão física só pode ocorrer em fluxo excepcional de manutenção, fora da operação comum e com política aprovada.
6. Alterações administrativas relevantes — papel/estado de conta, inativação, publicação de lista, aprovação/cancelamento de empréstimo, devolução, ajuste de prazo e exportação — registram ator, ação, recurso, horário e motivo quando aplicável. Auditoria não pode ser editada por USER nem apagada pela interface normal.
7. Administrador não deve usar acesso amplo para consultar dados pessoais sem finalidade operacional. Exportações devem conter somente campos necessários, filtros utilizados e registro de quem as gerou.
#### Estado da conta e regras comuns
- **PENDENTE_VERIFICACAO:** não pode iniciar sessão normal nem realizar operações protegidas; pode solicitar novo código dentro dos limites de envio.
- **ATIVA:** pode operar conforme papel, permissões e regras de empréstimo.
- **SUSPENSA:** autenticação pode permitir acesso limitado a catálogo, perfil e histórico; operações que aumentam compromissos (reserva, fila, renovação e novo empréstimo) são bloqueadas.
- **INATIVA:** não pode iniciar sessão; registros e histórico permanecem preservados.
- Papel e estado são dimensões separadas: trocar USER por ADMIN não deve ativar uma conta inativa ou não verificada. Toda mudança de estado precisa seguir a política definida e ser auditada.
- Falha de autenticação deve ter resposta genérica que não revele se um e-mail está cadastrado. Aplicar limite de tentativas e expiração/revogação de sessão; não guardar credenciais em mensagens de erro.
- Se funcionários também puderem emprestar livros, associar a conta administrativa a um cadastro de leitor separado ou explicitamente vinculado. Não conceder acesso de leitor a dados de terceiros como efeito colateral do papel ADMIN.
#### Preparação, implementação e aceite
- **Preparar (Fase 0):** confirmar campos editáveis, política de suspensão, duração de sessão, regras de empréstimo, escopo das turmas e se o ADMIN também pode atuar como leitor. Transformar a matriz acima em catálogo de permissões e migração versionada de usuários, papéis, turma e preferências.
- **Construir (Fases 1–2):** autenticação e autorização no servidor; sessão associada ao ator; verificação de propriedade em cada operação; bloqueio de conta aplicado no backend; auditoria de ações administrativas; mensagens e telas compatíveis com cada estado.
- **Aceite:** testes automatizados cobrem cada linha da matriz e tentativas negativas; alterar papel/ID no payload não aumenta acesso; USER não acessa dados de outro usuário; ADMIN não contorna estado da conta nem apaga histórico pela interface; conta não verificada não usa o sistema; senha não pode ser recuperada do banco.
### 4.2 Catálogo, exemplares e exclusão segura — P0 — Fases 1–2
- **Preparar:** separar “obra” de “exemplar”; confirmar unicidade de ISBN e código do exemplar; prever biblioteca/localização, categoria e status ativo.
- **Construir:** cadastro/edição de obra, exemplar, editora, gênero, estante e biblioteca; exclusão lógica quando houver histórico; transações JDBC para operações que alteram obra e exemplares juntos.
- **Aceite:** a interface identifica registros inativos sem apagá-los do histórico; tentativa duplicada ou dado inválido retorna orientação útil; o histórico de empréstimos continua íntegro após inativação.
### 4.3 Empréstimos, devoluções, solicitações e fila — P0 — Fases 1–2
- **Preparar:** fechar regras de prazo, disponibilidade, renovação, atraso, cancelamento e bloqueio; escolher identificador do usuário e do exemplar no balcão.
- **Construir:** solicitação de reserva; aprovação pelo ADMIN; conversão em empréstimo ativo; devolução; fila persistente para exemplares indisponíveis; prevenção de empréstimo simultâneo do mesmo exemplar.
- **Aceite:** em concorrência, um exemplar não pode ser emprestado duas vezes; a posição de fila sobrevive a reinício; USER cancela somente a própria solicitação pendente; ADMIN pode consultar ativos, atrasados e devolvidos.
### 4.4 Listas pessoais e listas escolares — P1 — Fase 2
- **Preparar:** definir privacidade, ciclo de vida e associação livro/lista/turma/período.
- **Construir:** USER cria e altera listas próprias, como “Quero Ler” e “Favoritos”; ADMIN cria, associa à turma e publica lista obrigatória; registrar leituras/devoluções consideradas para o progresso.
- **Aceite:** lista privada não aparece para outros usuários sem autorização; progresso corresponde à regra acordada e exibe numerador e denominador, além do percentual; alterações administrativas ficam registradas.
### 4.5 Preferências e recomendações locais — P2 — Fases 2 e 5
- **Preparar:** definir quais dados de preferência são opcionais e como o leitor pode removê-los.
- **Construir:** armazenar gêneros/autores preferidos e gerar recomendações locais, explicáveis e baseadas no catálogo disponível; sem serviço externo obrigatório.
- **Aceite:** recomendações mudam quando preferências mudam, nunca impedem consulta ao catálogo e têm fallback informativo quando não há dados suficientes.
### 4.6 Histórico e exportação CSV — P1 — Fase 2
- **Construir:** ADMIN exporta histórico respeitando filtros e intervalo; USER consulta somente o próprio histórico. Definir cabeçalho, codificação UTF-8 e escaping correto de vírgulas, aspas e quebras de linha.
- **Aceite:** arquivo abre corretamente em planilha comum; contém apenas o escopo autorizado e não inclui dados sensíveis desnecessários.
## 5. Plano faseado de cada melhoria de IHC
Para cada item, “construir” significa integrar a tela com as regras reais do sistema; não criar um componente isolado sem uso. Testar em português e verificar os demais idiomas suportados.
### 1. Prevenção de erros e feedback de status — P0/P1 — Fases 1, 2 e 5
- **Fase 1:** criar tratamento central de falhas de conexão, autorização, criptografia e validação, com mensagens ao usuário e detalhes técnicos apenas no log.
- **Fase 2:** validar ISBN, ano, e-mail, telefone e campos obrigatórios enquanto o usuário digita; destacar o campo inválido e explicar como corrigir.
- **Fase 5:** revisar textos de recuperação e estados de carregamento/erro em todas as telas.
- **Aceite:** formato inválido é identificado antes do envio; falha de banco não congela a tela; nenhuma exceção técnica ou segredo é mostrado no diálogo.
### 2. Dashboard enxuto e carregamento visível — P1 — Fases 2 e 5
- **Fase 2:** manter apenas indicadores necessários à tarefa diária: empréstimos ativos, atrasos e capacidade/disponibilidade do acervo, conforme existirem dados confiáveis.
- **Fase 5:** carregar dados em segundo plano; mostrar spinner ou progresso, estado de erro e opção de tentar novamente; mover relatórios secundários para abas ou telas próprias.
- **Aceite:** interação permanece responsiva durante consulta; cada indicador tem definição e origem claras; falha não é confundida com valor zero.
### 3. Status consistentes e internacionalização completa — P1 — Fases 2 e 5
- **Fase 2:** definir um vocabulário único de estados de exemplar e empréstimo e mapear estado para texto e cor semântica.
- **Fase 5:** localizar botões, colunas, diálogos, tooltips, placeholders, empty states e mensagens de erro em todos os bundles mantidos.
- **Aceite:** status não depende só da cor; nenhuma tela mistura idioma ativo com texto fixo de outro idioma; chaves ausentes são detectadas em teste.
### 4. Teclado e acessibilidade básica — P1 — Fase 5
- **Construir:** definir ordem de foco lógica; indicar foco com contraste; permitir busca por atalho, navegação de resultados por setas e seleção por Enter; verificar nomes acessíveis e tamanho/contraste dos controles críticos.
- **Aceite:** busca e seleção principais são concluídas sem mouse; foco é visível em cada componente interativo; atalho não intercepta entrada de texto em campo incompatível.
### 5. ISBN automático e fluxo rápido de empréstimo — P1 — Fases 2, 4 e 5
- **Fase 2:** operador pode buscar um usuário e selecionar exemplar por código; validar disponibilidade antes de confirmar.
- **Fase 4:** ao informar ISBN, solicitar metadados em segundo plano; preservar edição manual quando a busca falhar.
- **Fase 5:** implementar sequência de balcão por teclado (Enter entre campos e Ctrl+S para confirmar), com foco e atalhos contextualizados.
- **Aceite:** busca automática não bloqueia a janela nem sobrescreve campos que o operador já editou; fluxo de teclado respeita validações e pede confirmação antes de efetivar o empréstimo.
### 6. Estados vazios e erros com orientação — P1 — Fases 2 e 5
- **Fase 2:** distinguir “sem registros”, “falha ao carregar” e “sem permissão”.
- **Fase 5:** empty state contextual para estantes, listas, catálogo e histórico, com ação imediata quando apropriada.
- **Aceite:** o usuário entende por que não há conteúdo e qual é o próximo passo; erro de rede não aparece como tabela vazia.
### 7. Reconhecimento visual, capas e cache — P1/P2 — Fases 2, 4 e 5
- **Fase 2:** exibir miniatura local da capa no empréstimo/histórico, com alternativa textual se a imagem não carregar.
- **Fase 4:** guardar capa padrão e estado de sincronização; evitar apagar capa válida por falha externa.
- **Fase 5:** revisar DadosCache, definir validade e invalidação após cadastro, edição, devolução ou troca de idioma; não cachear decisões de autorização.
- **Aceite:** imagem ausente não quebra a tela; alterações aparecem sem dados obsoletos; cache não mistura dados de usuários diferentes.
### 8. Cancelamento, timeout e confirmação de ações destrutivas — P0/P1 — Fases 1, 2 e 5
- **Fase 1:** timeout configurável de conexão/leitura e tratamento de desconexão; operações com prazo limite não deixam a EDT bloqueada.
- **Fase 2:** definir quais operações são canceláveis e quais já foram confirmadas pelo servidor.
- **Fase 5:** oferecer cancelamento de busca/relatório em andamento e confirmação localizada para inativar registro ou limpar carrinho.
- **Aceite:** cancelamento não derruba o cliente; a interface diferencia “cancelado antes de enviar” de “resultado ainda não confirmado pelo servidor”; ação destrutiva informa o item afetado.
### 9. Sinalização de sincronismo de capas — P1 — Fase 4
- **Preparar:** definir estados PENDENTE, EM_PROCESSAMENTO, SINCRONIZADA e FALHA, com data/último erro resumido.
- **Construir:** mostrar badge e capa local no catálogo; atualizar a tela após confirmação do servidor; repetir apenas operações seguras.
- **Aceite:** o estado visível corresponde ao banco; uma falha de rede preserva capa e metadados existentes; repetição não duplica registros.
### 10. Leitura de tabelas e organização por abas — P1/P2 — Fase 5
- **Construir:** aplicar zebra com contraste acessível, alinhamento por tipo de dado, ordenação e larguras úteis; organizar dashboard em abas somente quando houver tarefas distintas e conteúdo suficiente.
- **Aceite:** dados críticos continuam identificáveis em escala de cinza; ordenação não altera nem corrompe os dados; abas têm rótulos objetivos e foco pelo teclado.
### 11. Tooltips e placeholders — P2 — Fase 5
- **Preparar:** escolher quais controles críticos precisam de explicação e escrever textos curtos por idioma.
- **Construir:** usar textos do ResourceBundle, exemplos de formato em campos e remoção/retorno do placeholder sem confundi-lo com valor gravado.
- **Aceite:** placeholder nunca é salvo como dado; tooltip não é a única forma de comunicar requisito essencial; chaves existem para os idiomas ativos.
### 12. Histórico de versão, logs e auditoria — P1/P2 — Fases 1, 2 e 5
- **Fase 1:** definir log técnico sem senhas, tokens, conteúdo de e-mail ou dados pessoais desnecessários; rotação e localização do arquivo.
- **Fase 2:** registrar auditoria mínima das ações sensíveis: ator, ação, entidade, data/hora e resultado, sem expor segredos.
- **Fase 5:** apresentar “Novidades / Histórico de versões” em uma tela de ajuda, lendo um arquivo de release notes versionado. Não misturar notas de versão com log técnico nem permitir leitura arbitrária de caminhos.
- **Aceite:** administrador consulta mudanças e eventos autorizados; logs ajudam a diagnosticar falhas sem armazenar credenciais ou senhas; arquivo de notas ausente tem fallback amigável.
### 13. Senha mascarada e indicador de força — P0/P1 — Fases 1 e 5
- **Fase 1:** implementar fluxo seguro de criação e verificação de senha no servidor, com hash salgado; não usar AES para armazenar senha.
- **Fase 5:** usar JPasswordField em login/cadastro; mostrar força como orientação local, sem transmitir senha enquanto o usuário digita; permitir revelar/ocultar opcionalmente se a política de acessibilidade aprovar.
- **Aceite:** caracteres são mascarados por padrão; força não é registrada em log; senha é validada e derivada somente no envio seguro; troca de senha invalida ou atualiza sessões conforme política definida.
## 6. E-mails e tarefas automáticas — P1 — Fase 3
1. **Verificação de cadastro:** gerar código de uso único, armazenar somente o necessário com expiração de 15 minutos, limitar tentativas e permitir reenviar com controle de frequência.
2. **Aprovação de empréstimo:** informar livro, vencimento e instruções; enviar depois que a transação do empréstimo for confirmada no banco.
3. **Aviso de vencimento:** job diário identifica empréstimos que vencem em dois dias e marca o aviso enviado para não duplicar.
4. **Atraso:** estabelecer periodicidade e política de bloqueio com o responsável; evitar e-mail repetitivo diário sem limite ou configuração.
5. **Nova lista escolar:** notificar alunos somente após publicação confirmada, registrar resultado por destinatário e permitir nova tentativa segura.
6. **Teste local:** ambiente de desenvolvimento aponta para SMTP de teste, como MailHog; produção local usa SMTP configurado fora do código. Falha de e-mail fica registrada e não desfaz empréstimo ou publicação já confirmada.
**Aceite da fase:** os cinco tipos de evento são testáveis sem enviar e-mail real; processamento é assíncrono e limitado por pool; eventos têm chave/idempotência para evitar duplicatas; a configuração SMTP não é commitada.
## 7. Testes e qualidade ao longo do plano
Testes não ficam para o fim. Cada fase amplia uma suíte que pode rodar sem serviços externos:
- **Unitários:** domínio de Livro e Empréstimo, ISBN, expiração do código, prazo e percentual de progresso escolar (inclusive lista vazia e progresso completo).
- **Autorização:** matriz USER/ADMIN × operações; tentativa de mudar ID/perfil no payload; usuário tentando acessar dado alheio.
- **Persistência:** DAOs, transações, exclusão lógica, concorrência na retirada do último exemplar e persistência da fila. Usar banco de teste local isolado ou camada substituível; nunca apontar teste para a base real.
- **Integrações falsas:** serviço de livros retorna sucesso, timeout e resultado vazio; SMTP registra mensagem sem enviá-la; teste offline não depende da internet.
- **Sockets:** conexão, protocolo inválido, timeout, desconexão, requisição duplicada e múltiplos clientes; não desserializar objetos Java arbitrários recebidos pela rede.
- **Interface:** campos, foco, atalhos, diálogos, empty states, idiomas e acessibilidade básica. Testar as atualizações Swing na EDT.
- **Aceite ponta a ponta:** cadastrar exemplar → reservar/emprestar → devolver → consultar histórico/CSV; publicar lista escolar → acompanhar progresso; queda da API externa → completar cadastro manual.
Os relatórios Maven incluídos no ZIP devem ser tratados como referência histórica. Na Fase 0, executar a suíte no código-fonte recebido, anotar resultado e corrigir ou classificar falhas antes de usar o número de testes como indicador.
## 8. Definição de pronto para cada item
Uma melhoria só sai do backlog quando:
- regra e permissão são verificadas no servidor, não apenas na interface;
- alterações de banco têm migração/documentação e preservam histórico;
- sucesso, vazio, falha, timeout e cancelamento têm estados visíveis adequados;
- operações lentas não bloqueiam a interface;
- textos estão nos bundles de idioma mantidos;
- há teste automatizado para a regra principal e autorização correspondente;
- o roteiro manual relevante foi executado em ambiente local limpo;
- configuração sensível está fora do código e dos logs.
## 9. Sequência recomendada de execução
1. Fazer Fase 0 e fechar decisões de escopo, sobretudo um ou vários clientes, política de empréstimo e o que offline significa.
2. Construir a Fase 1 antes de espalhar acesso de rede ou permissões pelas telas existentes.
3. Entregar primeiro o ciclo de catálogo + exemplar + empréstimo/devolução da Fase 2; depois listas escolares, listas pessoais, fila, progresso e CSV.
4. Acrescentar e-mails e integrações somente após os eventos principais estarem transacionais e testáveis.
5. Implementar sincronização de capas e cache sem prometer sincronização offline de operações de empréstimo.
6. Aplicar as melhorias de IHC nos fluxos já estáveis, priorizando feedback, acessibilidade por teclado e cancelamento.
7. Fechar com instalação local limpa, backup/restauração do banco e manual curto para operador.
**Resultado esperado:** cada melhoria passa a ter pré-requisitos, ordem, responsável técnico implícito no módulo correto e um teste objetivo de conclusão. A equipe pode executar fase a fase e demonstrar valor sem tentar entregar toda a lista em um único salto.
