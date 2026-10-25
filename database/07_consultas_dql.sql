PRAGMA foreign_keys = ON;

-- 1. Doses perdidas no último mês por paciente
SELECT u.nome AS paciente, COUNT(*) AS doses_perdidas
FROM lembrete l
JOIN medicamento m ON m.id = l.medicamento_id
JOIN usuario u     ON u.id = m.paciente_id
WHERE l.status = 'PERDIDO'
  AND l.horario_previsto >= datetime('now', '-30 days')
GROUP BY u.id, u.nome;

-- 2. Taxa de adesão ao tratamento por paciente
SELECT u.nome AS paciente,
       COUNT(*) AS total,
       SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) AS confirmados,
       ROUND(100.0 * SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) / COUNT(*), 1) AS adesao_percentual
FROM lembrete l
JOIN medicamento m ON m.id = l.medicamento_id
JOIN usuario u     ON u.id = m.paciente_id
WHERE l.status <> 'PENDENTE'
GROUP BY u.id, u.nome;

-- 3. Pacientes de um responsável específico
SELECT p.nome AS paciente, p.email AS email_paciente
FROM vinculo v
JOIN usuario r ON r.id = v.responsavel_id
JOIN usuario p ON p.id = v.paciente_id
WHERE r.email = 'ana@exemplo.com';

-- 4. Próximas consultas por especialidade (LEFT JOIN de exemplo)
SELECT c.data_hora, c.local, m.nome AS medico, e.nome AS especialidade, u.nome AS paciente
FROM consulta c
JOIN medico m         ON m.id = c.medico_id
JOIN especialidade e  ON e.id = m.especialidade_id
JOIN usuario u        ON u.id = c.paciente_id
WHERE c.status = 'AGENDADA'
ORDER BY c.data_hora;