CREATE TABLE agenda_lembrete_preferencia (
    empresa_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    antecedencia_minutos INTEGER NOT NULL DEFAULT 1440 CHECK(antecedencia_minutos IN (0,15,60,1440)),
    PRIMARY KEY(empresa_id,usuario_id),
    FOREIGN KEY(empresa_id,usuario_id) REFERENCES empresa_usuario(empresa_id,usuario_id) ON DELETE CASCADE
);
CREATE TABLE agenda_lembrete_lido (
    empresa_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    atividade_id UUID NOT NULL,
    versao BIGINT NOT NULL CHECK(versao>=0),
    marco VARCHAR(20) NOT NULL CHECK(marco IN ('ATIVIDADE','PRAZO_FATAL')),
    lido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(empresa_id,usuario_id,atividade_id,versao,marco),
    FOREIGN KEY(empresa_id,usuario_id) REFERENCES empresa_usuario(empresa_id,usuario_id) ON DELETE CASCADE,
    FOREIGN KEY(empresa_id,atividade_id) REFERENCES agenda_atividade(empresa_id,id) ON DELETE CASCADE
);
CREATE INDEX ix_agenda_pendente_lembrete ON agenda_atividade(empresa_id,situacao,data,prazo_fatal);
