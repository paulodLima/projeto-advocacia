CREATE TABLE google_identity (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    subject VARCHAR(255) NOT NULL UNIQUE
);
