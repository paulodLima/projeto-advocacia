CREATE TABLE financeiro_config (
 empresa_id UUID PRIMARY KEY REFERENCES empresa(id) ON DELETE CASCADE,
 versao BIGINT NOT NULL DEFAULT 0,
 urh NUMERIC(14,2) NOT NULL DEFAULT 0, competencia VARCHAR(7) NOT NULL DEFAULT '',
 horas NUMERIC(12,2) NOT NULL DEFAULT 160, modo_horas VARCHAR(10) NOT NULL DEFAULT 'estrutura',
 advogados INTEGER NOT NULL DEFAULT 1, horas_semanais NUMERIC(6,2) NOT NULL DEFAULT 40,
 percentual_produtivo NUMERIC(5,2) NOT NULL DEFAULT 60, semanas_por_mes NUMERIC(3,2) NOT NULL DEFAULT 4.2,
 margem_lucro NUMERIC(5,2) NOT NULL DEFAULT 30, fator_posicionamento NUMERIC(4,2) NOT NULL DEFAULT 3,
 CHECK(urh>=0), CHECK(horas>0), CHECK(modo_horas IN ('direto','estrutura')),
 CHECK(advogados BETWEEN 1 AND 10000), CHECK(horas_semanais BETWEEN 1 AND 168),
 CHECK(percentual_produtivo BETWEEN 1 AND 100), CHECK(semanas_por_mes BETWEEN 1 AND 5),
 CHECK(margem_lucro BETWEEN 0 AND 100), CHECK(fator_posicionamento BETWEEN 1.5 AND 10)
);
CREATE TABLE financeiro_conta (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL, banco VARCHAR(150) NOT NULL,
 tipo VARCHAR(15) NOT NULL CHECK(tipo IN ('conta','cartao','caixa','investimento')),
 saldo NUMERIC(14,2) NOT NULL, padrao BOOLEAN NOT NULL, ativo BOOLEAN NOT NULL,
 CHECK(NOT padrao OR ativo), UNIQUE(empresa_id,nome_chave)
);
CREATE TABLE financeiro_categoria (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL, grupo VARCHAR(80) NOT NULL,
 direcao VARCHAR(10) NOT NULL CHECK(direcao IN ('entrada','saida','ambas')), ativo BOOLEAN NOT NULL,
 UNIQUE(empresa_id,nome_chave)
);
CREATE TABLE financeiro_centro (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL, ativo BOOLEAN NOT NULL,
 UNIQUE(empresa_id,nome_chave)
);
CREATE TABLE financeiro_custo (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
 nome VARCHAR(150) NOT NULL, nome_chave VARCHAR(150) NOT NULL, valor NUMERIC(14,2) NOT NULL CHECK(valor>0),
 UNIQUE(empresa_id,nome_chave)
);
CREATE INDEX financeiro_conta_empresa ON financeiro_conta(empresa_id);
CREATE INDEX financeiro_categoria_empresa ON financeiro_categoria(empresa_id);
CREATE INDEX financeiro_centro_empresa ON financeiro_centro(empresa_id);
CREATE INDEX financeiro_custo_empresa ON financeiro_custo(empresa_id);
