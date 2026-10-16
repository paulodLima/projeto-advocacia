CREATE TABLE agenda_vinculo (
 id UUID PRIMARY KEY, empresa_id UUID NOT NULL REFERENCES empresa(id),
 lead_id UUID, tarefa_id UUID, caso_id UUID,
 titulo VARCHAR(300) NOT NULL, tipo VARCHAR(20) NOT NULL,
 data DATE, data_fim DATE, hora TIME, hora_fim TIME, prazo_fatal DATE,
 dia_inteiro BOOLEAN NOT NULL, local VARCHAR(500) NOT NULL,
 situacao VARCHAR(20) NOT NULL, mostrar BOOLEAN NOT NULL,
 privada BOOLEAN NOT NULL, autor_id UUID,
 CHECK((lead_id IS NOT NULL AND tarefa_id IS NULL AND caso_id IS NULL) OR (lead_id IS NULL AND tarefa_id IS NOT NULL AND caso_id IS NOT NULL)),
 UNIQUE(empresa_id,lead_id), UNIQUE(empresa_id,tarefa_id),
 FOREIGN KEY(empresa_id,lead_id) REFERENCES crm_lead(empresa_id,id) ON DELETE CASCADE,
 FOREIGN KEY(empresa_id,tarefa_id) REFERENCES caso_tarefa(empresa_id,id) ON DELETE CASCADE,
 FOREIGN KEY(empresa_id,caso_id) REFERENCES caso(empresa_id,id) ON DELETE CASCADE
);
CREATE INDEX ix_agenda_vinculo_data ON agenda_vinculo(empresa_id,data);
CREATE INDEX ix_agenda_vinculo_prazo ON agenda_vinculo(empresa_id,prazo_fatal);
