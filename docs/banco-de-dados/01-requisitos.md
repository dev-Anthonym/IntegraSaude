# Documento de Requisitos e Regras de Negócio - IntegraSaúde (ODS 3)

## 1. Contexto e Organização
O sistema **IntegraSaúde** insere-se no âmbito do **Objetivo de Desenvolvimento Sustentável 3 (ODS 3)** da ONU, que visa assegurar uma vida saudável e promover o bem-estar para todos, em todas as idades. 

O sistema destina-se a apoiar pacientes (especialmente idosos ou pessoas com tratamentos contínuos) e os seus respetivos familiares ou cuidadores (responsáveis). Tradicionalmente, o controlo de medicação e de consultas médicas é feito de forma manual e fragmentada através de papéis, cadernos, alarmes dispersos no telemóvel ou reliance na memória, o que gera falhas críticas[cite: 4].

## 2. Problemas Identificados
* **Esquecimento de doses:** Falha na toma de medicamentos nos horários corretos devido à falta de lembretes centralizados.
* **Duplicidade de doses:** Risco de o paciente tomar o medicamento duas vezes por confusão ou falta de registo.
* **Consultas perdidas:** Desorganização nas datas e horários das consultas médicas, prejudicando o seguimento clínico.
* **Falta de acompanhamento remoto:** Inexistência de um canal direto de aviso para o responsável/cuidador caso o paciente falhe uma medicação importante.

## 3. Atividades Contempladas pelo Sistema
O banco de dados do IntegraSaúde foi modelado para dar suporte às seguintes atividades operacionais:
* Registo e gestão de perfis de utilizadores (pacientes e responsáveis).
* Estabelecimento de vínculos de acompanhamento entre paciente e responsável[cite: 4].
* Registo e calendarização de medicamentos com esquemas de frequência e horários[cite: 4].
* Agendamento e gestão de consultas médicas associadas a especialidades e médicos[cite: 4].
* Geração automática de lembretes e registo de confirmações ou perdas de toma[cite: 4].
* Registo histórico de eventos para auditoria e acompanhamento do tratamento[cite: 4].

## 4. Informações Armazenadas (Por Assunto)
* **Utilizadores:** Dados cadastrais, credenciais de acesso, perfis (Paciente ou Responsável) e data de registo[cite: 4].
* **Vínculos:** Associações N:N entre pacientes e os respetivos responsáveis[cite: 4].
* **Medicamentos:** Nome comercial/princípio ativo, dosagem, frequência em horas, primeiro horário do dia e período de validade do tratamento[cite: 4].
* **Especialidades e Médicos:** Catálogo normalizado de especialidades médicas e médicos associados[cite: 4].
* **Consultas:** Informações de agendamento (médico, especialidade, local, data/hora e status atual)[cite: 4].
* **Lembretes e Histórico:** Instâncias geradas para cada evento de toma ou consulta, com estados de controlo e registo cronológico de eventos[cite: 4].

## 5. Necessidades de Consulta e Relatórios (DQL)
Para apoiar a tomada de decisão e a monitorização da saúde, o sistema contempla as seguintes consultas estratégicas:
* **Agenda do dia:** Listagem unificada (via `UNION ALL`) dos medicamentos a tomar e consultas agendadas para o dia[cite: 4].
* **Rede de contactos:** Consulta dos pacientes sob a responsabilidade de um utilizador e vice-versa[cite: 4].
* **Doses perdidas:** Relatório mensal de adesão para identificar medicamentos e pacientes com maior índice de falhas nos últimos 30 dias[cite: 4].
* **Taxa de adesão global:** Cálculo percentual de tratamentos cumpridos com base nos lembretes confirmados versus perdidos[cite: 4].
* **Próximas consultas:** Listagem filtrada por especialidade médica[cite: 4].

## 6. Regras de Negócio e Mapeamento Estrutural
As regras de negócio do sistema são garantidas ao nível da base de dados através de restrições (`Constraints`), chaves estrangeiras e índices, complementadas pela validação na camada de aplicação[cite: 4]:

* **RN01 (E-mail único):** Garantido por restrição `UNIQUE` na tabela `usuario.email`[cite: 4].
* **RN02 (Perfis válidos):** Restrição `CHECK` assegurando que o perfil é estritamente `'PACIENTE'` ou `'RESPONSAVEL'`[cite: 4].
* **RN03 (Integridade de vínculos):** Chave única composta e restrição `CHECK` para impedir que um utilizador seja responsável por si próprio na tabela `vinculo`[cite: 4].
* **RN04 (Validação de medicamentos):** Restrições de domínio para frequências de toma válidas (1 a 24 horas) e consistência de datas[cite: 4].
* **RN05 (Normalização médica):** Desagregação estrutural em tabelas normalizadas de `especialidade` e `medico` ligadas por chave estrangeira[cite: 4].
* **RN06 (Prevenção de sobreposição de consultas):** Índice único parcial que impede o agendamento de duas consultas ativas para o mesmo paciente no mesmo horário[cite: 4].
* **RN07 a RN11 (Controlo de lembretes e histórico):** Restrições de integridade referencial em cascata (`ON DELETE CASCADE`) e validação de estados de eventos[cite: 4].