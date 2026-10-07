CREATE TABLE usuario_tema (
 usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
 tema VARCHAR(10) NOT NULL CHECK(tema IN ('verde','vermelho','amarelo','azul','preto','roxo'))
);
