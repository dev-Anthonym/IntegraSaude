package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.exception.ServicoExternoException;
import com.IntegraSaude.model.Endereco;
import org.junit.jupiter.api.Test;

/** ViaCEP (opcional): usa um cliente falso, sem internet. */
class EnderecoServiceTest {

    private final EnderecoService servico = new EnderecoService(cep -> {
        if (cep.equals("00000000")) {
            throw new RegistroNaoEncontradoException("CEP nao encontrado");
        }
        if (cep.equals("11111111")) {
            throw new ServicoExternoException("sem rede");
        }
        return new Endereco(cep, "Rua A", "Centro", "Belo Horizonte", "MG");
    });

    @Test
    void deveRejeitarCepInvalido() {
        assertThrows(DadoInvalidoException.class, () -> servico.buscarPorCep("123"));
    }

    @Test
    void deveAceitarCepComHifen() {
        assertEquals("30140071", servico.buscarPorCep("30140-071").cep());
    }

    @Test
    void deveTratarCepInexistente() {
        assertThrows(RegistroNaoEncontradoException.class, () -> servico.buscarPorCep("00000000"));
    }

    @Test
    void devePropagarFalhaDeRedeComoExcecaoPropria() {
        assertThrows(ServicoExternoException.class, () -> servico.buscarPorCep("11111111"));
    }
}
