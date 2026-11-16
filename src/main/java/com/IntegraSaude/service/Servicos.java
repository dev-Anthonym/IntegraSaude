package com.IntegraSaude.service;

import com.IntegraSaude.repository.Repositorios;
import java.time.Clock;

/**
 * Agrupa todos os serviços já ligados aos repositórios. É a mesma "fiação" usada pelo menu de
 * console e pelo frontend web, então as duas interfaces seguem exatamente as mesmas regras.
 */
public record Servicos(
        UsuarioService usuarios,
        AutenticacaoService autenticacao,
        VinculoService vinculos,
        CalculadoraHorarios calculadora,
        MedicamentoService medicamentos,
        EspecialidadeService especialidades,
        MedicoService medicos,
        ConsultaService consultas,
        AgendaService agenda,
        HistoricoService historico,
        LembreteService lembretes,
        GeradorLembretes gerador,
        AlertaService alertas,
        EnderecoService enderecos,
        ConfiguracaoLembretes configuracao,
        Clock relogio) {

    // Cria todos os serviços com os repositórios, o relógio e o canal de aviso recebidos
    public static Servicos montar(Repositorios repos, ConfiguracaoLembretes configuracao, Clock relogio,
                                  Notificador notificador, EnderecoClient clienteCep) {
        // Usuários e vínculos
        SenhaService senhas = new SenhaService();
        UsuarioService usuarios = new UsuarioService(repos.usuarios(), new ValidadorUsuario(), senhas);
        AutenticacaoService autenticacao = new AutenticacaoService(repos.usuarios(), senhas);
        VinculoService vinculos = new VinculoService(repos.vinculos(), repos.usuarios());

        // Medicamentos, consultas e agenda
        CalculadoraHorarios calculadora = new CalculadoraHorarios();
        MedicamentoService medicamentos = new MedicamentoService(repos.medicamentos(), repos.usuarios(),
                repos.lembretes(), new ValidadorMedicamento(relogio));
        EspecialidadeService especialidades = new EspecialidadeService(repos.especialidades());
        MedicoService medicos = new MedicoService(repos.medicos(), repos.especialidades());
        ConsultaService consultas = new ConsultaService(repos.consultas(), repos.usuarios(),
                repos.lembretes(), new ValidadorConsulta(relogio), relogio);
        AgendaService agenda = new AgendaService(repos.medicamentos(), repos.consultas(), calculadora);

        // Lembretes, alerta e histórico
        HistoricoService historico = new HistoricoService(repos.historico(), relogio);
        LembreteService lembretes = new LembreteService(repos.lembretes(), historico, relogio, configuracao);
        GeradorLembretes gerador = new GeradorLembretes(repos.medicamentos(), repos.consultas(),
                repos.lembretes(), calculadora, historico, relogio, configuracao);
        AlertaService alertas = new AlertaService(repos.lembretes(), vinculos, usuarios, historico, notificador);

        return new Servicos(usuarios, autenticacao, vinculos, calculadora, medicamentos, especialidades,
                medicos, consultas, agenda, historico, lembretes, gerador, alertas,
                new EnderecoService(clienteCep), configuracao, relogio);
    }
}
