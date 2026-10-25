# Documentação dos Scripts SQL (DDL, DML e DQL) - IntegraSaúde

Este documento descreve a organização dos scripts de criação, população e consulta da base de dados SQLite do projeto **IntegraSaúde**, localizados na pasta `database/` e consolidados no script final de entrega[cite: 1].

## 1. Organização dos Módulos DDL (`database/`)
Os scripts de criação de tabelas foram divididos por módulos para garantir a ordem correta das dependências de chaves estrangeiras:
- `01_usuarios.sql`: Criação das tabelas `usuario` e `vinculo`, com restrições de e-mail único e verificação de perfis[cite: 1].
- `02_medicamentos.sql`: Criação da tabela `medicamento` e regras de frequência de toma[cite: 1].
- `03_consultas.sql`: Criação das tabelas `especialidade` e `medico`, além da tabela `consulta` e do índice único parcial para evitar sobreposição de horários[cite: 1].
- `04_lembretes.sql`: Criação das tabelas `lembrete` e `historico`, com suporte a exclusão em cascata (`ON DELETE CASCADE`)[cite: 1].
- `05_alteracoes.sql`: Demonstração de alteração estrutural com o comando `ALTER TABLE` (ex: adição de campo de telefone)[cite: 1].

## 2. Carga de Dados e Demonstração (DML)
- `06_dados_demonstracao.sql`: Contém instruções `INSERT`, `UPDATE` e `DELETE` com dados realistas (pacientes, responsáveis, médicos, medicamentos e histórico de lembretes nos três estados: pendente, confirmado e perdido)[cite: 1].

## 3. Consultas Analíticas (DQL)
- `07_consultas_dql.sql`: Reúne as consultas estratégicas com `JOIN` e `UNION ALL` para relatórios de adesão e agenda diária[cite: 1].

## 4. Script Único de Entrega
O ficheiro consolidado `database/entrega/lembretes_completo.sql` une todos os scripts acima numa única execução sequencial, iniciando com a ativação das chaves estrangeiras[cite: 1]:

```sql
PRAGMA foreign_keys = ON;

-- [Conteúdo consolidado de 01 a 07: DDL, DML e DQL]