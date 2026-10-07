CREATE TABLE empresa_identidade (
    empresa_id UUID PRIMARY KEY REFERENCES empresa(id) ON DELETE CASCADE,
    zoom DOUBLE PRECISION NOT NULL DEFAULT 1,
    pos_x DOUBLE PRECISION NOT NULL DEFAULT 50,
    pos_y DOUBLE PRECISION NOT NULL DEFAULT 50,
    CONSTRAINT identidade_zoom CHECK (zoom BETWEEN 1 AND 2.5),
    CONSTRAINT identidade_pos_x CHECK (pos_x BETWEEN 0 AND 100),
    CONSTRAINT identidade_pos_y CHECK (pos_y BETWEEN 0 AND 100)
);
CREATE TABLE empresa_identidade_imagem (
    empresa_id UUID NOT NULL REFERENCES empresa_identidade(empresa_id) ON DELETE CASCADE,
    tipo VARCHAR(40) NOT NULL,
    imagem TEXT NOT NULL,
    PRIMARY KEY (empresa_id, tipo)
);
CREATE TABLE empresa_identidade_uso (
    empresa_id UUID NOT NULL REFERENCES empresa_identidade(empresa_id) ON DELETE CASCADE,
    local VARCHAR(30) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    PRIMARY KEY (empresa_id, local)
);
