-- Sessões ativas do painel (domínio ACESSO).
--
-- Escolhemos token opaco guardado aqui, e não JWT: com 1–3 usuários a
-- consulta por request é irrelevante, e em troca ganhamos revogação
-- imediata — logout invalida de verdade, e dá para encerrar a sessão de
-- alguém na hora.

create table acesso.token_autenticacao (
    id_token      uuid primary key default gen_random_uuid(),
    id_usuario    uuid        not null references acesso.usuario(id_usuario),

    -- Guardamos o HASH do token, nunca o token em si. Mesma lógica da senha:
    -- se este banco vazar, os tokens não servem para entrar. É SHA-256 (e não
    -- BCrypt) porque precisa ser determinístico para servir de chave de busca.
    token_hash    text        not null,

    criado_em     timestamptz not null default now(),
    expira_em     timestamptz not null,
    -- Nulo enquanto a sessão está válida; preenchido no logout.
    revogado_em   timestamptz,

    constraint token_autenticacao_hash_unico unique (token_hash)
);

-- Caminho da autenticação: a cada request buscamos a sessão pelo hash.
create index token_autenticacao_por_usuario
    on acesso.token_autenticacao (id_usuario);

comment on table  acesso.token_autenticacao is 'Sessões ativas do painel administrativo';
comment on column acesso.token_autenticacao.token_hash is 'SHA-256 do token — o valor original só o cliente conhece';
comment on column acesso.token_autenticacao.revogado_em is 'Preenchido no logout; sessão revogada não autentica';
