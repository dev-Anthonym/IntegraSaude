package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Endereco;

/** Valida o CEP antes de chamar a API externa. */
public class EnderecoService {

    public static final int TAMANHO_CEP = 8;

    private final EnderecoClient cliente;

    public EnderecoService(EnderecoClient cliente) {
        this.cliente = cliente;
    }

    // Aceita "30140-071" ou "30140071"; rejeita qualquer coisa que não tenha 8 dígitos
    public Endereco buscarPorCep(String cep) {
        String digitos = cep == null ? "" : cep.replaceAll("[^0-9]", "");
        if (digitos.length() != TAMANHO_CEP) {
            throw new DadoInvalidoException("O CEP deve ter " + TAMANHO_CEP + " digitos");
        }
        return cliente.buscar(digitos);
    }
}
