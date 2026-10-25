# Consultas Estratégicas e Relatórios (DQL) - IntegraSaúde

Este documento descreve as principais consultas SQL escritas com `JOIN` para extração de relatórios analíticos e operacionais do sistema **IntegraSaúde**, cumprindo os requisitos da disciplina de Base de Dados[cite: 3].

## 1. Agenda do Dia (Medicamentos e Consultas)
Lista de forma unificada todos os compromissos agendados (tomas de medicamentos e consultas) para um determinado paciente na data atual[cite: 3].

-- Agenda combinada do dia para o paciente ID 1
SELECT 'MEDICAMENTO' AS tipo, m.nome AS descricao, m.primeiro_horario AS horario, 'Pendente' AS status
FROM medicamento m
WHERE m.paciente_id = 1
UNION ALL
SELECT 'CONSULTA' AS tipo, md.nome AS descricao, c.data_hora AS horario, c.status AS status
FROM consulta c
JOIN medico md ON md.id = c.medico_id
WHERE c.paciente_id = 1 AND c.status = 'AGENDADA';

## 2. Doses Perdidas no Mês por Paciente
Identifica o total de medicamentos cuja toma foi registada como 'PERDIDO' nos últimos 30 dias, agrupando por paciente[cite: 3].

SELECT u.nome AS paciente, COUNT(*) AS doses_perdidas
FROM lembrete l
JOIN medicamento m ON m.id = l.medicamento_id
JOIN usuario u     ON u.id = m.paciente_id
WHERE l.status = 'PERDIDO'
  AND l.horario_previsto >= strftime('%Y-%m-%dT%H:%M:%S', 'now', '-30 days')
GROUP BY u.id, u.nome
ORDER BY doses_perdidas DESC;

## 3. Taxa de Adesão ao Tratamento
Calcula a percentagem de adesão do paciente com base nos lembretes confirmados face ao total de lembretes processados (excluindo pendentes)[cite: 3].

SELECT u.nome AS paciente,
       COUNT(*) AS total_processados,
       SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) AS confirmados,
       ROUND(100.0 * SUM(CASE WHEN l.status = 'CONFIRMADO' THEN 1 ELSE 0 END) / COUNT(*), 1) AS adesao_percentual
FROM lembrete l
JOIN medicamento m ON m.id = l.medicamento_id
JOIN usuario u     ON u.id = m.paciente_id
WHERE l.status <> 'PENDENTE'
GROUP BY u.id, u.nome;

## 4. Rede de Vínculos (Pacientes e Responsáveis)
Mapeia a relação cruzada entre os utilizadores com perfil de responsável e os respetivos pacientes sob a sua tutela[cite: 3].

SELECT resp.nome AS responsavel, pac.nome AS paciente
FROM vinculo v
JOIN usuario resp ON resp.id = v.responsavel_id
JOIN usuario pac  ON pac.id = v.paciente_id;

## 5. Próximas Consultas por Especialidade
Lista as consultas futuras ordenadas cronologicamente, exibindo o nome do médico, a especialidade e o local de atendimento[cite: 4].

SELECT c.id, u.nome AS paciente, md.nome AS medico, esp.nome AS especialidade, c.local, c.data_hora
FROM consulta c
JOIN usuario u     ON u.id = c.paciente_id
JOIN medico md     ON md.id = c.medico_id
JOIN especialidade esp ON esp.id = md.especialidade_id
WHERE c.status = 'AGENDADA'
ORDER BY c.data_hora ASC;