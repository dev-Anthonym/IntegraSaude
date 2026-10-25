# IntegraSaude 

\## Como contribuir



\### Fluxo de branches (Git Flow)



| Branch | Para que serve | Criada a partir de | Volta para |

| --- | --- | --- | --- |

| `main` | Versão estável entregue | - | - |

| `develop` | Integração do trabalho de todos | `main` | - |

| `feature/<nome>` | Nova funcionalidade | `develop` | `develop` (via Pull Request) |

| `refactor/<nome>` | Melhoria de código sem mudar comportamento | `develop` | `develop` (via Pull Request) |

| `hotfix/<nome>` | Correção urgente | `main` | `main` e `develop` |

| `release/<versao>` | Preparação de uma versão | `develop` | `main` e `develop` |



Regras:



1\. Nunca faça commit direto em `main` ou `develop`; abra um Pull Request.

2\. Antes do PR, rode `mvn test`.

3\. O PR precisa de 1 aprovação e da pipeline verde.

4\. Apague a branch depois do merge.



\### Commits semânticos



Formato: `tipo: descricao curta no imperativo`



| Tipo | Quando usar |

| --- | --- |

| `feat` | Nova funcionalidade |

| `fix` | Correção de bug |

| `test` | Criação ou ajuste de testes |

| `refactor` | Refatoração sem mudar comportamento |

| `docs` | Documentação |

| `chore` | Configuração e tarefas de manutenção |

| `ci` | Pipeline de integração contínua |



Exemplo: `feat: implementa servico de consultas`



