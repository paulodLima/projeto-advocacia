ALTER TABLE contato ADD COLUMN cadastro_incompleto BOOLEAN NOT NULL DEFAULT FALSE;
-- Uma etiqueta compartilhada mantém campanhas e contatos ligados mesmo após renomear.
ALTER TABLE contato_indicador ALTER COLUMN nome TYPE VARCHAR(150);
ALTER TABLE contato_indicador ALTER COLUMN nome_chave TYPE VARCHAR(150);
ALTER TABLE contato_indicador ADD COLUMN etiqueta_id UUID;
INSERT INTO cadastro_item(id,empresa_id,tipo,nome,nome_chave,ativo)
SELECT i.id,i.empresa_id,'etiquetas',i.nome,i.nome_chave,TRUE FROM contato_indicador i
WHERE NOT EXISTS (SELECT 1 FROM cadastro_item c WHERE c.empresa_id=i.empresa_id AND c.tipo='etiquetas' AND c.nome_chave=i.nome_chave);
INSERT INTO cadastro_campo(item_id,campo,valor)
SELECT c.id,'nome',c.nome FROM cadastro_item c WHERE c.tipo='etiquetas'
AND NOT EXISTS(SELECT 1 FROM cadastro_campo d WHERE d.item_id=c.id AND d.campo='nome');
INSERT INTO cadastro_campo(item_id,campo,valor)
SELECT c.id,'cor',i.cor FROM cadastro_item c JOIN contato_indicador i ON i.empresa_id=c.empresa_id AND i.nome_chave=c.nome_chave
WHERE c.tipo='etiquetas' AND NOT EXISTS(SELECT 1 FROM cadastro_campo d WHERE d.item_id=c.id AND d.campo='cor');
UPDATE contato_indicador i SET etiqueta_id=(SELECT c.id FROM cadastro_item c WHERE c.empresa_id=i.empresa_id AND c.tipo='etiquetas' AND c.nome_chave=i.nome_chave);
ALTER TABLE contato_indicador ALTER COLUMN etiqueta_id SET NOT NULL;
ALTER TABLE contato_indicador ADD CONSTRAINT contato_indicador_etiqueta FOREIGN KEY(empresa_id,etiqueta_id) REFERENCES cadastro_item(empresa_id,id);
CREATE TABLE crm_lead (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id), versao INTEGER NOT NULL,
 nome VARCHAR(150) NOT NULL, status VARCHAR(12) NOT NULL, temperatura VARCHAR(8) NOT NULL,
 origem VARCHAR(150) NOT NULL, busca TEXT NOT NULL, entrada DATE NOT NULL, proximo DATE,
 dados TEXT NOT NULL, UNIQUE(empresa_id,id),
 CHECK(status IN ('ativo','agendou','ganho','perdido')),
 CHECK(temperatura IN ('quente','morno','frio'))
);
CREATE INDEX crm_lead_empresa_status ON crm_lead(empresa_id,status,proximo,id);
CREATE TABLE crm_cadencia (
 empresa_id UUID NOT NULL REFERENCES empresa(id), passo INTEGER NOT NULL,
 dias INTEGER NOT NULL CHECK(dias BETWEEN 0 AND 365), rotulo VARCHAR(80) NOT NULL,
 PRIMARY KEY(empresa_id,passo)
);
CREATE TABLE crm_etiqueta (
 empresa_id UUID NOT NULL, lead_id UUID NOT NULL, etiqueta_id UUID NOT NULL,
 PRIMARY KEY(lead_id,etiqueta_id),
 FOREIGN KEY(empresa_id,lead_id) REFERENCES crm_lead(empresa_id,id) ON DELETE CASCADE,
 FOREIGN KEY(empresa_id,etiqueta_id) REFERENCES cadastro_item(empresa_id,id)
);
CREATE TABLE crm_tratativa (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL, lead_id UUID NOT NULL,
 data DATE NOT NULL, canal VARCHAR(30) NOT NULL, resultado VARCHAR(80) NOT NULL,
 observacao VARCHAR(4000) NOT NULL, autor_id UUID NOT NULL REFERENCES usuario(id),
 registrado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(empresa_id,lead_id) REFERENCES crm_lead(empresa_id,id) ON DELETE CASCADE
);
CREATE INDEX crm_tratativa_lead ON crm_tratativa(empresa_id,lead_id,data);
CREATE TABLE crm_comentario (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL, lead_id UUID NOT NULL,
 texto VARCHAR(4000) NOT NULL, autor_id UUID NOT NULL REFERENCES usuario(id),
 criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(empresa_id,lead_id) REFERENCES crm_lead(empresa_id,id) ON DELETE CASCADE
);
CREATE TABLE crm_comentario_mencao (
 comentario_id UUID NOT NULL REFERENCES crm_comentario(id) ON DELETE CASCADE,
 usuario_id UUID NOT NULL REFERENCES usuario(id), PRIMARY KEY(comentario_id,usuario_id)
);
CREATE TABLE crm_notificacao (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL, usuario_id UUID NOT NULL REFERENCES usuario(id),
 comentario_id UUID NOT NULL REFERENCES crm_comentario(id) ON DELETE CASCADE,
 lida BOOLEAN NOT NULL DEFAULT FALSE, criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- Bases de integração: os módulos completos de Casos e Agenda serão desenvolvidos depois.
CREATE TABLE caso (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id), cliente_id UUID NOT NULL,
 lead_id UUID NOT NULL, titulo VARCHAR(300) NOT NULL, observacoes VARCHAR(4000) NOT NULL,
 historico TEXT NOT NULL, criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(empresa_id,id), UNIQUE(empresa_id,lead_id),
 FOREIGN KEY(empresa_id,cliente_id) REFERENCES contato(empresa_id,id),
 FOREIGN KEY(empresa_id,lead_id) REFERENCES crm_lead(empresa_id,id)
);
CREATE TABLE agenda_evento (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL, lead_id UUID NOT NULL,
 titulo VARCHAR(200) NOT NULL, quando TIMESTAMP NOT NULL, status VARCHAR(20) NOT NULL,
 UNIQUE(empresa_id,lead_id),
 FOREIGN KEY(empresa_id,lead_id) REFERENCES crm_lead(empresa_id,id) ON DELETE CASCADE
);
CREATE TABLE crm_campanha (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id), versao INTEGER NOT NULL,
 nome VARCHAR(150) NOT NULL, mes INTEGER, cor VARCHAR(7) NOT NULL,
 descricao VARCHAR(500) NOT NULL, UNIQUE(empresa_id,id), CHECK(mes BETWEEN 1 AND 12)
);
CREATE TABLE crm_campanha_tag (
 empresa_id UUID NOT NULL, campanha_id UUID NOT NULL, etiqueta_id UUID NOT NULL,
 PRIMARY KEY(campanha_id,etiqueta_id),
 FOREIGN KEY(empresa_id,campanha_id) REFERENCES crm_campanha(empresa_id,id) ON DELETE CASCADE,
 FOREIGN KEY(empresa_id,etiqueta_id) REFERENCES cadastro_item(empresa_id,id)
);
CREATE TABLE crm_abordagem (
 empresa_id UUID NOT NULL, campanha_id UUID NOT NULL, destinatario VARCHAR(50) NOT NULL,
 ano INTEGER NOT NULL, autor_id UUID NOT NULL REFERENCES usuario(id),
 PRIMARY KEY(campanha_id,destinatario,ano),
 FOREIGN KEY(empresa_id,campanha_id) REFERENCES crm_campanha(empresa_id,id) ON DELETE CASCADE
);
CREATE TABLE crm_manutencao (
 empresa_id UUID NOT NULL, caso_id UUID NOT NULL, data_entrega DATE,
 meses INTEGER NOT NULL CHECK(meses BETWEEN 1 AND 120), ultimo DATE, momento VARCHAR(150) NOT NULL,
 PRIMARY KEY(caso_id), FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id)
);
