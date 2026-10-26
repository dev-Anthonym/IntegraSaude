# Roteiro Git Flow e commits por etapa

Preparação (Etapa 1):
```bash
git checkout main && git checkout -b develop && git push -u origin develop
git checkout -b feature/base-projeto develop
```
Depois de cada etapa: `mvn test` verde, PR para `develop` (revisado pelo responsável por testes).

| Etapa | Branch | Pacotes/arquivos | Commits sugeridos |
|---|---|---|---|
| 1-2 | feature/base-projeto | pom.xml, .gitignore, .editorconfig | chore: ajusta gitignore para maven e sqlite / chore: cria projeto maven com junit 5 e sqlite |
| 3 | feature/base-projeto | exception/ | feat: adiciona hierarquia de excecoes da aplicacao |
| 4 | feature/modelo-dominio | model/ | feat: adiciona enums do dominio / feat: adiciona entidades ... |
| 5 | feature/validacoes | service/Validador* | feat: implementa validacao de usuario / medicamento / consulta |
| 6 | feature/repositorios-memoria | repository/ | feat: define interfaces dos repositorios / feat: implementa repositorios em memoria |
| 7 | feature/usuarios-login-vinculo | SenhaService, UsuarioService, AutenticacaoService, SessaoUsuario, VinculoService | feat: adiciona hash de senha com pbkdf2 ... |
| 8 | feature/medicamentos-horarios | MedicamentoService, CalculadoraHorarios | feat: calcula horarios do dia por frequencia |
| 9 | feature/consultas | ConsultaService, EspecialidadeService, MedicoService | feat: implementa servico de consultas |
| 10 | feature/lembretes | LembreteService, GeradorLembretes | feat: implementa servico de lembretes |
| 11 | feature/alerta-historico | Notificador, AlertaService, HistoricoService | feat: implementa alerta ao responsavel |
| 12 | feature/agenda | AgendaService | feat: implementa agenda do dia e da semana |
| 13 | feature/agendador | scheduler/ | feat: cria agendador de verificacao de lembretes |
| 14 | feature/menu-console | ui/ | feat: adiciona menu de usuarios e vinculo ... |
| 15 | feature/integracao-sqlite | repository/jdbc, database/, Aplicacao | feat: integra repositorios jdbc ao sistema |
| 16 | feature/tratamento-erros | config/ConfiguracaoLog | feat: registra erros inesperados em log |
| 17 | refactor/limpeza-geral | geral | refactor: ... (um commit por melhoria) |
| 18 | feature/viacep | integracao/, EnderecoService | feat: cria cliente da api viacep |
| 19 | feature/demonstracao | ConfiguracaoApp, DadosDemonstracao | feat: adiciona carga de dados de demonstracao |
| 20 | release/1.0.0 | - | chore: prepara versao 1.0.0 |

Release:
```bash
git checkout -b release/1.0.0 develop
git checkout main && git merge --no-ff release/1.0.0
git tag -a v1.0.0 -m "Versao 1.0.0"
git checkout develop && git merge --no-ff release/1.0.0
git push origin main develop --tags
```
