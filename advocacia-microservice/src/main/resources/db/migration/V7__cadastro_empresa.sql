ALTER TABLE escritorio ADD COLUMN identificacao VARCHAR(80);
UPDATE escritorio SET identificacao = CONCAT('empresa-', CAST(id AS VARCHAR(36)));
ALTER TABLE escritorio ALTER COLUMN identificacao SET NOT NULL;
ALTER TABLE escritorio ADD CONSTRAINT uk_escritorio_identificacao UNIQUE (identificacao);
ALTER TABLE escritorio ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
