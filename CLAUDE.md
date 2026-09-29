# CLAUDE.md — vetplanet-api

API do **VetPlanet** — produto de gestão para veterinários autônomos que
atendem a domicílio (clínica geral, cães e gatos).

> Repositório irmão: **`vetplanet-app`** (Next.js), o painel do produto e o
> único consumidor desta API. O site institucional da Dra. Rafaela é outro
> projeto (`rafaelasoares-site`), não fala com esta API e não deve passar a
> falar.
>
> A Dra. Rafaela é a **usuária-zero**: o produto é construído contra as
> necessidades reais dela. Ver a direção declarada no `CLAUDE.md` da
> pasta-mãe — o sistema é **em primeira pessoa** (uma agenda por assinante,
> nunca coluna por profissional), e isso continua valendo no cenário SaaS.
>
> Este arquivo é **autocontido de propósito**: num clone limpo deste repo, o
> `CLAUDE.md` da pasta-mãe e o `padrao-nomenclatura.md` — que fica no
> `vetplanet-docs`, pasta **local**, fora do GitHub por decisão — não existem.
> Tudo que é preciso para trabalhar aqui está abaixo.

## Estado atual

O domínio **`acesso` está pronto**: usuários do painel (CRUD com hash BCrypt),
sessão por token opaco, autorização por perfil e troca de senha — com 22
testes cobrindo entidade, controllers e o bootstrap do administrador
inicial. As migrações `V001` e `V002` criam o
schema `acesso` com as tabelas `usuario` e `token_autenticacao`.

Também já existem, transversais a todos os domínios: envelope único de erro
por categoria, id de correlação por requisição, **CI no GitHub Actions**,
**documentação OpenAPI** (dev) e **`Dockerfile`** com o bootstrap do
administrador inicial (ver "Deploy e o primeiro administrador").

**Ainda não existe**: nenhum outro domínio. O próximo trabalho é `cliente`
(tutores e animais), que é o que destrava o agendamento.

## Stack

| Item | Versão / escolha |
|---|---|
| Java | **21** (LTS) — mínimo exigido pelo Spring Boot 4 |
| Spring Boot | **4.1** |
| Build | Gradle (via wrapper — use `./gradlew`, não o gradle do sistema) |
| Banco | **PostgreSQL 17**, schema isolado por domínio |
| Migrações | **Flyway** — é o dono do schema |

## Como rodar

**Um comando só sobe o backend inteiro:**

```bash
./gradlew bootRun
```

Ele sobe o Postgres do `docker-compose.yml`, espera ficar saudável, aplica as
migrações e serve a API em `http://localhost:8080`. Quem faz isso é o
`spring-boot-docker-compose` (dependência `developmentOnly`, então não vai
para o jar de produção — lá o banco é externo).

```bash
./gradlew test            # testes (usam Testcontainers, não o compose)
./gradlew build           # compila + testa
docker compose down       # desliga o banco (mantém os dados)
docker compose down -v    # desliga e APAGA os dados
```

Com a aplicação de pé, a documentação da API fica em
**<http://localhost:8080/swagger-ui.html>** e a spec em `/v3/api-docs`.
Só existe no perfil `dev` — ver "Documentação da API (OpenAPI)".

## Integração contínua

`.github/workflows/ci.yml` roda `./gradlew build` a cada push na `main` e em
todo pull request. É **o mesmo comando** que se roda localmente, de propósito:
não deve existir a situação de "passa aqui e quebra lá".

Os testes usam Testcontainers, que sobe um Postgres real — o runner
`ubuntu-latest` já tem Docker, então não é preciso declarar `services:` nem
configurar banco no workflow. Quando o build quebra, o relatório de testes
sobe como artefato (`relatorio-de-testes`), que é o que diz *qual* teste
falhou sem precisar decifrar o log.

`lifecycle-management: start-only` faz o banco continuar de pé ao parar a
aplicação — sem isso, cada Ctrl+C derrubaria o contêiner e o próximo start
pagaria o tempo de subida de novo.

