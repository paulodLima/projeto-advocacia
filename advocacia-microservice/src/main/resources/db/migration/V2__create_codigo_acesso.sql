CREATE TABLE codigo_acesso (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    desafio_id UUID NOT NULL UNIQUE,
    hash VARCHAR(100) NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    enviado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas INTEGER NOT NULL,
    utilizado BOOLEAN NOT NULL,
    janela_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    envios_na_janela INTEGER NOT NULL
);
