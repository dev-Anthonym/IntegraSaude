package com.IntegraSaude.service;

import com.IntegraSaude.model.Usuario;
import java.util.List;

// Porta para o vínculo paciente/responsável (Pessoa 1)
public interface BuscaResponsaveis {
    List<Usuario> listarResponsaveisDoPaciente(Long pacienteId);
}
