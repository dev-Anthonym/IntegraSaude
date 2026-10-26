PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS especialidade (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT    NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS medico (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    nome             TEXT    NOT NULL,
    especialidade_id INTEGER NOT NULL,
    FOREIGN KEY (especialidade_id) REFERENCES especialidade (id)
);

CREATE TABLE IF NOT EXISTS consulta (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    paciente_id INTEGER NOT NULL,
    medico_id   INTEGER NOT NULL,
    local       TEXT    NOT NULL,
    data_hora   TEXT    NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'AGENDADA'
                CHECK (status IN ('AGENDADA', 'REALIZADA', 'CANCELADA')),
    FOREIGN KEY (paciente_id) REFERENCES usuario (id),
    FOREIGN KEY (medico_id)   REFERENCES medico (id)
);

CREATE INDEX IF NOT EXISTS idx_medico_especialidade ON medico (especialidade_id);
CREATE INDEX IF NOT EXISTS idx_consulta_paciente ON consulta (paciente_id);
CREATE INDEX IF NOT EXISTS idx_consulta_medico ON consulta (medico_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_consulta_paciente_horario
    ON consulta (paciente_id, data_hora)
    WHERE status = 'AGENDADA';