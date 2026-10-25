PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS medicamento (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    paciente_id      INTEGER NOT NULL,
    nome             TEXT    NOT NULL,
    dosagem          TEXT    NOT NULL,
    frequencia_horas INTEGER NOT NULL
                     CHECK (frequencia_horas BETWEEN 1 AND 24),
    primeiro_horario TEXT    NOT NULL,
    data_inicio      TEXT    NOT NULL,
    data_fim         TEXT    NOT NULL,
    FOREIGN KEY (paciente_id) REFERENCES usuario (id),
    CHECK (data_inicio <= data_fim)
);

CREATE INDEX IF NOT EXISTS idx_medicamento_paciente ON medicamento (paciente_id);