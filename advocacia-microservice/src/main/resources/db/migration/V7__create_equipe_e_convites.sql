CREATE TABLE equipe_membro (
    usuario_id UUID PRIMARY KEY REFERENCES empresa_usuario(usuario_id) ON DELETE CASCADE,
    nome_exibicao VARCHAR(150) NOT NULL DEFAULT '',
    perfil VARCHAR(20) NOT NULL DEFAULT 'ASSISTENTE',
    situacao VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    admissao DATE,
    nascimento DATE,
    restrito BOOLEAN NOT NULL DEFAULT FALSE,
    envia_documento BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_equipe_perfil CHECK (perfil IN ('ADMINISTRADOR', 'ADVOGADO', 'ASSISTENTE', 'FINANCEIRO')),
    CONSTRAINT ck_equipe_situacao CHECK (situacao IN ('ATIVO', 'INATIVO', 'SUSPENSO'))
);
INSERT INTO equipe_membro (usuario_id, perfil, situacao)
SELECT v.usuario_id, CASE WHEN v.papel = 'MASTER' THEN 'ADMINISTRADOR' ELSE 'ASSISTENTE' END,
       CASE WHEN u.status = 'ATIVO' THEN 'ATIVO' ELSE 'INATIVO' END
FROM empresa_usuario v JOIN usuario u ON u.id = v.usuario_id;

CREATE TABLE equipe_membro_aba (
    usuario_id UUID NOT NULL REFERENCES equipe_membro(usuario_id) ON DELETE CASCADE,
    aba VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, aba)
);

CREATE TABLE equipe_convite (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresa(id),
    email VARCHAR(254) NOT NULL UNIQUE,
    nome VARCHAR(150) NOT NULL,
    nome_exibicao VARCHAR(150) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    restrito BOOLEAN NOT NULL,
    envia_documento BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_convite_status CHECK (status IN ('PENDENTE', 'ACEITO', 'CANCELADO'))
);
CREATE INDEX ix_equipe_convite_empresa ON equipe_convite(empresa_id);
CREATE TABLE equipe_convite_aba (
    convite_id UUID NOT NULL REFERENCES equipe_convite(id) ON DELETE CASCADE,
    aba VARCHAR(30) NOT NULL,
    PRIMARY KEY (convite_id, aba)
);