**O Postgres publica na porta 5433 do host**, não na 5432 — a 5432 já está
ocupada por outro projeto na máquina do dev. Dentro do contêiner continua
5432. Rodando pelo compose, o Spring descobre a conexão sozinho; os valores
em `application.yml` valem para produção, onde não há compose.

O **frontend não entra neste compose** — roda à parte com `npm run dev` no
`vetplanet-app`. Cada repositório sobe o que é seu.

## Arquitetura

**Monólito modular por domínio.** Um pacote Java por domínio, **todos dentro de
`com.vetplanet.modules`**, e cada domínio é dono do seu próprio schema Postgres:

| Domínio | Pacote | Schema | Responsabilidade |
|---|---|---|---|
| Acesso | `com.vetplanet.modules.acesso` | `acesso` | usuários do painel, autenticação |
| Cliente | `...modules.cliente` | `cliente` | tutores, animais, endereços |
| Agendamento | `...modules.agendamento` | `agendamento` | consultas, disponibilidade |
| Prontuário | `...modules.prontuario` | `prontuario` | atendimento clínico, vacinas, peso |
| Faturamento | `...modules.faturamento` | `faturamento` | (fora do MVP) |
| Notificação | `...modules.notificacao` | — | (ainda não iniciado) |
| Relatório | `...modules.relatorio` | — | (ainda não iniciado) |

**Por que `modules/` existe.** Antes os domínios eram irmãos de `common`, `web`
e `config`, e nada no caminho dizia quais eram domínio e quais não. A regra de
fronteira existia só no texto. Agora a árvore mostra a divisão:

```
com/vetplanet/
  VetPlanetApiApplication.java
  modules/     acesso, agendamento, cliente, prontuario   ← domínios
  common/      vocabulário compartilhado
  web/         borda HTTP genérica
  config/      composition root
```

O ganho não é estético: quando houver um teste de arquitetura (ArchUnit), a
regra "nenhum módulo importa o interior de outro" passa a ser expressável em uma
linha, sobre um prefixo de pacote. A classe principal continua em
`com.vetplanet`, então o scan do Spring alcança `modules.*` sem configuração.

Regras de fronteira:

- **Nunca acessar tabela de outro domínio diretamente.** Precisa de dado do
  vizinho? Chama o application service dele.
- **Comunicação com o frontend é sempre REST** via controller.
- Entre módulos, hoje, só chamada síncrona de service. Eventos assíncronos
  entram quando houver fato real a propagar (ver "Infraestrutura adiada").

### Estrutura de um domínio

Domínio dentro de `modules/`, **camadas dentro dele**. `acesso` é o modelo a
copiar:

```
modules/acesso/
  controller/    UsuarioController              entrada HTTP
  service/       CriarUsuarioService...         um por caso de uso
  repository/    UsuarioRepository              acesso a dados
  entity/        UsuarioEntity, PerfilAcesso    modelo de domínio
  dto/           CriarUsuarioRequestDto,
                 UsuarioResponseDto             contrato da API
  exception/     UsuarioNaoEncontrado...        erros do domínio
  filter/        TokenAutenticacaoFilter        filtro HTTP do domínio (opcional)
  config/        DevSeedConfig                  beans só deste domínio (opcional)
```

As duas últimas são **opcionais** — existem quando o domínio precisa delas, e
`acesso` é o único caso hoje. A regra para criar uma: o que está lá dentro só
faz sentido para este domínio.

Assim a fronteira que importa continua sendo `modules.acesso.*` vs
`modules.cliente.*` — um domínio novo não mexe em pasta de outro — e dentro de cada um fica óbvio onde
cada coisa mora.

**Consequência a ter em mente:** com subpacotes, o `UsuarioRepository`
precisa ser `public`, então o compilador não impede mais `agendamento` de
importá-lo. A regra de "não acessar dado de outro domínio" passa a valer por
disciplina. Se isso começar a ser violado, o caminho é um teste de
arquitetura (ArchUnit) que quebre o build — não voltar a achatar os pacotes.

### O que vive fora dos domínios

Há três pacotes que não pertencem a domínio nenhum. **Cada um tem uma regra
diferente sobre poder ou não conhecer um domínio** — é isso que impede
qualquer um deles de virar gaveta de bagunça:

