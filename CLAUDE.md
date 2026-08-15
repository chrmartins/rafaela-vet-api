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

```bash
docker compose up -d      # sobe o Postgres
./gradlew bootRun         # sobe a API em http://localhost:8080
./gradlew test            # testes
./gradlew build           # compila + testa
```

**O Postgres publica na porta 5433 do host**, não na 5432 — a 5432 já está
ocupada por outro projeto na máquina do dev. Dentro do container continua
5432. Se `BANCO_DADOS_URL` não estiver definida, o `application.yml` aponta
para `localhost:5433` por padrão.

`docker compose down` para o banco mantendo os dados; `down -v` apaga tudo.

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

## Autenticação (decidida, ainda não implementada)

- **Auth própria aqui no Spring** (Spring Security + JWT, domínio `acesso`).
  Não usar Clerk/Auth0/Keycloak. Razão: 1–3 usuários, **sem cadastro
  público** (usuários criados pelo administrador), identidade no mesmo
  Postgres do prontuário (LGPD), sem mensalidade.
- **O frontend usa padrão BFF**: o token vai para um cookie `httpOnly` que o
  servidor do Next guarda; o navegador nunca vê o token em JavaScript. Quem
  chama esta API é o servidor do Next, não o browser.
- O frontend tem um guard de rota (`proxy.ts`), mas ele só checa presença de
  cookie. **A autorização real é responsabilidade desta API, em todo
  request.** Nunca assuma que o frontend já validou algo.
- Não existe auto-cadastro: não criar endpoint público de registro.

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
- **Jackson 3.0**, com renomeações de classe e mudança de groupId.
- Requer **Java 21**.

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
