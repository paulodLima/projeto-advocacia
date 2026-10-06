CREATE TABLE escritorio (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL
);
CREATE TABLE escritorio_membro (
    id UUID PRIMARY KEY,
    escritorio_id UUID NOT NULL REFERENCES escritorio(id),
    usuario_id UUID NOT NULL REFERENCES usuario(id),
    perfil VARCHAR(30) NOT NULL CHECK (perfil IN ('PROPRIETARIO', 'MEMBRO')),
    ativo BOOLEAN NOT NULL,
    CONSTRAINT uk_escritorio_usuario UNIQUE (escritorio_id, usuario_id)
);
CREATE INDEX idx_membro_usuario ON escritorio_membro(usuario_id);
