CREATE TABLE agenda_workflow_execucao (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES empresa(id),
    requisicao_id UUID NOT NULL,
    assinatura VARCHAR(64) NOT NULL,
    modelo_id UUID NOT NULL,
    modelo_versao INTEGER NOT NULL,
    nome VARCHAR(150) NOT NULL,
    tipo_tarefa_id UUID NOT NULL,
    prazo_fatal DATE NOT NULL,
    buffer INTEGER NOT NULL CHECK(buffer BETWEEN 0 AND 365),
    total_etapas INTEGER NOT NULL CHECK(total_etapas BETWEEN 1 AND 100),
    autor_id UUID REFERENCES usuario(id) ON DELETE SET NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(empresa_id,id),
    UNIQUE(empresa_id,requisicao_id)
);
CREATE TABLE agenda_workflow_exclusao (
    empresa_id UUID NOT NULL,
    execucao_id UUID NOT NULL,
    data DATE NOT NULL,
    PRIMARY KEY(empresa_id,execucao_id,data),
    FOREIGN KEY(empresa_id,execucao_id) REFERENCES agenda_workflow_execucao(empresa_id,id)
);
CREATE TABLE agenda_workflow_etapa (
    empresa_id UUID NOT NULL,
    execucao_id UUID NOT NULL,
    atividade_id UUID NOT NULL,
    modelo_etapa_id UUID NOT NULL,
    ordem INTEGER NOT NULL CHECK(ordem BETWEEN 1 AND 100),
    nome VARCHAR(150) NOT NULL,
    responsavel_snapshot UUID,
    responsavel_nome VARCHAR(150) NOT NULL,
    dias INTEGER NOT NULL CHECK(dias BETWEEN 1 AND 365),
    inicio DATE NOT NULL,
    fim DATE NOT NULL CHECK(fim>=inicio),
    PRIMARY KEY(empresa_id,atividade_id),
    UNIQUE(empresa_id,execucao_id,ordem),
    FOREIGN KEY(empresa_id,execucao_id) REFERENCES agenda_workflow_execucao(empresa_id,id),
    FOREIGN KEY(empresa_id,atividade_id) REFERENCES agenda_atividade(empresa_id,id)
);
