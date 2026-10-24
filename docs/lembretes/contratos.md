# Contratos do módulo Lembretes e Alertas (Pessoa 4)

Este documento registra como o módulo de lembretes conversa com os módulos das
outras pessoas. O módulo programa contra **interfaces próprias** (portas), de modo
que funciona e é testado mesmo antes de os outros módulos ficarem prontos. Quando
cada módulo estiver pronto, basta escrever um adaptador pequeno que implemente a
porta correspondente.

## Pessoa 2: medicamentos e cálculo de horários

Portas do módulo de lembretes (pacote `com.IntegraSaude.service`):

```java
public interface FonteMedicamentos {
    List<MedicamentoAgendado> listarDoPaciente(Long pacienteId);
}

public interface HorariosMedicamento {
    List<LocalTime> horariosDoDia(Long medicamentoId, LocalDate dia);
}
```

- `MedicamentoAgendado(id, nome, inicio, fim)`: `fim` nulo significa uso contínuo.
- O adaptador de `HorariosMedicamento` deve delegar para
  `CalculadoraHorarios.horariosDoDia(medicamento, dia)`.
- `CalculadoraHorarios.proximoHorario(medicamento, agora)` não é usado pelo
  módulo de lembretes; os lembretes são gerados por dia.
- O gerador já ignora medicamentos fora do período (`inicio`/`fim`), então a
  calculadora não precisa tratar isso.

## Pessoa 3: consultas

```java
public interface FonteConsultas {
    List<ConsultaAgendada> listarDoPaciente(Long pacienteId);
}
```

- `ConsultaAgendada(id, descricao, dataHora)`: `dataHora` é um `LocalDateTime`
  com a data e a hora da consulta.
- O lembrete de consulta é gerado em `dataHora - antecedência` (RN10, padrão de
  60 minutos).

## Pessoa 1: vínculo paciente/responsável

```java
public interface BuscaResponsaveis {
    List<Usuario> listarResponsaveisDoPaciente(Long pacienteId);
}
```

- Enquanto o `VinculoService` não tiver `listarResponsaveisDoPaciente`, o
  adaptador `ResponsaveisPorVinculo` lê o `responsavelId` do paciente pelo
  `UsuarioRepository`.
- Lista vazia significa "sem vínculo": nenhum alerta é enviado e não é erro (RN06).
- O nome do paciente usado na mensagem de alerta vem de
  `UsuarioRepository.buscarPorId`.

## Banco de dados (Pessoa 2)

- O DDL do módulo fica em `database/04_lembretes.sql` e depende das tabelas
  `usuario(id)`, `medicamento(id)` e `consulta(id)`.
- Os repositórios JDBC recebem uma `FabricaConexao` (`Connection abrir()`) e
  fecham a conexão ao fim de cada operação (try-with-resources). Se a conexão da
  Pessoa 2 for compartilhada, o adaptador deve devolver uma conexão nova a cada
  chamada.

## Quando os lembretes são gerados

`GeradorLembretes.gerarDoDia(pacienteId, dia)` é idempotente (RN08), então pode
ser chamado sempre que necessário. Hoje o menu chama a geração do dia ao listar
os lembretes pendentes do paciente. Os módulos de medicamento e consulta também
podem chamá-la logo após um cadastro.

## Configuração

| Propriedade do sistema               | Padrão | Uso                                    |
|--------------------------------------|--------|----------------------------------------|
| `lembretes.tempoLimiteMinutos`       | 30     | Minutos até o lembrete virar PERDIDO   |
| `lembretes.antecedenciaConsultaMinutos` | 60  | Antecedência do lembrete de consulta   |
| `lembretes.intervaloVerificacaoSegundos` | 30 | Intervalo do agendador                 |

Para a apresentação, rode com `-Dlembretes.tempoLimiteMinutos=1` para mostrar o
alerta sem esperar 30 minutos.