| Pacote | O que é | Pode importar de um domínio? |
|---|---|---|
| `common/` | vocabulário compartilhado entre domínios (hoje só a hierarquia de exceções) | **Nunca** |
| `web/` | borda HTTP genérica: handler de erro, envelope de erro, id de correlação | **Nunca** |
| `config/` | *composition root* — fiação do Spring e política da aplicação | **Sim**, é o trabalho dele |

O critério para `common/` e `web/`: se a classe cita `Usuario`, `Consulta` ou
qualquer conceito da clínica, ela **não** é genérica — é de um domínio, e o
lugar dela é lá dentro. Um `import com.vetplanet.<dominio>` aparecendo em
`common/` ou `web/` é o sinal de que a abstração está furada.

O caso concreto que já aconteceu: o `ApiExceptionHandler` tinha um
`@ExceptionHandler` para `CredenciaisInvalidasException`, exceção do domínio
`acesso`, porque **faltava a categoria de 401** em `common/exception/`. A
correção certa foi criar `UnauthorizedException` como quarta categoria — não
manter o método dedicado. Se tivesse ficado, `agendamento` e `prontuario`
copiariam o precedente e o handler viraria a lista de todos os domínios.

O `config/` é a exceção porque fiação é literalmente a função dele: o
`SecurityConfig` precisa injetar o `TokenAutenticacaoFilter` de `acesso` para
montar o filter chain, e não há como declarar política de segurança da
aplicação sem tocar em quem autentica. O que **não** vale é o inverso — deixar
em `config/` uma classe que é de um domínio só porque é onde os exemplos de
Spring põem filtros. Foi o que aconteceu com `TokenAutenticacaoFilter` e
`DevSeedConfig`, hoje em `acesso/filter/` e `acesso/config/`.

## Nomenclatura

**Princípio: linguagem de negócio em português, vocabulário técnico em
inglês.** O que é da clínica veterinária fala português; o que é vocabulário
universal de programação fala inglês. Vale igualmente no frontend.

| Camada | Padrão | Exemplo |
|---|---|---|
| Controller | `<Entidade>Controller` | `UsuarioController` |
| Service | `<Ação><Entidade>Service` | `CriarUsuarioService`, `InativarUsuarioService` |
| Repository | `<Entidade>Repository` | `UsuarioRepository` |
| Entity | `<Entidade>Entity` | `UsuarioEntity`, `ConsultaEntity`, `TutorEntity` |
| DTO entrada | `<Ação><Entidade>RequestDto` | `CriarUsuarioRequestDto` |
| DTO saída | `<Entidade>ResponseDto` | `UsuarioResponseDto` |
| Evento | `<Entidade><FatoOcorrido>Event` | `UsuarioCriadoEvent` |
| Exception | `<Situacao>Exception` | `EmailJaCadastradoException` |

- **Toda classe carrega o sufixo da sua camada**, para o fluxo
  `Controller → Service → Repository → Entity` se ler na própria linha, sem
  consultar o import. Em DTO, `Request`/`Response` vêm **antes** do `Dto`
  (`CriarUsuarioRequestDto`, não `CriarUsuarioDto`) — o sufixo diz a camada, o
  que vem antes dele diz a direção do fluxo.
- **Nem tudo numa pasta de camada é daquela camada.** `PerfilAcesso` fica em
  `entity/` e não leva sufixo (é enum de domínio, não tabela); `TokenGenerator`
  fica em `service/` e não leva sufixo (é utilitário técnico, não caso de uso).
- **Renomear entity exige olhar o JPQL**: o nome em `@Query` é o nome simples
  da classe, então a string tem de mudar junto ou a aplicação quebra no boot.
  O `@Table(name = ...)` não muda — a tabela continua `usuario`, sem sufixo.
- **Uma classe de service por caso de uso**, com o verbo explícito —
  `CriarUsuarioService`, nunca `UsuarioService` genérico ou `UsuarioManager`.
