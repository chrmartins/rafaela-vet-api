# CLAUDE.md — rafaela-vet-api

API do sistema **Dra. Rafaela Soares** — atendimento veterinário domiciliar
(clínica geral, cães e gatos) no Rio de Janeiro.

> Repositório irmão: **`rafaela-vet-front`** (Next.js — site público em
> `rafaelasoares.vet` + painel administrativo em `/painel`). É o único
> consumidor desta API hoje.
>
> Este arquivo é **autocontido de propósito**: num clone limpo deste repo, o
> `CLAUDE.md` e o `padrao-nomenclatura.md` da pasta-mãe não existem. Tudo que
> é preciso para trabalhar aqui está abaixo.

## Estado atual

Esqueleto recém-criado. **Existe**: build Gradle, Docker Compose com Postgres,
`application.yml`, e a migração `V001` criando o schema `acesso` e a tabela
`usuario`.

**Ainda não existe**: nenhuma entidade, repository, service, controller ou
configuração de segurança. O primeiro trabalho é a fatia vertical de
`acesso`/`usuario` (CRUD + hash de senha), que é o que destrava o login do
painel.

⚠️ A migração `V001` **ainda não foi executada** contra o banco — o Flyway
roda no primeiro `bootRun`. Confirme que ela aplica antes de seguir.

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

`lifecycle-management: start-only` faz o banco continuar de pé ao parar a
aplicação — sem isso, cada Ctrl+C derrubaria o contêiner e o próximo start
pagaria o tempo de subida de novo.

**O Postgres publica na porta 5433 do host**, não na 5432 — a 5432 já está
ocupada por outro projeto na máquina do dev. Dentro do contêiner continua
5432. Rodando pelo compose, o Spring descobre a conexão sozinho; os valores
em `application.yml` valem para produção, onde não há compose.

O **frontend não entra neste compose** — roda à parte com `npm run dev` no
`rafaela-vet-front`. Cada repositório sobe o que é seu.

## Arquitetura

**Monólito modular por domínio.** Um pacote Java por domínio, e cada domínio
é dono do seu próprio schema Postgres:

| Domínio | Pacote | Schema | Responsabilidade |
|---|---|---|---|
| Acesso | `com.rafaelasoares.acesso` | `acesso` | usuários do painel, autenticação |
| Cadastro | `...cadastro` | `cadastro` | tutores, animais, endereços |
| Agendamento | `...agendamento` | `agendamento` | consultas, disponibilidade |
| Prontuário | `...prontuario` | `prontuario` | atendimento clínico, vacinas, peso |
| Faturamento | `...faturamento` | `faturamento` | (fora do MVP) |
| Notificação | `...notificacao` | — | (ainda não iniciado) |
| Relatório | `...relatorio` | — | (ainda não iniciado) |

Regras de fronteira:

- **Nunca acessar tabela de outro domínio diretamente.** Precisa de dado do
  vizinho? Chama o application service dele.
- **Comunicação com o frontend é sempre REST** via controller.
- Entre módulos, hoje, só chamada síncrona de service. Eventos assíncronos
  entram quando houver fato real a propagar (ver "Infraestrutura adiada").

### Estrutura de um domínio

Domínio no topo, **camadas dentro dele**. `acesso` é o modelo a copiar:

```
acesso/
  controller/    UsuarioController          entrada HTTP
  service/       CriarUsuarioService...     um por caso de uso
  repository/    UsuarioRepository          acesso a dados
  entity/        Usuario, PerfilAcesso      modelo de domínio
  dto/           CriarUsuarioRequest,
                 UsuarioResponse            contrato da API
  exception/     UsuarioNaoEncontrado...    erros do domínio
```

Assim a fronteira que importa continua sendo `acesso.*` vs `cadastro.*` — um
domínio novo não mexe em pasta de outro — e dentro de cada um fica óbvio onde
cada coisa mora.

