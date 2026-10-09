CREATE TABLE parceiro_dado (
    contato_id UUID NOT NULL REFERENCES contato(id) ON DELETE CASCADE,
    campo VARCHAR(40) NOT NULL,
    valor TEXT NOT NULL,
    PRIMARY KEY (contato_id, campo)
);
CREATE TABLE parceiro_socio (
    contato_id UUID NOT NULL REFERENCES contato(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    nome VARCHAR(150) NOT NULL,
    oab VARCHAR(150) NOT NULL,
    PRIMARY KEY (contato_id, ordem)
);
CREATE TABLE parceiro_area (
    contato_id UUID NOT NULL REFERENCES contato(id) ON DELETE CASCADE,
    area VARCHAR(80) NOT NULL,
    PRIMARY KEY (contato_id, area)
);
CREATE INDEX idx_contato_carteira ON contato(empresa_id, parceiro_id);
