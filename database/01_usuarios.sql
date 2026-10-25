PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS usuario (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    nome          TEXT    NOT NULL,
    email         TEXT    NOT NULL UNIQUE,
    senha_hash    TEXT    NOT NULL,
    perfil        TEXT    NOT NULL
                  CHECK (perfil IN ('PACIENTE', 'RESPONSAVEL')),
    data_cadastro TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS vinculo (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    paciente_id    INTEGER NOT NULL,
    responsavel_id INTEGER NOT NULL,
    FOREIGN KEY (paciente_id)    REFERENCES usuario (id),
    FOREIGN KEY (responsavel_id) REFERENCES usuario (id),
    UNIQUE (paciente_id, responsavel_id),
    CHECK (paciente_id <> responsavel_id)
);

CREATE INDEX IF NOT EXISTS idx_vinculo_paciente ON vinculo (paciente_id);
CREATE INDEX IF NOT EXISTS idx_vinculo_responsavel ON vinculo (responsavel_id);