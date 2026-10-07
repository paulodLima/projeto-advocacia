CREATE TABLE escritorio_workflow (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL, buffer INTEGER NOT NULL CHECK(buffer BETWEEN 0 AND 365),
 versao INTEGER NOT NULL DEFAULT 0, UNIQUE(empresa_id,nome_chave), UNIQUE(empresa_id,id)
);
CREATE TABLE workflow_etapa (
 workflow_id UUID NOT NULL REFERENCES escritorio_workflow(id) ON DELETE CASCADE,
 id UUID NOT NULL, nome VARCHAR(150) NOT NULL, responsavel UUID REFERENCES usuario(id) ON DELETE SET NULL,
 dias INTEGER NOT NULL CHECK(dias BETWEEN 1 AND 365), ordem INTEGER NOT NULL,
 PRIMARY KEY(workflow_id,id), UNIQUE(workflow_id,ordem)
);
CREATE TABLE workflow_gatilho (
 empresa_id UUID NOT NULL, workflow_id UUID NOT NULL, tarefa_id UUID NOT NULL,
 PRIMARY KEY(workflow_id,tarefa_id),
 FOREIGN KEY(empresa_id,workflow_id) REFERENCES escritorio_workflow(empresa_id,id) ON DELETE CASCADE,
 FOREIGN KEY(empresa_id,tarefa_id) REFERENCES cadastro_item(empresa_id,id)
);
CREATE INDEX workflow_empresa ON escritorio_workflow(empresa_id);
CREATE INDEX workflow_gatilho_tarefa ON workflow_gatilho(empresa_id,tarefa_id);
