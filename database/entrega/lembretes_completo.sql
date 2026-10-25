PRAGMA foreign_keys = ON;

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

-- telefone do usuario, para futuros alertas por SMS
ALTER TABLE usuario ADD COLUMN telefone TEXT;

PRAGMA foreign_keys = ON;

-- UsuÃ¡rios
INSERT INTO usuario (nome, email, senha_hash, perfil, data_cadastro) VALUES 
('Maria Souza', 'maria@exemplo.com', 'HASH_DEMO_123', 'PACIENTE', '2026-10-01T09:00:00'),
('JoÃ£o Silva', 'joao@exemplo.com', 'HASH_DEMO_123', 'PACIENTE', '2026-10-01T09:30:00'),
('Ana Souza', 'ana@exemplo.com', 'HASH_DEMO_123', 'RESPONSAVEL', '2026-10-01T10:00:00');

-- VÃ­nculos
INSERT INTO vinculo (paciente_id, responsavel_id) VALUES (1, 3);

-- Especialidades
INSERT INTO especialidade (nome) VALUES ('Cardiologia'), ('Neurologia'), ('Clinico Geral');

-- MÃ©dicos
INSERT INTO medico (nome, especialidade_id) VALUES 
('Dr. Roberto Carlos', 1),
('Dra. Fernanda Lima', 2),
('Dr. Carlos Alberto', 3);

-- Medicamentos
INSERT INTO medicamento (paciente_id, nome, dosagem, frequencia_horas, primeiro_horario, data_inicio, data_fim) VALUES 
(1, 'Paracetamol', '500mg', 8, '2026-10-01T08:00:00', '2026-10-01T00:00:00', '2026-10-15T23:59:59'),
(1, 'Omeprazol', '20mg', 24, '2026-10-01T07:00:00', '2026-10-01T00:00:00', '2026-10-30T23:59:59');

-- Consultas
INSERT INTO consulta (paciente_id, medico_id, local, data_hora, status) VALUES 
(1, 1, 'Hospital Vida', '2026-10-10T14:00:00', 'REALIZADA'),
(1, 2, 'ClÃ­nica Neuro', '2026-10-05T10:30:00', 'CANCELADA'),
(2, 3, 'Posto de SaÃºde', '2026-11-20T09:00:00', 'AGENDADA');

-- Lembretes
INSERT INTO lembrete (tipo, medicamento_id, consulta_id, horario_previsto, status, alerta_enviado) VALUES 
('MEDICAMENTO', 1, NULL, '2026-10-02T08:00:00', 'CONFIRMADO', 1),
('MEDICAMENTO', 1, NULL, '2026-10-02T16:00:00', 'PERDIDO', 1),
('CONSULTA', NULL, 1, '2026-10-10T14:00:00', 'CONFIRMADO', 1);

-- HistÃ³rico
INSERT INTO historico (lembrete_id, tipo_evento, data_hora) VALUES 
(1, 'GERADO', '2026-10-02T08:00:00'),
(1, 'CONFIRMADO', '2026-10-02T08:05:00'),
(2, 'GERADO', '2026-10-02T16:00:00'),
(2, 'PERDIDO', '2026-10-02T17:00:00');

-- Exemplos exigidos de UPDATE e DELETE
UPDATE lembrete SET status = 'CONFIRMADO' WHERE id = 2;
DELETE FROM consulta WHERE status = 'CANCELADA';

PRAGMA foreign_keys = ON;

-- 1. Doses perdidas no Ãºltimo mÃªs por paciente
SELECT u.nome AS paciente, COUNT(*) AS doses_perdidas
FROM lembrete l
JOIN medicamento m ON m.id = l.medicamento_id
JOIN usuario u     ON u.id = m.paciente_id
WHERE l.status = 'PERDIDO'
  AND l.horario_previsto >= datetime('now', '-30 days')
GROUP BY u.id, u.nome;

-- 2. Taxa de adesÃ£o ao tratamento por paciente
SELECT u.nome AS paciente,
       COUNT(*) AS total,
       SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) AS confirmados,
       ROUND(100.0 * SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) / COUNT(*), 1) AS adesao_percentual
FROM lembrete l
JOIN medicamento m ON m.id = l.medicamento_id
JOIN usuario u     ON u.id = m.paciente_id
WHERE l.status <> 'PENDENTE'
GROUP BY u.id, u.nome;

-- 3. Pacientes de um responsÃ¡vel especÃ­fico
SELECT p.nome AS paciente, p.email AS email_paciente
FROM vinculo v
JOIN usuario r ON r.id = v.responsavel_id
JOIN usuario p ON p.id = v.paciente_id
WHERE r.email = 'ana@exemplo.com';

-- 4. PrÃ³ximas consultas por especialidade (LEFT JOIN de exemplo)
SELECT c.data_hora, c.local, m.nome AS medico, e.nome AS especialidade, u.nome AS paciente
FROM consulta c
JOIN medico m         ON m.id = c.medico_id
JOIN especialidade e  ON e.id = m.especialidade_id
JOIN usuario u        ON u.id = c.paciente_id
WHERE c.status = 'AGENDADA'
ORDER BY c.data_hora;

