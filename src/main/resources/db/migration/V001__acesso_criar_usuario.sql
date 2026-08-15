-- Domínio ACESSO — quem entra no painel administrativo.
--
-- Prefixo do arquivo indica o domínio; a versão (V001, V002...) é global e
-- crescente entre todos os domínios, porque o Flyway exige versão única.

create schema if not exists acesso;

create table acesso.usuario (
    id_usuario      uuid primary key default gen_random_uuid(),
    nome_completo   text        not null,
    email           text        not null,
    senha_hash      text        not null,
    -- DESVIO DELIBERADO do padrao-nomenclatura.md, que previa uma tabela
    -- `perfil_acesso`: os perfis são fixos e as permissões são conferidas em
    -- código, então uma tabela só adicionaria join sem ganhar nada. Vira
    -- tabela no dia em que a Dra. Rafaela precisar criar perfil sem deploy.
    perfil_acesso   text        not null,
    -- Usuário nunca é excluído fisicamente: é inativado (mesma política de
    -- consulta e prontuário, ver CLAUDE.md).
    ativo           boolean     not null default true,
    criado_em       timestamptz not null default now(),
    atualizado_em   timestamptz not null default now(),

    constraint usuario_perfil_acesso_valido
        check (perfil_acesso in ('ADMINISTRADOR', 'VETERINARIO', 'ATENDENTE')),
    constraint usuario_nome_completo_nao_vazio
        check (length(trim(nome_completo)) > 0)
);

-- E-mail é o identificador de login: único sem diferenciar maiúsculas.
-- Índice em lower() evita cadastrar "Rafaela@..." e "rafaela@..." como dois.
create unique index usuario_email_unico
    on acesso.usuario (lower(email));

comment on table  acesso.usuario is 'Pessoas com acesso ao painel administrativo';
comment on column acesso.usuario.senha_hash is 'Hash BCrypt — nunca armazenar senha em texto puro';
comment on column acesso.usuario.ativo is 'Inativação lógica; não há exclusão física';
