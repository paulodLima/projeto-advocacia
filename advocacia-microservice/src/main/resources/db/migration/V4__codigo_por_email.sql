CREATE TABLE codigo_email (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    desafio_id UUID NOT NULL UNIQUE,
    hash VARCHAR(100) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    enviado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas INTEGER NOT NULL,
    utilizado BOOLEAN NOT NULL,
    janela_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    envios_na_janela INTEGER NOT NULL
);
INSERT INTO codigo_email SELECT c.usuario_id, u.email, c.desafio_id, c.hash, c.expira_em,
    c.enviado_em, c.tentativas, c.utilizado, c.janela_inicio, c.envios_na_janela
    FROM codigo_acesso c JOIN usuario u ON u.id = c.usuario_id;
DROP TABLE codigo_acesso;
