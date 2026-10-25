PRAGMA foreign_keys = ON;

-- Usuários
INSERT INTO usuario (nome, email, senha_hash, perfil, data_cadastro) VALUES 
('Maria Souza', 'maria@exemplo.com', 'HASH_DEMO_123', 'PACIENTE', '2026-10-01T09:00:00'),
('João Silva', 'joao@exemplo.com', 'HASH_DEMO_123', 'PACIENTE', '2026-10-01T09:30:00'),
('Ana Souza', 'ana@exemplo.com', 'HASH_DEMO_123', 'RESPONSAVEL', '2026-10-01T10:00:00');

-- Vínculos
INSERT INTO vinculo (paciente_id, responsavel_id) VALUES (1, 3);

-- Especialidades
INSERT INTO especialidade (nome) VALUES ('Cardiologia'), ('Neurologia'), ('Clinico Geral');

-- Médicos
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
(1, 2, 'Clínica Neuro', '2026-10-05T10:30:00', 'CANCELADA'),
(2, 3, 'Posto de Saúde', '2026-11-20T09:00:00', 'AGENDADA');

-- Lembretes
INSERT INTO lembrete (tipo, medicamento_id, consulta_id, horario_previsto, status, alerta_enviado) VALUES 
('MEDICAMENTO', 1, NULL, '2026-10-02T08:00:00', 'CONFIRMADO', 1),
('MEDICAMENTO', 1, NULL, '2026-10-02T16:00:00', 'PERDIDO', 1),
('CONSULTA', NULL, 1, '2026-10-10T14:00:00', 'CONFIRMADO', 1);

-- Histórico
INSERT INTO historico (lembrete_id, tipo_evento, data_hora) VALUES 
(1, 'GERADO', '2026-10-02T08:00:00'),
(1, 'CONFIRMADO', '2026-10-02T08:05:00'),
(2, 'GERADO', '2026-10-02T16:00:00'),
(2, 'PERDIDO', '2026-10-02T17:00:00');

-- Exemplos exigidos de UPDATE e DELETE
UPDATE lembrete SET status = 'CONFIRMADO' WHERE id = 2;
DELETE FROM consulta WHERE status = 'CANCELADA';