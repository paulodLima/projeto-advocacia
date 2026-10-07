CREATE TABLE cadastro_item (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 tipo VARCHAR(20) NOT NULL, nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL,
 ativo BOOLEAN NOT NULL, referencia_id UUID,
 UNIQUE (empresa_id, id), UNIQUE (empresa_id, tipo, nome_chave),
 FOREIGN KEY (empresa_id, referencia_id) REFERENCES cadastro_item(empresa_id, id)
);
CREATE INDEX cadastro_empresa_tipo ON cadastro_item(empresa_id, tipo);
CREATE TABLE cadastro_campo (
 item_id UUID NOT NULL REFERENCES cadastro_item(id) ON DELETE CASCADE,
 campo VARCHAR(30) NOT NULL, valor VARCHAR(150) NOT NULL, PRIMARY KEY (item_id, campo)
);
CREATE TABLE escritorio_rotina (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL, periodo VARCHAR(10) NOT NULL,
 UNIQUE (empresa_id, periodo, nome_chave)
);
CREATE TABLE escritorio_sistema (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, url VARCHAR(1000) NOT NULL, ativo BOOLEAN NOT NULL,
 icone VARCHAR(20) NOT NULL, cor VARCHAR(7) NOT NULL, logo TEXT NOT NULL, ordem INTEGER NOT NULL
);
CREATE INDEX sistema_empresa ON escritorio_sistema(empresa_id);
