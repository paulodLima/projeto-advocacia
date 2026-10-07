CREATE TABLE usuario_perfil (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    telefone VARCHAR(30) NOT NULL DEFAULT '',
    email_pessoal VARCHAR(254) NOT NULL DEFAULT '',
    endereco VARCHAR(500) NOT NULL DEFAULT '',
    foto TEXT NOT NULL DEFAULT ''
);
