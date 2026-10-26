package com.IntegraSaude.service;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Gera e confere hash de senha com PBKDF2 (já vem no JDK) e salt aleatório.
 * O texto guardado tem o formato "salt:hash", os dois em Base64.
 */
public class SenhaService {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_SALT_BYTES = 16;
    private static final int TAMANHO_HASH_BITS = 256;
    private static final String SEPARADOR = ":";

    private final SecureRandom aleatorio = new SecureRandom();

    // Gera um salt novo, calcula o hash e devolve "salt:hash" em Base64
    public String gerarHash(String senha) {
        byte[] salt = new byte[TAMANHO_SALT_BYTES];
        aleatorio.nextBytes(salt);
        byte[] hash = calcular(senha, salt);
        return Base64.getEncoder().encodeToString(salt) + SEPARADOR
                + Base64.getEncoder().encodeToString(hash);
    }

    // Recalcula o hash com o mesmo salt e compara em tempo constante
    public boolean conferir(String senha, String armazenado) {
        if (senha == null || armazenado == null || !armazenado.contains(SEPARADOR)) {
            return false;
        }
        String[] partes = armazenado.split(SEPARADOR);
        byte[] salt = Base64.getDecoder().decode(partes[0]);
        byte[] esperado = Base64.getDecoder().decode(partes[1]);
        byte[] calculado = calcular(senha, salt);
        return MessageDigest.isEqual(esperado, calculado);
    }

    // Executa o PBKDF2 e limpa a senha da memória logo depois
    private byte[] calcular(String senha, byte[] salt) {
        char[] caracteres = senha.toCharArray();
        PBEKeySpec especificacao = new PBEKeySpec(caracteres, salt, ITERACOES, TAMANHO_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new OperacaoInvalidaException("Nao foi possivel proteger a senha", e);
        } finally {
            especificacao.clearPassword();
            Arrays.fill(caracteres, '\0');
        }
    }
}
