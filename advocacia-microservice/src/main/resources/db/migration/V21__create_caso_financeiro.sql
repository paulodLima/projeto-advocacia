ALTER TABLE financeiro_categoria ADD CONSTRAINT financeiro_categoria_empresa_id UNIQUE(empresa_id,id);
CREATE TABLE caso_financeiro (
    empresa_id UUID NOT NULL, caso_id UUID NOT NULL, versao BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY(caso_id), FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id) ON DELETE CASCADE
);
CREATE TABLE caso_honorario (
    id UUID PRIMARY KEY,empresa_id UUID NOT NULL,caso_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('contratual','exito','sucumbencia')),
    valor NUMERIC(14,2),percentual NUMERIC(5,2),base_calculo NUMERIC(14,2),observacoes VARCHAR(2000) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK(valor IS NULL OR valor>0),CHECK(percentual IS NULL OR percentual>0 AND percentual<=100),CHECK(base_calculo IS NULL OR base_calculo>0),
    FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id) ON DELETE CASCADE
);
CREATE TABLE caso_lancamento (
    id UUID PRIMARY KEY,empresa_id UUID NOT NULL,caso_id UUID NOT NULL,grupo UUID NOT NULL,
    descricao VARCHAR(300) NOT NULL,valor NUMERIC(14,2) NOT NULL CHECK(valor>0),
    direcao VARCHAR(10) NOT NULL CHECK(direcao IN ('entrada','saida')),
    modalidade VARCHAR(20) NOT NULL,vencimento DATE NOT NULL,parcela INTEGER NOT NULL,parcelas INTEGER NOT NULL,
    categoria_id UUID,contato_id UUID,observacoes VARCHAR(2000) NOT NULL,
    pago BOOLEAN NOT NULL DEFAULT FALSE,pago_em DATE,
    titularidade VARCHAR(15) NOT NULL DEFAULT 'escritorio' CHECK(titularidade IN ('escritorio','cliente')),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK(parcelas BETWEEN 1 AND 60 AND parcela BETWEEN 1 AND parcelas),CHECK((pago AND pago_em IS NOT NULL) OR (NOT pago AND pago_em IS NULL)),
    UNIQUE(grupo,parcela),
    FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id) ON DELETE CASCADE,
    FOREIGN KEY(empresa_id,categoria_id) REFERENCES financeiro_categoria(empresa_id,id),
    FOREIGN KEY(empresa_id,contato_id) REFERENCES contato(empresa_id,id)
);
CREATE INDEX caso_lancamento_lista ON caso_lancamento(empresa_id,caso_id,vencimento);
CREATE TABLE caso_despesa (
    id UUID PRIMARY KEY,empresa_id UUID NOT NULL,caso_id UUID NOT NULL,descricao VARCHAR(300) NOT NULL,
    valor NUMERIC(14,2) NOT NULL CHECK(valor>0),data DATE NOT NULL,
    reembolsavel BOOLEAN NOT NULL,reembolsada BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK(NOT reembolsada OR reembolsavel),
    FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id) ON DELETE CASCADE
);
CREATE TABLE caso_financeiro_evento (
    id UUID PRIMARY KEY,empresa_id UUID NOT NULL,caso_id UUID NOT NULL,autor_id UUID NOT NULL REFERENCES usuario(id),
    operacao VARCHAR(40) NOT NULL,referencia UUID NOT NULL,criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id)
);
