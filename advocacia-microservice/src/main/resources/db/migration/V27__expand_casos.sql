ALTER TABLE caso ALTER COLUMN cliente_id DROP NOT NULL;
ALTER TABLE caso ALTER COLUMN lead_id DROP NOT NULL;
ALTER TABLE caso ADD COLUMN versao INTEGER NOT NULL DEFAULT 0;
ALTER TABLE caso ADD COLUMN status VARCHAR(12) NOT NULL DEFAULT 'ativo';
ALTER TABLE caso ADD COLUMN grupo_id UUID;
ALTER TABLE caso ADD COLUMN responsavel_id UUID;
ALTER TABLE caso ADD COLUMN partes_adversas VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE caso ADD COLUMN valor_proposta NUMERIC(16,2);
ALTER TABLE caso ADD COLUMN proposta_em DATE;
ALTER TABLE caso ADD COLUMN desfecho VARCHAR(20) NOT NULL DEFAULT '';
ALTER TABLE caso ADD COLUMN valor_fechado NUMERIC(16,2);
ALTER TABLE caso ADD COLUMN motivo_perda VARCHAR(2000) NOT NULL DEFAULT '';
ALTER TABLE caso ADD COLUMN fechado_em DATE;
ALTER TABLE caso ADD COLUMN atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE caso ADD CONSTRAINT caso_grupo FOREIGN KEY(empresa_id,grupo_id) REFERENCES cadastro_item(empresa_id,id);
ALTER TABLE caso ADD CONSTRAINT caso_responsavel FOREIGN KEY(responsavel_id) REFERENCES usuario(id);
ALTER TABLE caso ADD CONSTRAINT caso_status CHECK(status IN ('ativo','encerrado','convertido'));
ALTER TABLE caso ADD CONSTRAINT caso_valores CHECK((valor_proposta IS NULL OR valor_proposta>=0) AND (valor_fechado IS NULL OR valor_fechado>=0));
CREATE INDEX caso_lista ON caso(empresa_id,status,atualizado_em,id);
CREATE TABLE caso_anotacao (
 id UUID PRIMARY KEY,empresa_id UUID NOT NULL,caso_id UUID NOT NULL,
 data DATE NOT NULL,descricao VARCHAR(4000) NOT NULL,autor_id UUID NOT NULL REFERENCES usuario(id),
 criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id) ON DELETE CASCADE
);