- **Métodos em português** (são ação de negócio): `criarUsuario(...)`,
  `buscarConsultasPorTutor(...)`. Exceção: métodos que implementam contrato
  de framework mantêm o nome esperado (`findByEmail` do Spring Data,
  `loadUserByUsername` do Spring Security).
- **Enums de domínio em português**: `PerfilAcesso.ADMINISTRADOR`,
  `StatusConsulta.CONFIRMADA`.
- **Tabelas e colunas em português**, descritivas. PK sempre
  `id_<entidade>` (`id_usuario`, `id_consulta`) — nunca `id` solto. Coluna
  nunca depende da tabela vizinha para ser entendida: `status_consulta`,
  não `status`; `data_agendamento`, não `data`.

### Endpoints REST

Recurso no plural, em português, **sem verbo na URL** — o verbo é o método
HTTP:

```
POST   /api/usuarios                       criar
GET    /api/usuarios                       listar
GET    /api/usuarios/{idUsuario}           buscar
PUT    /api/usuarios/{idUsuario}           atualizar
PATCH  /api/usuarios/{idUsuario}/inativar

POST   /api/sessoes                        autenticar (e-mail + senha)
GET    /api/sessoes/atual                  usuário logado + perfil
DELETE /api/sessoes/atual                  encerrar sessão
```

## Documentação da API (OpenAPI)

Gerada pelo **springdoc-openapi 3.1.0** a partir dos controllers e dos DTOs.

⚠️ **A versão importa.** A série 3.x é a que suporta Spring Boot 4 (a 3.1.0 é
construída sobre o `spring-boot-starter-parent` 4.1.0, a mesma linha usada
aqui); a série 2.x é para Boot 3.x e não serve. Cuidado adicional: o **índice
de busca** do Maven Central ainda lista apenas 2.x — quem manda é o
`maven-metadata.xml` do repositório.

**Fica desligada por padrão e só liga no perfil `dev`.** O default seguro está
em `application.yml` (`springdoc.*.enabled: false`) e o opt-in em
`application-dev.yml`. É deliberado: a documentação mapeia endpoints que
manipulam CPF e prontuário, e um default ligado falharia *aberto* se alguém
esquecesse de configurar o perfil em produção.

São **duas travas independentes**, não uma:

1. o springdoc não sobe (`springdoc.api-docs.enabled: false`);
2. o `SecurityConfig` lê essa mesma propriedade e, com `false`, nem adiciona
   as rotas de documentação ao filter chain — em produção elas caem no
   `anyRequest().authenticated()` e devolvem 401.

**Ao criar um domínio novo**, siga o padrão que `acesso` estabeleceu:

- `@Tag` na classe do controller, nomeado `"<Domínio> — <recurso>"`
  (`"Acesso — usuários"`), que é o que agrupa e ordena na tela;
- `@Operation(summary = ...)` em cada método, com o *porquê* quando houver
  regra não óbvia (por que a senha não entra no update, por que a mensagem de
  erro é vaga);
- endpoint público leva `@SecurityRequirements` vazio, para anular a
  exigência global de token — hoje só o `POST /api/sessoes`.

O `OpenApiConfig` fica em `config/` porque descreve a API inteira. Pela regra
da seção "O que vive fora dos domínios", ele **não importa nada de domínio** —
o que é específico de um domínio é declarado no controller dele.

## Erros e observabilidade

**Exceções são tratadas por categoria, não por classe.** Toda exceção de
negócio estende uma das quatro em `common/exception/`:

| Categoria | HTTP | Quando |
|---|---|---|
| `UnauthorizedException` | 401 | falta credencial válida (não autenticado) |
| `NotFoundException` | 404 | recurso não existe |
| `ConflictException` | 409 | conflita com dado existente (unicidade, concorrência) |
| `BusinessRuleException` | 422 | requisição válida, mas fere regra de domínio |

401 e 403 são coisas diferentes: `UnauthorizedException` é "não sei quem você
é"; 403 é "sei quem você é e você não pode", e quem gera é o
`AccessDeniedException` do Spring Security, tratado à parte. Subclasse de
`UnauthorizedException` **não pode revelar qual parte da credencial falhou** —
distinguir "e-mail não existe" de "senha errada" permite enumerar contas.

