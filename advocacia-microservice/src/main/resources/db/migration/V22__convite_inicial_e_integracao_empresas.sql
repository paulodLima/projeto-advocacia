-- Integra a fundação já aplicada com os módulos recebidos da developer.
-- Mantém os IDs existentes; nenhuma empresa é recriada e nenhum registro é removido.
INSERT INTO empresa(id,cnpj)
SELECT e.id, COALESCE(e.cnpj, CONCAT('LEGACY-', SUBSTRING(CAST(e.id AS VARCHAR(36)),1,25)))
FROM escritorio e WHERE NOT EXISTS (SELECT 1 FROM empresa n WHERE n.id=e.id);

INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'razao_social', COALESCE(razao_social,nome) FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'nome_fantasia', nome FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'cnpj', COALESCE(cnpj,'') FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'endereco', COALESCE(endereco,'') FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'cidade', COALESCE(cidade,'') FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'email', COALESCE(email,'') FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'telefone', COALESCE(telefone,'') FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'uf', COALESCE(uf,'') FROM escritorio;
INSERT INTO empresa_dado(empresa_id,campo,valor)
SELECT id, 'cep', COALESCE(cep,'') FROM escritorio;

-- O modelo recebido aceita uma empresa por conta. Preserva todos os vínculos
-- antigos em escritorio_membro e escolhe deterministicamente um vínculo ativo.
INSERT INTO empresa_usuario(usuario_id,empresa_id,papel)
SELECT usuario_id, escritorio_id, CASE WHEN perfil='PROPRIETARIO' THEN 'MASTER' ELSE 'MEMBRO' END
FROM (SELECT m.*, ROW_NUMBER() OVER(PARTITION BY usuario_id ORDER BY
       CASE WHEN perfil='PROPRIETARIO' THEN 0 ELSE 1 END, escritorio_id) AS prioridade
      FROM escritorio_membro m JOIN escritorio e ON e.id=m.escritorio_id
      WHERE m.ativo=TRUE AND e.ativo=TRUE) existentes
WHERE prioridade=1;

CREATE TABLE plataforma_convite_inicial (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL CHECK(status IN ('PENDENTE','ACEITO','CANCELADO','CONCLUIDO')),
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usuario_id UUID REFERENCES usuario(id),
    empresa_id UUID REFERENCES empresa(id)
);
