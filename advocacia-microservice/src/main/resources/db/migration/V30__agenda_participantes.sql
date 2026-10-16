CREATE TABLE agenda_participante (
    empresa_id UUID NOT NULL,
    atividade_id UUID NOT NULL,
    contato_id UUID NOT NULL,
    PRIMARY KEY(atividade_id,contato_id),
    FOREIGN KEY(empresa_id,atividade_id) REFERENCES agenda_atividade(empresa_id,id) ON DELETE CASCADE,
    FOREIGN KEY(empresa_id,contato_id) REFERENCES contato(empresa_id,id)
);
CREATE INDEX ix_agenda_participante_contato ON agenda_participante(empresa_id,contato_id);