Cada domínio cria as suas em `<dominio>/exception/`, com nome que descreve a
situação em português — `UsuarioNaoEncontradoException`,
`HorarioIndisponivelException` — estendendo a categoria certa. **O
`ApiExceptionHandler` não precisa de método novo a cada exceção**; ele trata
as quatro categorias e cobre todos os domínios futuros.

Se nenhuma categoria servir, **acrescente uma quinta** em `common/exception/`.
O que não vale é estender `DomainException` direto e pedir um
`@ExceptionHandler` dedicado: isso faz o handler crescer a cada domínio, que é
justamente o que a hierarquia existe para evitar.

Se o problema for o *formato* do dado, isso é 400 e quem resolve é o Bean
Validation nos DTOs — não uma exceção de domínio.

**Toda requisição tem um id de correlação** (`RequestIdFilter`):

- Vai para o MDC, então **toda linha de log da requisição sai marcada com
  ele** (padrão configurado em `logging.pattern.level`)
- Volta no header `X-Request-Id` e no corpo das respostas de erro
- Se o chamador mandar `X-Request-Id`, o valor é reaproveitado — é assim que
  se correlaciona frontend e backend numa mesma requisição
- Valor recebido de fora é sanitizado (evita log forging)

Na prática: o usuário relata um erro, informa o id que apareceu na tela, e
`grep <id>` no log entrega o rastro completo.

Há um handler final para `Exception` que devolve 500 genérico e loga o stack
trace. **Nunca devolva detalhe interno ao cliente** — só o id da requisição.

## Banco e migrações

- **O Flyway é o dono do schema.** JPA roda com `ddl-auto: validate`, então a
  aplicação só sobe se as entidades baterem com o banco migrado — erro de
  mapeamento aparece no boot, não em produção. **Nunca** mudar para `update`.
- Migrações em `src/main/resources/db/migration/`.
- Nome do arquivo: `V<versão>__<domínio>_<o_que_faz>.sql`, ex.
  `V001__acesso_criar_usuario.sql`. A **versão é global e crescente entre
  todos os domínios** (o Flyway exige unicidade); o prefixo no nome indica a
  qual domínio pertence.
- A tabela de histórico do Flyway fica em `public`; cada migração cria e
  popula o schema do seu próprio domínio.
- **Migração aplicada nunca é editada.** Corrigir com uma nova versão.
- Timestamps sempre `timestamptz`; persistir em **UTC** (exibir em
  `America/Sao_Paulo` é responsabilidade do frontend).

## Autenticação e autorização (implementadas)

> O fluxo completo entre frontend e backend — login, requisição autenticada,
> logout, ciclo de vida da sessão — está escrito e **desenhado em diagramas**
> em [`docs/autenticacao.md`](docs/autenticacao.md). O resumo abaixo é o
> suficiente para codar; o documento explica o porquê de cada peça.

- **Auth própria aqui no Spring**, domínio `acesso`. Não usar
  Clerk/Auth0/Keycloak. Razão: 1–3 usuários, **sem cadastro público**
  (usuários criados pelo administrador), identidade no mesmo Postgres do
  prontuário (LGPD), sem mensalidade.
- **Token opaco no banco, não JWT.** A vantagem do JWT é ser stateless — o
  que só importa em escala. Com 1–3 usuários a consulta por request é
  irrelevante, e em troca ganhamos **revogação imediata**: logout invalida
  de verdade, e inativar um usuário derruba as sessões dele na hora. Com JWT
  puro, o token continuaria valendo até expirar.
- **O banco guarda o SHA-256 do token, nunca o token.** O valor original
  existe só na resposta do login. SHA-256 (e não BCrypt) porque precisa ser
  determinístico para servir de chave de busca — e um token de 256 bits
  aleatórios não é alvo de força bruta como uma senha humana.
- Validade padrão de 12h (`app.sessao.validade`).
- **O frontend usa padrão BFF**: o token vai para um cookie `httpOnly` que o
  servidor do Next guarda; o navegador nunca o vê em JavaScript. Quem chama
  esta API é o servidor do Next, não o browser.
