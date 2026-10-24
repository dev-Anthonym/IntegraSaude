-- Dados fictícios do módulo Lembretes e Alertas (Pessoa 4)
-- Supõe os dados iniciais dos outros módulos: paciente id 1, medicamentos id 1 e 2, consulta id 1

INSERT INTO lembrete (paciente_id, tipo, medicamento_id, consulta_id, descricao, horario_previsto, status, alerta_enviado)
VALUES
    (1, 'MEDICAMENTO', 1, NULL, 'Losartana 50mg', TIMESTAMP '2026-10-23 08:00:00', 'CONFIRMADO', FALSE),
    (1, 'MEDICAMENTO', 2, NULL, 'Metformina 850mg', TIMESTAMP '2026-10-23 12:00:00', 'PERDIDO', TRUE),
    (1, 'MEDICAMENTO', 1, NULL, 'Losartana 50mg', TIMESTAMP '2026-10-24 08:00:00', 'PENDENTE', FALSE),
    (1, 'CONSULTA', NULL, 1, 'Consulta com cardiologista', TIMESTAMP '2026-10-24 13:00:00', 'PENDENTE', FALSE);

INSERT INTO historico (lembrete_id, tipo_evento, data_hora)
SELECT id, 'GERADO', TIMESTAMP '2026-10-23 00:00:00' FROM lembrete WHERE paciente_id = 1;

INSERT INTO historico (lembrete_id, tipo_evento, data_hora)
SELECT id, 'CONFIRMADO', TIMESTAMP '2026-10-23 08:05:00' FROM lembrete
WHERE medicamento_id = 1 AND horario_previsto = TIMESTAMP '2026-10-23 08:00:00';

INSERT INTO historico (lembrete_id, tipo_evento, data_hora)
SELECT id, 'PERDIDO', TIMESTAMP '2026-10-23 12:31:00' FROM lembrete
WHERE medicamento_id = 2 AND horario_previsto = TIMESTAMP '2026-10-23 12:00:00';

INSERT INTO historico (lembrete_id, tipo_evento, data_hora)
SELECT id, 'ALERTA_ENVIADO', TIMESTAMP '2026-10-23 12:31:00' FROM lembrete
WHERE medicamento_id = 2 AND horario_previsto = TIMESTAMP '2026-10-23 12:00:00';
