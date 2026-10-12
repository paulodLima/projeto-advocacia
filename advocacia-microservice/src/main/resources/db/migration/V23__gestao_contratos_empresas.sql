CREATE TABLE empresa_contrato (
    empresa_id UUID PRIMARY KEY REFERENCES escritorio(id),
    situacao VARCHAR(20) NOT NULL CHECK (situacao IN ('ATIVA','SUSPENSA','ENCERRADA')),
    financeiro VARCHAR(20) NOT NULL DEFAULT 'NAO_INFORMADO' CHECK (financeiro IN ('NAO_INFORMADO','REGULAR','INADIMPLENTE')),
    inicio DATE,
    versao BIGINT NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO empresa_contrato(empresa_id,situacao)
SELECT id, CASE WHEN ativo THEN 'ATIVA' ELSE 'SUSPENSA' END FROM escritorio;
-- Preserva as operações existentes; escolhas explícitas anteriores continuam válidas.
INSERT INTO empresa_modulo(empresa_id,modulo_codigo,liberado)
SELECT e.id,m.codigo,TRUE FROM escritorio e CROSS JOIN modulo_plataforma m
WHERE NOT EXISTS (SELECT 1 FROM empresa_modulo em WHERE em.empresa_id=e.id AND em.modulo_codigo=m.codigo);
CREATE TABLE empresa_contrato_evento (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL REFERENCES escritorio(id),
    autor_id UUID NOT NULL REFERENCES usuario(id),
    acao VARCHAR(40) NOT NULL,
    anterior VARCHAR(20) NOT NULL,
    atual VARCHAR(20) NOT NULL,
    motivo VARCHAR(1200) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_contrato_evento_empresa ON empresa_contrato_evento(empresa_id,criado_em);
