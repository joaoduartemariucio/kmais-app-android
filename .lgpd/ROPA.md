# Registro das Operações de Tratamento (ROPA) — Rodaro Android

Formato simplificado para agente de tratamento de pequeno porte (Res. CD/ANPD nº 2/2022, Art. 9º).
Base: LGPD Art. 37. Versão 1 — 2026-11-06. Revisar a cada SDK, permissão ou função nova.

**Controlador:** João Vitor Duarte Mariucio (pessoa natural) — joaovitorduartemariucio@gmail.com
**Canal do titular / encarregado:** o mesmo e-mail (Art. 41; Res. 2/2022 Art. 11)
**Dados sensíveis (Art. 11):** nenhum. **Menores:** app não direcionado (Art. 14).

| # | Operação | Titulares | Dados | Finalidade | Base legal | Onde / operador | Transf. internacional | Retenção |
|---|---|---|---|---|---|---|---|---|
| 1 | Registros de veículos, abastecimentos, serviços, lembretes | usuário | apelido, placa (opcional), km, litros, preços, custos, observações livres | controle de manutenção e custos | n/a — dados só no aparelho; o controlador não tem acesso | Room local | não | até o usuário apagar |
| 2 | Rotas salvas | usuário | texto de origem/destino, traçado, custos | histórico de viagens | n/a — só no aparelho | Room local | não | até o usuário apagar |
| 3 | Busca de lugares e cálculo de rota | usuário | texto de origem/destino, coordenadas, IP | sugerir lugares e calcular distância | execução de serviço pedido (Art. 7º, V) | OpenRouteService — HeiGIT gGmbH | Alemanha (UE) | não guardado pelo app; logs técnicos do operador |
| 4 | Mapa da rota | usuário | área do mapa, IP | desenhar a rota | execução de serviço pedido (Art. 7º, V) | OpenFreeMap | exterior | idem |
| 5 | Conta opcional | usuário | e-mail, nome (Google), uid, hash de senha | manter o login | execução de serviço pedido (Art. 7º, V) | Firebase Authentication — Google LLC | EUA | até excluir a conta |
| 6 | Estatísticas de uso | usuário | eventos, telas, ID de instalação, aparelho, região aproximada, ID de publicidade | melhorar o app | legítimo interesse (Art. 7º, IX) com oposição no app | Google Analytics for Firebase | EUA | 2 ou 14 meses (config. do projeto) |
| 7 | Relatórios de falha e desempenho | usuário | stack trace, estado do aparelho, ID de instalação, tempos | corrigir falhas | legítimo interesse (Art. 7º, IX) com oposição no app | Crashlytics, Performance — Google | EUA | 90 dias (Crashlytics) |
| 8 | Notificações push | usuário | token FCM | avisos sobre o app | consentimento (permissão do Android; Art. 7º, I) | Firebase Cloud Messaging — Google | EUA | até apagar dados / desinstalar |
| 9 | Configuração remota | usuário | ID de instalação, versão, idioma, país | ligar/desligar funções | legítimo interesse (Art. 7º, IX) | Firebase Remote Config — Google | EUA | política do Google |
| 10 | Anúncios não personalizados | usuário | IP, aparelho, ID de publicidade (frequência/fraude), interações | manter o app gratuito | legítimo interesse (Art. 7º, IX) | Google AdMob | EUA | política do Google |
| 11 | Anúncios personalizados | usuário que ativou | + perfil de interesses pelo ID de publicidade | anúncios relevantes | consentimento (Art. 7º, I; revogável, Art. 8º, § 5º) | Google AdMob | EUA | política do Google |

## Medidas de segurança (Art. 46)

- Dados do usuário só no armazenamento privado do app; nada em servidor próprio.
- HTTPS para todos os serviços; logs de rede e de analytics só em build de debug, com chaves e
  headers sensíveis ocultos.
- Exclusão completa pelo app, com ou sem conta, incluindo reinício dos identificadores do Firebase.

## Pendências

- Contratos/termos de operador: aceitar os Data Processing Terms do Firebase/Google e do AdMob no
  console; verificar os termos da HeiGIT para o plano da chave ORS (`lgpd-vendor-audit`, `lgpd-dpa`).
- Revisão jurídica das bases legais 6, 9 e 10 (legítimo interesse) — ver `legal-basis.md`.
