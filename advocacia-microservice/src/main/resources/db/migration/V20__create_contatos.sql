CREATE TABLE contato (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id),
 nome VARCHAR(150) NOT NULL, tipo VARCHAR(30) NOT NULL, tipo_pessoa VARCHAR(2) NOT NULL,
 documento_chave VARCHAR(14), origem_id UUID, parceiro_id UUID, versao INTEGER NOT NULL DEFAULT 0,
 criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE (empresa_id,id), UNIQUE (empresa_id,documento_chave),
 CHECK (tipo_pessoa IN ('PF','PJ')),
 CHECK (tipo IN ('Cliente','Parte adversa','Parte interessada','Advogado(a)','Fornecedor','Parceiro')),
 FOREIGN KEY (empresa_id,origem_id) REFERENCES cadastro_item(empresa_id,id),
 FOREIGN KEY (empresa_id,parceiro_id) REFERENCES contato(empresa_id,id)
);
CREATE INDEX contato_empresa_nome ON contato(empresa_id,nome,id);
CREATE INDEX contato_empresa_tipo ON contato(empresa_id,tipo);
CREATE TABLE contato_dado (
 contato_id UUID NOT NULL REFERENCES contato(id) ON DELETE CASCADE,
 campo VARCHAR(40) NOT NULL, valor VARCHAR(4000) NOT NULL, PRIMARY KEY (contato_id,campo)
);
CREATE TABLE contato_representante (
 contato_id UUID NOT NULL REFERENCES contato(id) ON DELETE CASCADE,
 ordem INTEGER NOT NULL, campo VARCHAR(40) NOT NULL, valor VARCHAR(150) NOT NULL,
 PRIMARY KEY (contato_id,ordem,campo)
);
CREATE TABLE contato_indicador (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id),
 nome VARCHAR(80) NOT NULL, nome_chave VARCHAR(80) NOT NULL, cor VARCHAR(7) NOT NULL,
 UNIQUE (empresa_id,id), UNIQUE (empresa_id,nome_chave)
);
CREATE TABLE contato_tag (
 empresa_id UUID NOT NULL, contato_id UUID NOT NULL, indicador_id UUID NOT NULL,
 PRIMARY KEY (contato_id,indicador_id),
 FOREIGN KEY (empresa_id,contato_id) REFERENCES contato(empresa_id,id) ON DELETE CASCADE,
 FOREIGN KEY (empresa_id,indicador_id) REFERENCES contato_indicador(empresa_id,id)
);
CREATE TABLE contato_evento (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id), contato_id UUID NOT NULL,
 autor_id UUID NOT NULL, acao VARCHAR(20) NOT NULL,
 ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
