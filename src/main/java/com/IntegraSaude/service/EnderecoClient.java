package com.IntegraSaude.service;

import com.IntegraSaude.model.Endereco;

/** Contrato para buscar endereço por CEP. Nos testes usa-se uma versão falsa (sem internet). */
public interface EnderecoClient {

    // Recebe só os 8 dígitos. CEP inexistente: RegistroNaoEncontradoException. Rede fora: ServicoExternoException
    Endereco buscar(String cep);
}