- O frontend tem guard de rota (`proxy.ts`), mas ele só checa presença de
  cookie. **A autorização real é desta API, em todo request.** Nunca assuma
  que o frontend validou algo.

**Regras de acesso:**

| Rota | Quem pode |
|---|---|
| `POST /api/sessoes` | público (é o único jeito de obter token) |
| `/api/usuarios/**` | apenas `ADMINISTRADOR` |
| `PATCH /api/usuarios/atual/senha` | qualquer autenticado, sobre a própria conta |
| resto | qualquer autenticado |

Declaradas com `@PreAuthorize` no controller, onde ficam visíveis. O
`@PreAuthorize` de classe em `UsuarioController` faz endpoint novo **nascer
protegido**.

**Cuidados que já estão no código e não devem ser desfeitos:**

- Login compara a senha mesmo quando o e-mail não existe, e devolve a mesma
  mensagem nos dois casos — diferença de tempo ou de texto revelaria quem
  tem conta.
- Trocar a senha exige a senha atual e **derruba todas as sessões**: de nada
  adiantaria a senha nova se a sessão do invasor seguisse aberta.
- Não se pode inativar nem rebaixar o **último administrador ativo** — sem
  isso o sistema ficaria trancado, sem ninguém para reativar alguém.
- **`acesso/filter/TokenAutenticacaoFilter` não pode levar `@Transactional`.** Isso faria o
  Spring proxiá-lo com CGLIB; o proxy é criado sem chamar o construtor, o
  `logger` herdado de `GenericFilterBean` fica nulo e a aplicação quebra no
  boot. Por isso a consulta usa `join fetch` para trazer o usuário.

**Ainda não feito:** limite de tentativas de login (força bruta) e
redefinição de senha pelo administrador.

## Segurança e LGPD (não negociável)

- Dados de tutor (CPF, endereço) e **prontuário são dados pessoais
  sensíveis**.
- Senha **sempre** com hash (BCrypt). Nunca texto puro, nunca em log, nunca
  em resposta de API — `UsuarioResponseDto` não expõe `senhaHash`.
- **Nenhuma exclusão física** de usuário, consulta ou prontuário — sempre
  inativação por status (`ativo`, `status_consulta`).
- **Duas exceções, com o mesmo critério: não há histórico a proteger.** Animal
  sem consulta (o banco recusa o resto, por chave estrangeira) e bloqueio de
  agenda, que é regra de disponibilidade e não fato ocorrido. Ver
  `ExcluirAnimalService` e `ExcluirBloqueioService`, que documentam o porquê.
- **Prontuário é sempre editável, e toda alteração deixa rastro.** A versão
  anterior desta regra dizia o contrário — "imutável após confirmado, correção
  gera retificação" — e caiu por um argumento melhor: a veterinária lembra do
  nódulo na pata depois de sair da casa, ou a tutora conta algo no portão.
  **Impedir o registro de crescer não protege o prontuário; empobrece**, que é
  o oposto do que a trava pretendia. E é o modelo do próprio CFMV, que pede
  "evolução diária" (Art. 9º, VIII) — registro que cresce, não documento que
  fecha.
  O que dá valor ao documento não é a impossibilidade de editar; é o **rastro**.
  Cada campo alterado depois da conclusão vira uma linha em
  `prontuario.alteracao_atendimento`, com autor, CRMV, valor antigo e novo,
  gravada sozinha — ela não preenche formulário de correção nem escreve motivo.
  Um prontuário que se reescreve em silêncio não prova nada; um que registra a
  mudança prova, e ainda deixa acrescentar.
  **Quem pode escrever continua restrito**: só veterinário com CRMV. Caiu a
  trava por tempo, não a trava por pessoa.

## Regras de negócio

- Atendimento é **sempre presencial e domiciliar**. Não há telemedicina — não
  implementar nem prever videochamada.
- Consulta não é excluída: muda de status (`solicitada`, `confirmada`,
  `cancelada`, `concluida`). **Sem máquina de estados** — a veterinária é a
  única operadora e corrigir um clique errado precisa ser trivial.
