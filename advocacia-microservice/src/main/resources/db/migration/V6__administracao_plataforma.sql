CREATE TABLE administrador_plataforma (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id),
    ativo BOOLEAN NOT NULL
);
CREATE TABLE plataforma_provisionamento (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    concluido BOOLEAN NOT NULL
);
INSERT INTO plataforma_provisionamento (id, concluido) VALUES (1, FALSE);
CREATE TABLE plataforma_auditoria (
    id UUID PRIMARY KEY,
    autor_id UUID REFERENCES usuario(id),
    acao VARCHAR(80) NOT NULL,
    alvo_id UUID NOT NULL,
    registrado_em TIMESTAMP WITH TIME ZONE NOT NULL
);
