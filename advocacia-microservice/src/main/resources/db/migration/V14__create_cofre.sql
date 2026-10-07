CREATE TABLE cofre_credencial (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL, url VARCHAR(1000) NOT NULL,
 so_admin BOOLEAN NOT NULL, versao INTEGER NOT NULL DEFAULT 0, conteudo TEXT NOT NULL,
 UNIQUE(empresa_id,nome_chave)
);
CREATE INDEX cofre_empresa ON cofre_credencial(empresa_id);
