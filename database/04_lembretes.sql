PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS lembrete (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo             TEXT    NOT NULL
                     CHECK (tipo IN ('MEDICAMENTO', 'CONSULTA')),
    medicamento_id   INTEGER,
    consulta_id      INTEGER,
    horario_previsto TEXT    NOT NULL,
    status           TEXT    NOT NULL DEFAULT 'PENDENTE'
                     CHECK (status IN ('PENDENTE', 'CONFIRMADO', 'PERDIDO')),
    alerta_enviado   INTEGER NOT NULL DEFAULT 0
                     CHECK (alerta_enviado IN (0, 1)),
    FOREIGN KEY (medicamento_id) REFERENCES medicamento (id)
        ON DELETE CASCADE,
    FOREIGN KEY (consulta_id) REFERENCES consulta (id)
        ON DELETE CASCADE,
    UNIQUE (medicamento_id, horario_previsto),
    UNIQUE (consulta_id, horario_previsto),
    CHECK (
        (tipo = 'MEDICAMENTO' AND medicamento_id IS NOT NULL
                              AND consulta_id IS NULL)
     OR (tipo = 'CONSULTA'    AND consulta_id IS NOT NULL
                              AND medicamento_id IS NULL)
    )
);

CREATE TABLE IF NOT EXISTS historico (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    lembrete_id INTEGER NOT NULL,
    tipo_evento TEXT    NOT NULL
                CHECK (tipo_evento IN ('GERADO', 'CONFIRMADO',
                                       'PERDIDO', 'ALERTA_ENVIADO')),
    data_hora   TEXT    NOT NULL,
    FOREIGN KEY (lembrete_id) REFERENCES lembrete (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lembrete_status_horario ON lembrete (status, horario_previsto);
CREATE INDEX IF NOT EXISTS idx_historico_lembrete ON historico (lembrete_id);