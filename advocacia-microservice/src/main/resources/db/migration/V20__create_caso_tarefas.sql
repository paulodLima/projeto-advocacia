ALTER TABLE empresa_usuario ADD CONSTRAINT empresa_usuario_empresa_id UNIQUE(empresa_id,usuario_id);
CREATE TABLE caso_tarefa (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL,
    caso_id UUID NOT NULL,
    versao INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(12) NOT NULL DEFAULT 'pendente' CHECK(status IN ('pendente','concluida')),
    dados TEXT NOT NULL,
    autor_id UUID NOT NULL REFERENCES usuario(id),
    concluida_por UUID REFERENCES usuario(id),
    concluida_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(empresa_id,id),
    FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id) ON DELETE CASCADE
);
CREATE INDEX caso_tarefa_lista ON caso_tarefa(empresa_id,caso_id,status);
CREATE TABLE caso_tarefa_responsavel (
    empresa_id UUID NOT NULL,
    tarefa_id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    PRIMARY KEY(tarefa_id,usuario_id),
    FOREIGN KEY(empresa_id,tarefa_id) REFERENCES caso_tarefa(empresa_id,id) ON DELETE CASCADE,
    FOREIGN KEY(empresa_id,usuario_id) REFERENCES empresa_usuario(empresa_id,usuario_id)
);
CREATE TABLE caso_tarefa_comentario (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL,
    tarefa_id UUID NOT NULL,
    autor_id UUID NOT NULL REFERENCES usuario(id),
    texto VARCHAR(4000) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(empresa_id,tarefa_id) REFERENCES caso_tarefa(empresa_id,id) ON DELETE CASCADE
);