**Consequência a ter em mente:** com subpacotes, o `UsuarioRepository`
precisa ser `public`, então o compilador não impede mais `agendamento` de
importá-lo. A regra de "não acessar dado de outro domínio" passa a valer por
disciplina. Se isso começar a ser violado, o caminho é um teste de
arquitetura (ArchUnit) que quebre o build — não voltar a achatar os pacotes.

## Nomenclatura

**Princípio: linguagem de negócio em português, vocabulário técnico em
inglês.** O que é da clínica veterinária fala português; o que é vocabulário
universal de programação fala inglês. Vale igualmente no frontend.

| Camada | Padrão | Exemplo |
|---|---|---|
| Controller | `<Entidade>Controller` | `UsuarioController` |
| Service | `<Ação><Entidade>Service` | `CriarUsuarioService`, `InativarUsuarioService` |
| Repository | `<Entidade>Repository` | `UsuarioRepository` |
| Entity | `<Entidade>` (sem sufixo) | `Usuario`, `Consulta`, `Tutor` |
| DTO entrada | `<Ação><Entidade>Request` | `CriarUsuarioRequest` |
| DTO saída | `<Entidade>Response` | `UsuarioResponse` |
| Evento | `<Entidade><FatoOcorrido>Event` | `UsuarioCriadoEvent` |
| Exception | `<Situacao>Exception` | `EmailJaCadastradoException` |

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

## Erros e observabilidade

**Exceções são tratadas por categoria, não por classe.** Toda exceção de
negócio estende uma das três em `common/exception/`:

| Categoria | HTTP | Quando |
|---|---|---|
| `NotFoundException` | 404 | recurso não existe |
| `ConflictException` | 409 | conflita com dado existente (unicidade, concorrência) |
| `BusinessRuleException` | 422 | requisição válida, mas fere regra de domínio |

Cada domínio cria as suas em `<dominio>/exception/`, com nome que descreve a
situação em português — `UsuarioNaoEncontradoException`,
`HorarioIndisponivelException` — estendendo a categoria certa. **O
`ApiExceptionHandler` não precisa de método novo a cada exceção**; ele trata
as três categorias e cobre todos os domínios futuros.

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
- **`TokenAutenticacaoFilter` não pode levar `@Transactional`.** Isso faria o
  Spring proxiá-lo com CGLIB; o proxy é criado sem chamar o construtor, o
  `logger` herdado de `GenericFilterBean` fica nulo e a aplicação quebra no
  boot. Por isso a consulta usa `join fetch` para trazer o usuário.

**Ainda não feito:** limite de tentativas de login (força bruta) e
redefinição de senha pelo administrador.

## Segurança e LGPD (não negociável)

- Dados de tutor (CPF, endereço) e **prontuário são dados pessoais
  sensíveis**.
- Senha **sempre** com hash (BCrypt). Nunca texto puro, nunca em log, nunca
  em resposta de API — `UsuarioResponse` não expõe `senhaHash`.
- **Nenhuma exclusão física** de usuário, consulta ou prontuário — sempre
  inativação por status (`ativo`, `status_consulta`).
- **Prontuário é imutável após confirmado** — correção gera registro de
  retificação, nunca sobrescreve o original.

## Regras de negócio

- Atendimento é **sempre presencial e domiciliar**. Não há telemedicina — não
  implementar nem prever videochamada.
- Consulta não é excluída: muda de status (`solicitada`, `confirmada`,
  `cancelada`, `concluida`).
- Perfis de acesso são fixos, em enum: `ADMINISTRADOR`, `VETERINARIO`,
  `ATENDENTE`. Ficam como coluna de `acesso.usuario` com `check` constraint,
  não como tabela — as permissões são conferidas em código. Vira tabela no
  dia em que for preciso criar perfil sem deploy.
- Local do atendimento é campo variável da consulta (casa do tutor, clínica
  parceira, outro). **Os tipos suportados e as regras por tipo ainda não
  foram definidos** — perguntar antes de assumir.
- Escopo do MVP: **só o clínico** (cadastro, agendamento, prontuário).
  Faturamento fica para depois.

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
