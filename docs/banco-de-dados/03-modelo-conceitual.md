# Modelo Conceitual (DER) - IntegraSaúde

## 1. Introdução ao Diagrama de Entidade-Relacionamento (DER)
O modelo conceitual representa a alta abstração dos dados do sistema **IntegraSaúde**, focando-se nas entidades do mundo real necessárias para cumprir os objetivos do ODS 3 (saúde e bem-estar). O diagrama conceptual foi desenhado em ferramenta externa (como draw.io ou dbdiagram.io) e a imagem de alta resolução encontra-se exportada em `docs/banco-de-dados/img/der-conceitual.png`[cite: 3].

## 2. Entidades e Atributos Principais

* **Usuário (`usuario`):**
  * **Atributos:** `id` (Chave Primária), `nome`, `email` (Único), `senha_hash`, `perfil` (`PACIENTE` ou `RESPONSAVEL`), `data_cadastro`[cite: 3].
* **Medicamento (`medicamento`):**
  * **Atributos:** `id` (Chave Primária), `paciente_id` (Chave Estrangeira), `nome`, `dosagem`, `frequencia_horas`, `primeiro_horario`, `data_inicio`, `data_fim`[cite: 3].
* **Especialidade (`especialidade`):**
  * **Atributos:** `id` (Chave Primária), `nome` (Único)[cite: 3].
* **Médico (`medico`):**
  * **Atributos:** `id` (Chave Primária), `nome`, `especialidade_id` (Chave Estrangeira)[cite: 3].
* **Consulta (`consulta`):**
  * **Atributos:** `id` (Chave Primária), `paciente_id` (Chave Estrangeira), `medico_id` (Chave Estrangeira), `local`, `data_hora`, `status`[cite: 3].
* **Lembrete (`lembrete`):**
  * **Atributos:** `id` (Chave Primária), `tipo`, `medicamento_id` (Chave Estrangeira opcional), `consulta_id` (Chave Estrangeira opcional), `horario_previsto`, `status`, `alerta_enviado`[cite: 3].
* **Histórico (`historico`):**
  * **Atributos:** `id` (Chave Primária), `lembrete_id` (Chave Estrangeira), `tipo_evento`, `data_hora`[cite: 3].

## 3. Relacionamentos e Cardinalidades

| Relacionamento | Cardinalidade | Descrição / Observação |
| :--- | :--- | :--- |
| **Usuário (Paciente) ↔ Usuário (Responsável)** | `N:N` | Resolvido através da entidade associativa **Vínculo** (`vinculo`), permitindo que um paciente tenha múltiplos responsáveis e vice-versa[cite: 3]. |
| **Paciente ↔ Medicamento** | `1:N` | Um paciente pode registar vários medicamentos associados ao seu plano de tratamento[cite: 3]. |
| **Paciente ↔ Consulta** | `1:N` | Um paciente pode agendar e realizar várias consultas médicas[cite: 3]. |
| **Especialidade ↔ Médico** | `1:N` | Uma especialidade médica agrupa vários profissionais médicos[cite: 3]. |
| **Médico ↔ Consulta** | `1:N` | Um médico participa em várias consultas agendadas no sistema[cite: 3]. |
| **Medicamento ↔ Lembrete** | `1:N` | Cada medicamento gera múltiplos lembretes automáticos de toma ao longo do tempo[cite: 3]. |
| **Consulta ↔ Lembrete** | `1:N` | Cada consulta agendada pode originar lembretes de aviso prévio para o paciente[cite: 3]. |
| **Lembrete ↔ Histórico** | `1:N` | Um lembrete gera eventos registados no histórico (ex: gerado, confirmado, perdido)[cite: 3]. |