- **`PUT /api/consultas/{id}` corrige e remarca, e não aceita `idAnimal` nem
  `status`.** Remarcar precisa existir porque cancelar e recriar escreveria um
  fato falso — `CANCELADA` quer dizer que não aconteceu. Trocar o animal não
  entra porque a consulta é a âncora do histórico clínico dele; o caso do bicho
  errado se resolve cancelando, e ali o cancelamento é honesto. Status tem
  endpoint próprio: é fato que ocorre, não campo de formulário.
- **A edição não trava por status.** A trava, quando existir, vem do prontuário
  preso à consulta — não do relógio. É a mesma lógica do `ExcluirAnimalService`:
  quem recusa é a chave estrangeira, não uma checagem de estado.
- **`agendamento.bloqueio` é o tempo indisponível**, em duas formas na mesma
  tabela: semanal (`dia_da_semana`) ou período (`data_inicio`/`data_fim`), e o
  banco recusa a linha que tentar ser as duas. **As horas são civis de
  America/Sao_Paulo, não UTC** — desvio deliberado da regra de persistência,
  explicado na migração `V005`: bloqueio semanal não é um instante, é uma hora
  do relógio da parede, e em UTC ele mudaria de horário junto com o offset do
  fuso.
- **Não existe disponibilidade positiva** ("atendo das 9h às 18h"). O que há é
  o negativo. Perguntar antes de implementar a outra.
- Perfis de acesso são fixos, em enum: `ADMINISTRADOR`, `VETERINARIO`,
  `ATENDENTE`. Ficam como coluna de `acesso.usuario` com `check` constraint,
  não como tabela — as permissões são conferidas em código. Vira tabela no
  dia em que for preciso criar perfil sem deploy.
- Local do atendimento é campo variável da consulta (casa do tutor, clínica
  parceira, outro). **Os tipos suportados e as regras por tipo ainda não
  foram definidos** — perguntar antes de assumir.
- Escopo do MVP: **só o clínico** (cadastro, agendamento, prontuário).
  Faturamento fica para depois.
- **O prontuário é documento técnico-legal**, com conteúdo definido pela
  Resolução CFMV nº 1.321/2020 (alterada pela nº 1.653/2025). O que a norma
  exige, o que ela **não** exige e as decisões tomadas estão em
  [`docs/prontuario.md`](docs/prontuario.md). Dois pontos que circulam errados
  em blogs de fornecedores e foram verificados no texto oficial: a guarda é de
  **5 anos** após o último atendimento (não 20), e **não há exigência de
  assinatura ICP-Brasil** — a única menção a ICP na resolução está no rodapé
  do próprio PDF do Diário Oficial.
- **Prontuário exige nome completo e número de CRMV** do profissional
  (Art. 9º, II e VIII). Consequência: `acesso.usuario` precisa de campo CRMV,
  e **perfil não é profissão** — o `ADMINISTRADOR` não herda o direito de
  assinar prontuário.

## Deploy e o primeiro administrador

`Dockerfile` em dois estágios (JDK para build, JRE para runtime, usuário sem
privilégio). Os testes ficam **fora** da imagem de propósito: usam
Testcontainers, que precisa de um Docker acessível — quem roda a suíte é a CI,
antes do deploy.

Tudo que varia por ambiente é variável, com default só para o local:

| Variável | Para quê |
|---|---|
| `BANCO_DADOS_URL` / `_USUARIO` / `_SENHA` | conexão com o Postgres |
| `PORTA_HTTP` | porta (plataformas costumam injetar a delas) |
| `ADMIN_INICIAL_EMAIL` / `ADMIN_INICIAL_SENHA` | ver abaixo |

### O ovo e a galinha

Todo endpoint exige autenticação, **inclusive o que cria usuário**, e não há
cadastro público. Uma instância recém-subida tem a tabela `usuario` vazia:
sem um mecanismo próprio, ninguém entra — nem o dono.

`acesso/config/AdministradorInicialConfig` resolve isso fora do perfil `dev`.
Ele é o irmão de produção do `DevSeedConfig`, e difere em três pontos
deliberados:

