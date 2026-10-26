package com.IntegraSaude;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.Repositorios;
import com.IntegraSaude.service.AgendaService;
import com.IntegraSaude.service.AlertaService;
import com.IntegraSaude.service.AutenticacaoService;
import com.IntegraSaude.service.CalculadoraHorarios;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import com.IntegraSaude.service.ConsultaService;
import com.IntegraSaude.service.EspecialidadeService;
import com.IntegraSaude.service.GeradorLembretes;
import com.IntegraSaude.service.HistoricoService;
import com.IntegraSaude.service.LembreteService;
import com.IntegraSaude.service.MedicamentoService;
import com.IntegraSaude.service.MedicoService;
import com.IntegraSaude.service.SenhaService;
import com.IntegraSaude.service.UsuarioService;
import com.IntegraSaude.service.ValidadorConsulta;
import com.IntegraSaude.service.ValidadorMedicamento;
import com.IntegraSaude.service.ValidadorUsuario;
import com.IntegraSaude.service.VinculoService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Monta o sistema inteiro com repositórios em memória e relógio controlado.
 * "Agora" é segunda-feira, 23/11/2026, às 10:00.
 */
public class CenarioTeste {

    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    public static final String SENHA_VALIDA = "Senha123";

    public final RelogioAjustavel relogio = new RelogioAjustavel(AGORA);
    public final Repositorios repos;
    public final NotificadorFalso notificador = new NotificadorFalso();
    public final SenhaService senhas = new SenhaService();
    public final UsuarioService usuarios;
    public final AutenticacaoService autenticacao;
    public final VinculoService vinculos;
    public final CalculadoraHorarios calculadora = new CalculadoraHorarios();
    public final MedicamentoService medicamentos;
    public final EspecialidadeService especialidades;
    public final MedicoService medicos;
    public final ConsultaService consultas;
    public final HistoricoService historico;
    public final LembreteService lembretes;
    public final GeradorLembretes gerador;
    public final AlertaService alertas;
    public final AgendaService agenda;

    public CenarioTeste() {
        this(ConfiguracaoLembretes.padrao());
    }

    public CenarioTeste(ConfiguracaoLembretes configuracao) {
        this(configuracao, Repositorios.emMemoria());
    }

    // Permite montar o mesmo cenário com qualquer conjunto de repositórios (memória ou SQLite)
    public CenarioTeste(ConfiguracaoLembretes configuracao, Repositorios repos) {
        this.repos = repos;
        usuarios = new UsuarioService(repos.usuarios(), new ValidadorUsuario(), senhas);
        autenticacao = new AutenticacaoService(repos.usuarios(), senhas);
        vinculos = new VinculoService(repos.vinculos(), repos.usuarios());
        medicamentos = new MedicamentoService(repos.medicamentos(), repos.usuarios(), repos.lembretes(),
                new ValidadorMedicamento(relogio));
        especialidades = new EspecialidadeService(repos.especialidades());
        medicos = new MedicoService(repos.medicos(), repos.especialidades());
        consultas = new ConsultaService(repos.consultas(), repos.usuarios(), repos.lembretes(),
                new ValidadorConsulta(relogio), relogio);
        historico = new HistoricoService(repos.historico(), relogio);
        lembretes = new LembreteService(repos.lembretes(), historico, relogio, configuracao);
        gerador = new GeradorLembretes(repos.medicamentos(), repos.consultas(), repos.lembretes(),
                calculadora, historico, relogio, configuracao);
        alertas = new AlertaService(repos.lembretes(), vinculos, usuarios, historico, notificador);
        agenda = new AgendaService(repos.medicamentos(), repos.consultas(), calculadora);
    }

    public Usuario criarPaciente(String email) {
        return usuarios.cadastrar("Paciente " + email, email, SENHA_VALIDA, Perfil.PACIENTE);
    }

    public Usuario criarResponsavel(String email) {
        return usuarios.cadastrar("Responsavel " + email, email, SENHA_VALIDA, Perfil.RESPONSAVEL);
    }

    // Medicamento de 7 dias a partir de hoje
    public Medicamento medicamento(Long pacienteId, int frequenciaHoras, LocalTime primeiroHorario) {
        LocalDate hoje = AGORA.toLocalDate();
        return new Medicamento(pacienteId, "Dipirona", "500 mg", frequenciaHoras, primeiroHorario,
                hoje, hoje.plusDays(7));
    }

    public Medico criarMedico() {
        return medicos.cadastrar("Dr. Teste", especialidades.cadastrar("Cardiologia").getId());
    }

    public Consulta consulta(Long pacienteId, Medico medico, LocalDateTime dataHora) {
        return new Consulta(pacienteId, medico, "Clinica Central", dataHora);
    }
}
