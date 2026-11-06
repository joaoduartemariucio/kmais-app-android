# Bases legais e Teste de Legítimo Interesse (LIA) — Rodaro Android

Versão 1 — 2026-11-06. Base: LGPD Art. 7º, Art. 10 (§ 2º transparência, § 3º relatório).
Não substitui parecer jurídico.

## Resumo

| Operação (ROPA) | Base | Artigo |
|---|---|---|
| 3, 4, 5 — rota, mapa, conta | execução de serviço pedido pelo titular | Art. 7º, V |
| 6, 7 — estatísticas, falhas, desempenho | legítimo interesse, com oposição em Preferências | Art. 7º, IX; Art. 18, § 2º |
| 8 — push | consentimento (permissão do Android) | Art. 7º, I |
| 9 — Remote Config | legítimo interesse | Art. 7º, IX |
| 10 — anúncios não personalizados | legítimo interesse | Art. 7º, IX |
| 11 — anúncios personalizados | consentimento opt-in, revogável | Art. 7º, I; Art. 8º, § 5º |

## LIA — estatísticas, relatórios de falha e desempenho (operações 6 e 7)

1. **Finalidade legítima:** saber quais funções são usadas e corrigir travamentos de um app
   gratuito sem servidor próprio. Concreta e atual (Art. 10, I).
2. **Necessidade:** eventos sem dados pessoais nos parâmetros (só booleanos, método de login,
   chave de preferência); nenhum registro do usuário é enviado; Crashlytics desligado em debug
   (Art. 6º, III).
3. **Expectativa do titular:** prática comum e informada na política (seção 4) e na tela de
   Preferências.
4. **Balanceamento:** impacto baixo — dados técnicos, sem nome/e-mail, sem perfilamento. O ID de
   publicidade coletado pelo Analytics é o ponto mais sensível; mitigado pela oposição no app.
5. **Salvaguardas:** chave "Estatísticas de uso e relatórios de falha" desliga Analytics,
   Crashlytics e Performance; apagar dados reinicia os identificadores; retenção do Analytics
   configurada no projeto.
6. **Conclusão:** legítimo interesse adequado.

## LIA — anúncios não personalizados e Remote Config (operações 9 e 10)

1. **Finalidade:** manter o app gratuito; ligar/desligar funções sem nova versão.
2. **Necessidade:** pedidos de anúncio com `npa=1`; o ID de publicidade só serve para limitar
   frequência, medir e evitar fraude.
3. **Expectativa:** app gratuito com anúncios é prática esperada; informada na seção 5.
4. **Balanceamento:** sem perfil de interesses; impacto baixo.
5. **Salvaguardas:** o titular pode excluir o ID de publicidade no Android.
6. **Conclusão:** legítimo interesse adequado. Perfilamento publicitário fica sob consentimento
   (operação 11).

## Para revisão jurídica

- O Consent Mode do Analytics já nega `AD_STORAGE`, `AD_USER_DATA` e `AD_PERSONALIZATION`
  enquanto o titular não ativa anúncios personalizados. Confirmar se isso basta ou se a coleta do
  ID de publicidade pelo Analytics deve ser desligada de vez
  (`google_analytics_adid_collection_enabled = false` no manifest).
