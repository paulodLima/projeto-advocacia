CREATE TABLE modulo_plataforma (
    codigo VARCHAR(80) PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    grupo VARCHAR(80) NOT NULL,
    situacao VARCHAR(30) NOT NULL CHECK (situacao IN ('DISPONIVEL', 'EM_DESENVOLVIMENTO')),
    ordem INTEGER NOT NULL
);
CREATE TABLE modulo_dependencia (
    modulo_codigo VARCHAR(80) NOT NULL REFERENCES modulo_plataforma(codigo),
    dependencia_codigo VARCHAR(80) NOT NULL REFERENCES modulo_plataforma(codigo),
    PRIMARY KEY (modulo_codigo, dependencia_codigo),
    CHECK (modulo_codigo <> dependencia_codigo)
);
CREATE TABLE empresa_modulo (
    empresa_id UUID NOT NULL REFERENCES escritorio(id),
    modulo_codigo VARCHAR(80) NOT NULL REFERENCES modulo_plataforma(codigo),
    liberado BOOLEAN NOT NULL,
    PRIMARY KEY (empresa_id, modulo_codigo)
);
INSERT INTO modulo_plataforma (codigo, nome, grupo, situacao, ordem) VALUES
('inicio', 'Início', 'Principal', 'DISPONIVEL', 1),
('assistente', 'Assistente', 'Principal', 'EM_DESENVOLVIMENTO', 2),
('agenda', 'Agenda', 'Principal', 'EM_DESENVOLVIMENTO', 3),
('crm', 'CRM', 'Principal', 'EM_DESENVOLVIMENTO', 4),
('casos', 'Casos', 'Principal', 'EM_DESENVOLVIMENTO', 5),
('processual', 'Gestão Processual', 'Principal', 'EM_DESENVOLVIMENTO', 6),
('contatos', 'Contatos', 'Relacionamento', 'EM_DESENVOLVIMENTO', 7),
('parceiros', 'Parceiros', 'Relacionamento', 'EM_DESENVOLVIMENTO', 8),
('equipe', 'Equipe', 'Relacionamento', 'EM_DESENVOLVIMENTO', 9),
('documentos', 'Documentos', 'Ferramentas', 'EM_DESENVOLVIMENTO', 10),
('calculadoras', 'Calculadoras', 'Ferramentas', 'EM_DESENVOLVIMENTO', 11),
('teses', 'Super Teses', 'Ferramentas', 'EM_DESENVOLVIMENTO', 12),
('pops', 'POPs', 'Ferramentas', 'EM_DESENVOLVIMENTO', 13),
('financeiro', 'Financeiro', 'Gestão', 'EM_DESENVOLVIMENTO', 14),
('relatorios', 'Relatórios', 'Gestão', 'EM_DESENVOLVIMENTO', 15),
('produtos', 'Produtos', 'Gestão', 'EM_DESENVOLVIMENTO', 16),
('config', 'Configurações', 'Configurações', 'EM_DESENVOLVIMENTO', 17);
