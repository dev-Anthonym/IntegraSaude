package com.IntegraSaude.repository;

/**
 * Agrupa todos os repositórios em um único objeto.
 * Trocar memória por SQLite é trocar a fábrica usada no Aplicacao (uma linha).
 */
public record Repositorios(
        UsuarioRepository usuarios,
        VinculoRepository vinculos,
        MedicamentoRepository medicamentos,
        EspecialidadeRepository especialidades,
        MedicoRepository medicos,
        ConsultaRepository consultas,
        LembreteRepository lembretes,
        HistoricoRepository historico) {

    // Fábrica com as versões em memória (testes e modo --memoria)
    public static Repositorios emMemoria() {
        return new Repositorios(
                new UsuarioRepositoryMemoria(),
                new VinculoRepositoryMemoria(),
                new MedicamentoRepositoryMemoria(),
                new EspecialidadeRepositoryMemoria(),
                new MedicoRepositoryMemoria(),
                new ConsultaRepositoryMemoria(),
                new LembreteRepositoryMemoria(),
                new HistoricoRepositoryMemoria());
    }
}
