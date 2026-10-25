# Padrão de Revisão e Merge de PR (Testes e Qualidade)

## Fluxo de TDD com Pipeline Obrigatória
1. **Red:** Testes e qualidade cria a branch e faz o commit `test:` com o teste que falha (pode abrir PR como *Draft*).
2. **Green:** Dev principal faz commits `feat:` na mesma branch até os testes passarem.
3. **Refactor:** Refatoração (`refactor:`), pipeline fica verde, PR é aprovado e recebe merge (usando *Create a merge commit*, nunca *Squash*).

## Prefixos de Comentários
- `[bloqueante]` — Precisa ser corrigido para aprovar
- `[sugestão]` — Melhoria desejável, não impede o merge
- `[dúvida]` — Pergunta que precisa de resposta
- `[elogio]` — Algo bem feito

## Modelo de Comentário Final de Aprovação
```text
## Revisão
- [x] Triagem (branch, título, descrição, tamanho)
- [x] Pipeline verde
- [x] Código: camadas, erros, Clean Code, indentação
- [x] Testes: cobrem a regra e os casos de borda
- [x] Git: commits semânticos e branch atualizada

Rodei `mvn test` localmente: OK.
[elogio] ...
[sugestão] ...

Aprovado. Pode seguir para o merge.