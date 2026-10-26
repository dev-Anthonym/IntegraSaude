package com.IntegraSaude.ui;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.ConsultaService;
import com.IntegraSaude.service.EspecialidadeService;
import com.IntegraSaude.service.MedicamentoService;
import com.IntegraSaude.service.MedicoService;
import com.IntegraSaude.service.UsuarioService;
import com.IntegraSaude.service.VinculoService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Carrega dados fictícios para a apresentação não depender de digitar tudo ao vivo.
 * O primeiro horário do remédio é "daqui a 1 minuto", para o lembrete aparecer logo.
 */
public class DadosDemonstracao {

    public static final String EMAIL_PACIENTE = "maria@demo.com";
    public static final String EMAIL_RESPONSAVEL = "joao@demo.com";
    public static final String SENHA = "Senha123";

    private static final int FREQUENCIA_DEMO_HORAS = 8;
    private static final int DIAS_DE_TRATAMENTO = 7;
    private static final int HORA_CONSULTA = 9;

    private final UsuarioService usuarios;
    private final VinculoService vinculos;
    private final MedicamentoService medicamentos;
    private final EspecialidadeService especialidades;
    private final MedicoService medicos;
    private final ConsultaService consultas;
    private final Clock relogio;

    public DadosDemonstracao(UsuarioService usuarios, VinculoService vinculos, MedicamentoService medicamentos,
                             EspecialidadeService especialidades, MedicoService medicos,
                             ConsultaService consultas, Clock relogio) {
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.medicamentos = medicamentos;
        this.especialidades = especialidades;
        this.medicos = medicos;
        this.consultas = consultas;
        this.relogio = relogio;
    }

    // Cria usuários, vínculo, medicamento e consulta. Devolve o texto com os dados de acesso
    public String carregar() {
        Usuario paciente;
        try {
            paciente = usuarios.cadastrar("Maria Paciente", EMAIL_PACIENTE, SENHA, Perfil.PACIENTE);
        } catch (RegistroDuplicadoException e) {
            return "Os dados de demonstracao ja foram carregados antes.";
        }
        Usuario responsavel = usuarios.cadastrar("Joao Responsavel", EMAIL_RESPONSAVEL, SENHA, Perfil.RESPONSAVEL);
        vinculos.criar(paciente.getId(), responsavel.getId());
        cadastrarMedicamentoDeDemonstracao(paciente);
        cadastrarConsultaDeDemonstracao(paciente);
        return "Dados carregados.\n"
                + "  Paciente:    " + EMAIL_PACIENTE + " / " + SENHA + "\n"
                + "  Responsavel: " + EMAIL_RESPONSAVEL + " / " + SENHA;
    }

    // Remédio com primeiro horário daqui a 1 minuto
    private void cadastrarMedicamentoDeDemonstracao(Usuario paciente) {
        LocalDateTime primeiraDose = LocalDateTime.now(relogio).plusMinutes(1).truncatedTo(ChronoUnit.MINUTES);
        medicamentos.cadastrar(new Medicamento(paciente.getId(), "Dipirona", "500 mg", FREQUENCIA_DEMO_HORAS,
                primeiraDose.toLocalTime(), primeiraDose.toLocalDate(),
                primeiraDose.toLocalDate().plusDays(DIAS_DE_TRATAMENTO)));
    }

    // Consulta amanhã às 09:00
    private void cadastrarConsultaDeDemonstracao(Usuario paciente) {
        Especialidade cardiologia = especialidades.cadastrar("Cardiologia");
        Medico medico = medicos.cadastrar("Dr. Carlos Silva", cardiologia.getId());
        LocalDateTime amanha = LocalDateTime.now(relogio).plusDays(1)
                .withHour(HORA_CONSULTA).withMinute(0).withSecond(0).withNano(0);
        consultas.cadastrar(new Consulta(paciente.getId(), medico, "Clinica Saude, Belo Horizonte", amanha));
    }
}