1. **Sem valor padrão.** Sem as duas variáveis, não cria nada e avisa no log.
   Credencial padrão em ambiente real é como sistema é invadido no primeiro
   dia.
2. **Não registra a senha no log.** A do `DevSeedConfig` é conhecida e
   descartável; esta é real.
3. **Exige 12 caracteres** e falha o start se a senha for menor — subir de pé
   com administrador fraco é pior do que não subir.

É **idempotente**: se já existe qualquer usuário, não faz nada. Por isso as
variáveis podem ficar configuradas sem risco em plataforma que reinicia
contêiner sozinha.

Depois do primeiro acesso: **trocar a senha pelo painel e remover as duas
variáveis** — elas ficam visíveis no painel de qualquer plataforma de deploy,
e o valor delas deixa de ser necessário assim que a conta existe.

> Quando houver mais de um assinante, este mecanismo precisa virar parte do
> provisionamento de cada inquilino — não uma variável global.

## Infraestrutura adiada

Versões antigas da documentação tratavam estes itens como obrigatórios desde
o início. **Não são.** Cada um entra quando existir necessidade real:

| Item | Entra quando |
|---|---|
| Kafka + Outbox | houver evento de domínio real a publicar entre módulos |
| Redis | houver algo que valha cachear (medido, não suposto) |
| Resilience4j | houver chamada a serviço externo |

Quando o Kafka entrar, o padrão de tópico é
`<dominio>.<entidade>.<fato-ocorrido>.v1` (ex.: `acesso.usuario.criado.v1`),
com payload de campos em português. Não subir broker antes de existir
mensagem.

## Spring Boot 4 — o que muda em relação ao que você "sabe"

O Spring Boot 4 é recente e a maior parte da documentação, tutoriais e
memória de modelo de IA assume **3.x**. Pontos que já nos pegaram:

- **Starters foram renomeados**: é `spring-boot-starter-webmvc` (não
  `-web`), e `spring-boot-starter-flyway` existe como starter próprio.
- **Starters de teste são por módulo**: `spring-boot-starter-data-jpa-test`,
  `-security-test`, `-webmvc-test`... e não um único
  `spring-boot-starter-test`.
- **Testcontainers**: o artefato é `org.testcontainers:testcontainers-postgresql`.
- **Spring Security 7** vem junto e tem defaults de CSRF mais agressivos: uma
  API REST sem um `SecurityFilterChain` explícito bloqueia todo request que
  altera estado. Vamos precisar declarar o filter chain — isso é bom, não um
  obstáculo.
- **Jackson 3.0 mudou de pacote**: é `tools.jackson.databind.ObjectMapper`,
  não `com.fasterxml.jackson.databind.ObjectMapper`. O groupId virou
  `tools.jackson.core`.
- **`@AutoConfigureMockMvc` mudou de pacote**: agora é
  `org.springframework.boot.webmvc.test.autoconfigure`, não
  `org.springframework.boot.test.autoconfigure.web.servlet`. (Se aparecer o
  pacote antigo numa busca no cache do Gradle, é jar de outro projeto.)
- Requer **Java 21**.

Truque útil quando um import não resolve: procurar a classe dentro dos jars
em vez de adivinhar o pacote —
`find ~/.gradle/caches -name "spring-boot*.jar" | while read j; do unzip -l "$j" | grep NomeDaClasse.class; done`

**Na dúvida, consulte a documentação da versão instalada em vez de confiar na
memória.** Foi o que evitou erro na migração do frontend para o Next 16.

## Antes de codificar

1. Este arquivo é a fonte de verdade das convenções deste repo.
2. Se a regra de negócio necessária não estiver na seção "Regras de negócio",
   **pergunte** — não assuma comportamento.
3. Rode `./gradlew build` antes de considerar qualquer coisa pronta.
4. Timezone: persistir em **UTC**.
5. Nenhum valor real de produção entra no repositório. O `docker-compose.yml`
   traz credenciais de desenvolvimento local apenas, e o banco só escuta em
   localhost.
