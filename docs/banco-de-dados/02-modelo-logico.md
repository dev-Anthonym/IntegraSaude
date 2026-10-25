# Modelo Lógico, Normalização (3FN) e Dicionário de Dados - IntegraSaúde

## 1. Modelo Lógico Relacional
O modelo lógico traduz o Diagrama de Entidade-Relacionamento (DER) para o modelo relacional de tabelas, definindo as chaves primárias (PK), chaves estrangeiras (FK) e restrições de domínio:

* **usuario** (`id` [PK], `nome`, `email` [UNIQUE], `senha_hash`, `perfil`, `data_cadastro`)
* **vinculo** (`id` [PK], `paciente_id` [FK -> usuario], `responsavel_id` [FK -> usuario])
* **especialidade** (`id` [PK], `nome` [UNIQUE])
* **medico** (`id` [PK], `nome`, `especialidade_id` [FK -> especialidade])
* **medicamento** (`id` [PK], `paciente_id` [FK -> usuario], `nome`, `dosagem`, `frequencia_horas`, `primeiro_horario`, `data_inicio`, `data_fim`)
* **consulta** (`id` [PK], `paciente_id` [FK -> usuario], `medico_id` [FK -> medico], `local`, `data_hora`, `status`)
* **lembrete** (`id` [PK], `tipo`, `medicamento_id` [FK -> medicamento, ON DELETE CASCADE], `consulta_id` [FK -> consulta, ON DELETE CASCADE], `horario_previsto`, `status`, `alerta_enviado`)
* **historico** (`id` [PK], `lembrete_id` [FK -> lembrete, ON DELETE CASCADE], `tipo_evento`, `data_hora`)

## 2. Processo de Normalização até a 3FN
* **1ª Forma Normal (1FN):** Todos os atributos contêm apenas valores atômicos e indivisíveis. Não existem listas ou grupos repetitivos nas colunas (por exemplo, os horários de medicação são calculados dinamicamente a partir do primeiro horário e da frequência em horas, evitando listas estáticas).
* **2ª Forma Normal (2FN):** A tabela encontra-se na 1FN e todos os atributos não-chave dependem totalmente da chave primária inteira (sem dependências parciais em chaves compostas).
* **3ª Forma Normal (3FN):** A tabela encontra-se na 2FN e não possui dependências transitivas. Por exemplo, os dados da especialidade médica foram isolados numa tabela própria (`especialidade`) em vez de se repetirem na tabela de médicos ou consultas, eliminando redundâncias. A tabela `medicamento` e `consulta` acedem ao paciente através de `paciente_id`, sem dependências transitivas.

## 3. Dicionário de Dados

### Tabela: `usuario`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único do utilizador |
| nome | TEXT | NOT NULL | Nome completo do utilizador |
| email | TEXT | NOT NULL, UNIQUE | Endereço de e-mail para acesso e contacto[cite: 4] |
| senha_hash | TEXT | NOT NULL | Hash encriptado da palavra-passe |
| perfil | TEXT | NOT NULL, CHECK | Perfil do utilizador (`PACIENTE` ou `RESPONSAVEL`)[cite: 4] |
| data_cadastro | TEXT | NOT NULL | Data e hora de registo no formato ISO-8601 |

### Tabela: `vinculo`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único do vínculo |
| paciente_id | INTEGER | NOT NULL, FK | Referência ao utilizador com perfil paciente |
| responsavel_id | INTEGER | NOT NULL, FK | Referência ao utilizador com perfil responsável |

### Tabela: `especialidade`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único da especialidade |
| nome | TEXT | NOT NULL, UNIQUE | Nome da especialidade médica (ex: Cardiologia) |

### Tabela: `medico`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único do médico |
| nome | TEXT | NOT NULL | Nome do profissional médico |
| especialidade_id | INTEGER | FK | Referência à especialidade associada |

### Tabela: `medicamento`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único do medicamento |
| paciente_id | INTEGER | FK | Utilizador (paciente) a quem o medicamento pertence |
| nome | TEXT | NOT NULL | Nome comercial ou princípio ativo |
| dosagem | TEXT | NOT NULL | Concentração e unidade (ex: 500mg) |
| frequencia_horas| INTEGER | CHECK (1-24) | Intervalo em horas entre tomas[cite: 4] |
| primeiro_horario | TEXT | | Horário da primeira toma diária |
| data_inicio | TEXT | | Data de início do tratamento |
| data_fim | TEXT | | Data de término do tratamento |

### Tabela: `consulta`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único da consulta |
| paciente_id | INTEGER | FK | Paciente que vai realizar a consulta |
| medico_id | INTEGER | FK | Médico responsável pelo atendimento |
| local | TEXT | | Local, clínica ou consultório |
| data_hora | TEXT | | Data e hora agendada |
| status | TEXT | | Estado atual (ex: AGENDADA, REALIZADA, CANCELADA) |

### Tabela: `lembrete`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único do lembrete |
| tipo | TEXT | NOT NULL, CHECK | Tipo de evento (`MEDICAMENTO` ou `CONSULTA`)[cite: 4] |
| medicamento_id | INTEGER | FK, ON DELETE CASCADE | Referência opcional ao medicamento |
| consulta_id | INTEGER | FK, ON DELETE CASCADE | Referência opcional à consulta |
| horario_previsto | TEXT | NOT NULL | Data e hora prevista para o alerta |
| status | TEXT | NOT NULL, CHECK | Estado (`PENDENTE`, `CONFIRMADO`, `PERDIDO`)[cite: 4] |
| alerta_enviado | INTEGER | NOT NULL, CHECK | Indicador booleano (0 ou 1) se o alerta foi disparado[cite: 4] |

### Tabela: `historico`
| Coluna | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| id | INTEGER | PK, AUTOINCREMENT | Identificador único do evento no histórico |
| lembrete_id | INTEGER | NOT NULL, FK, CASCADE | Lembrete associado ao evento |
| tipo_evento | TEXT | NOT NULL, CHECK | Tipo de evento registado (`GERADO`, `CONFIRMADO`, etc.)[cite: 4] |
| data_hora | TEXT | NOT NULL | Momento exato em que o evento ocorreu |