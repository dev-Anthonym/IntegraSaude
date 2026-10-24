-- Consultas (DQL) do módulo Lembretes e Alertas, usadas também no trabalho de Banco de Dados

-- 1. Doses perdidas por paciente no último mês
SELECT u.id AS paciente_id, u.nome AS paciente, COUNT(*) AS doses_perdidas
FROM lembrete l
JOIN usuario u ON u.id = l.paciente_id
WHERE l.tipo = 'MEDICAMENTO'
  AND l.status = 'PERDIDO'
  AND l.horario_previsto >= CURRENT_TIMESTAMP - INTERVAL '1' MONTH
GROUP BY u.id, u.nome
ORDER BY doses_perdidas DESC;

-- 2. Taxa de adesão por paciente: confirmados / (confirmados + perdidos)
SELECT u.id AS paciente_id,
       u.nome AS paciente,
       SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) AS confirmados,
       SUM(CASE WHEN l.status = 'PERDIDO' THEN 1 ELSE 0 END) AS perdidos,
       ROUND(100.0 * SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) / COUNT(*), 1) AS taxa_adesao_percentual
FROM lembrete l
JOIN usuario u ON u.id = l.paciente_id
WHERE l.status IN ('CONFIRMADO', 'PERDIDO')
GROUP BY u.id, u.nome
ORDER BY taxa_adesao_percentual;

-- 3. Medicamentos mais esquecidos (lembretes perdidos por medicamento)
SELECT l.medicamento_id, l.descricao AS medicamento, COUNT(*) AS vezes_perdido
FROM lembrete l
WHERE l.tipo = 'MEDICAMENTO'
  AND l.status = 'PERDIDO'
GROUP BY l.medicamento_id, l.descricao
ORDER BY vezes_perdido DESC;

-- 4. Histórico completo de um paciente, em ordem cronológica (troque o 1 pelo id do paciente)
SELECT h.data_hora, h.tipo_evento, l.tipo, l.descricao, l.horario_previsto
FROM historico h
JOIN lembrete l ON l.id = h.lembrete_id
WHERE l.paciente_id = 1
ORDER BY h.data_hora, h.id;

-- 5. Alertas enviados aos responsáveis por dia
SELECT CAST(h.data_hora AS DATE) AS dia, COUNT(*) AS alertas_enviados
FROM historico h
WHERE h.tipo_evento = 'ALERTA_ENVIADO'
GROUP BY CAST(h.data_hora AS DATE)
ORDER BY dia;
