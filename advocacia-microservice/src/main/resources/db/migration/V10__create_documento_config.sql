CREATE TABLE documento_config (
 empresa_id UUID PRIMARY KEY REFERENCES empresa(id) ON DELETE CASCADE,
 logo TEXT NOT NULL, rodape TEXT NOT NULL, alinhamento VARCHAR(10) NOT NULL,
 repetir BOOLEAN NOT NULL, documento BOOLEAN NOT NULL, endereco BOOLEAN NOT NULL, contato BOOLEAN NOT NULL
);
CREATE TABLE documento_modelo (
 empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 chave VARCHAR(50) NOT NULL, texto TEXT NOT NULL, PRIMARY KEY (empresa_id, chave)
);
