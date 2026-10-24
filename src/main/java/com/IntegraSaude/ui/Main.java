package com.IntegraSaude.ui;

import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import com.IntegraSaude.service.NotificadorConsole;
import java.time.Clock;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        // Inicializa o Scanner apontando para a entrada padrão (System.in), permitindo capturar o teclado
        Scanner scanner = new Scanner(System.in);
        int opcao = 0; // Variável que vai armazenar a escolha feita pelo usuário no menu

        // Módulo de lembretes: medicamentos (Pessoa 2) e consultas (Pessoa 3) entram no lugar das listas vazias
        UsuarioRepository usuarioRepository = new UsuarioRepositoryMemoria();
        ModuloLembretes moduloLembretes = new ModuloLembretes(usuarioRepository,
                pacienteId -> List.of(), (medicamentoId, dia) -> List.of(), pacienteId -> List.of(),
                new NotificadorConsole(System.out), Clock.systemDefaultZone());
        MenuLembretes menuLembretes = moduloLembretes.criarMenu(scanner, System.out);
        moduloLembretes.iniciar(); // Inicia o agendador que verifica os lembretes em segundo plano

        // Estrutura de repetição 'do-while': garante que o menu será exibido 
        // pelo menos uma vez e continuará rodando até que o usuário digite 0 para sair
        do {
            // Exibição visual do menu principal do sistema no console
            System.out.println("\n=== INTEGRA SAÚDE - CONTROLE DE MEDICAÇÃO ===");
            System.out.println("1. Cadastrar Usuário (Em breve)");
            System.out.println("2. Fazer Login (Em breve)");
            System.out.println("3. Vincular Responsável (Em breve)");
            System.out.println("4. Ver lembretes pendentes");
            System.out.println("5. Confirmar lembrete (tomei / compareci)");
            System.out.println("6. Ver histórico de lembretes");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");

            // Validação de segurança: verifica se o que o usuário digitou é realmente um número inteiro,
            // evitando que o programa quebre caso ele digite letras ou símbolos por engano.
            if (scanner.hasNextInt()) {
                opcao = scanner.nextInt(); // Lê o número inteiro digitado
                scanner.nextLine(); // Limpa o "enter" pendente (buffer) deixado pelo nextInt()

                // Estrutura de decisão 'switch': direciona o fluxo do programa 
                // com base no número de opção que o usuário escolheu
                switch (opcao) {
                    case 1:
                        System.out.println("\n[Funcionalidade de Cadastro será implementada na Etapa 2]");
                        break;
                    case 2:
                        System.out.println("\n[Funcionalidade de Login será implementada na Etapa 3]");
                        break;
                    case 3:
                        System.out.println("\n[Funcionalidade de Vínculo será implementada na Etapa 4]");
                        break;
                    case 4:
                        menuLembretes.verPendentes();
                        break;
                    case 5:
                        menuLembretes.confirmarLembrete();
                        break;
                    case 6:
                        menuLembretes.verHistorico();
                        break;
                    case 0:
                        System.out.println("\nEncerrando o sistema. Até logo!");
                        break;
                    default: // Caso o usuário digite um número que não existe no menu (ex: 9)
                        System.out.println("\nOpção inválida! Escolha uma opção do menu.");
                }
            } else {
                // Bloco executado caso o usuário digite texto em vez de um número
                System.out.println("\nErro: Por favor, digite um número válido.");
                scanner.next(); // Descarta a entrada inválida do buffer para evitar loop infinito
                opcao = -1; // Mantém a opção diferente de 0 para o menu continuar rodando
            }

        } while (opcao != 0); // Condição de parada do loop: repete enquanto a opção for diferente de 0

        moduloLembretes.encerrar(); // Encerra o agendador de forma limpa antes de sair
        scanner.close(); // Fecha o Scanner ao encerrar o programa para liberar recursos do sistema
    }
}