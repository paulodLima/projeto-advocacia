CREATE TABLE empresa (
    id UUID PRIMARY KEY,
    cnpj VARCHAR(32) NOT NULL,
    CONSTRAINT uk_empresa_cnpj UNIQUE (cnpj)
);

CREATE TABLE empresa_dado (
    empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    campo VARCHAR(40) NOT NULL,
    valor VARCHAR(1000) NOT NULL,
    PRIMARY KEY (empresa_id, campo)
);

CREATE TABLE empresa_usuario (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    empresa_id UUID NOT NULL REFERENCES empresa(id),
    papel VARCHAR(20) NOT NULL,
    CONSTRAINT ck_empresa_usuario_papel CHECK (papel IN ('MASTER', 'MEMBRO'))
);
CREATE INDEX ix_empresa_usuario_empresa ON empresa_usuario(empresa_id);
