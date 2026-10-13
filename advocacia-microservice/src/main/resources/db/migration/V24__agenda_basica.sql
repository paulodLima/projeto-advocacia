ALTER TABLE empresa_usuario ADD CONSTRAINT uk_empresa_usuario_vinculo UNIQUE(empresa_id,usuario_id);
CREATE TABLE agenda_atividade (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresa(id),
    versao BIGINT NOT NULL DEFAULT 0,
    tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('TAREFA','PRAZO','REUNIAO','AUDIENCIA')),
    titulo VARCHAR(150) NOT NULL,
    tipo_tarefa_id UUID,
    data DATE,
    data_fim DATE,
    hora TIME,
    hora_fim TIME,
    prazo_fatal DATE,
    dia_inteiro BOOLEAN NOT NULL,
    modalidade VARCHAR(20) CHECK(modalidade IN ('PRESENCIAL','ONLINE')),
    local VARCHAR(1000) NOT NULL DEFAULT '',
    descricao VARCHAR(4000) NOT NULL DEFAULT '',
    situacao VARCHAR(20) NOT NULL CHECK(situacao IN ('PENDENTE','CONCLUIDA','CANCELADA')),
    concluida_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(empresa_id,id),
    FOREIGN KEY(empresa_id,tipo_tarefa_id) REFERENCES cadastro_item(empresa_id,id),
    CHECK(data_fim IS NULL OR (data IS NOT NULL AND data_fim>=data)),
    CHECK(tipo='TAREFA' OR data IS NOT NULL)
);
CREATE INDEX ix_agenda_empresa_data ON agenda_atividade(empresa_id,data);
CREATE INDEX ix_agenda_empresa_prazo ON agenda_atividade(empresa_id,prazo_fatal);
CREATE TABLE agenda_responsavel (
    empresa_id UUID NOT NULL,
    atividade_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    PRIMARY KEY(atividade_id,usuario_id),
    FOREIGN KEY(empresa_id,atividade_id) REFERENCES agenda_atividade(empresa_id,id),
    FOREIGN KEY(empresa_id,usuario_id) REFERENCES empresa_usuario(empresa_id,usuario_id) ON DELETE CASCADE
);
CREATE INDEX ix_agenda_responsavel_usuario ON agenda_responsavel(usuario_id,atividade_id);
CREATE TABLE agenda_evento (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL,
    atividade_id UUID NOT NULL,
    autor_id UUID REFERENCES usuario(id) ON DELETE SET NULL,
    autor_nome VARCHAR(150) NOT NULL,
    acao VARCHAR(30) NOT NULL,
    observacao VARCHAR(2000) NOT NULL DEFAULT '',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(empresa_id,atividade_id) REFERENCES agenda_atividade(empresa_id,id)
);
CREATE INDEX ix_agenda_evento ON agenda_evento(empresa_id,atividade_id,criado_em);
UPDATE modulo_plataforma SET situacao='DISPONIVEL' WHERE codigo='agenda';
