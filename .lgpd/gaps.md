# Gap Analysis e Plano de Remediação — 2026-11-06

Escopo: app Android `br.com.joaoduartemariucio.rodaro` + site `rodaro.web.app/privacidade`
(vigente desde 28/10/2026). Cenário B (legado em produção). Controlador: pessoa natural
(provável ATPP — Res. CD/ANPD nº 2/2022), sem servidor próprio.

> Diagnóstico técnico, não parecer jurídico. Os pontos marcados "decisão jurídica" pedem
> revisão de advogado especializado em proteção de dados.

## Resumo

- 3 gaps críticos/altos de transparência (política diz uma coisa, app faz outra)
- 3 gaps altos de direitos do titular e base legal
- 4 gaps médios
- 3 gaps baixos

## Quadro

| # | Item | Status | Evidência | Norma | Severidade |
|---|---|---|---|---|---|
| G1 | Rotas: política diz que "não são salvas" | RED | `TripRouteModel` grava `origin`, `destination` e `path` no Room (`data/trips/models/TripRouteModel.kt`) | Art. 6º, VI; Art. 9º, I e II | crítica |
| G2 | OpenRouteService não declarado como operador | RED | `OrsNetwork` envia o texto digitado de origem/destino (pode ser endereço residencial) a `api.openrouteservice.org` (HeiGIT, Alemanha); política diz que os registros não saem do aparelho e só lista o Google | Art. 9º, V; Art. 39; Arts. 33–36 | crítica |
| G3 | OpenFreeMap, Google Maps e Waze não declarados | RED | `RouteMapCard.kt` carrega tiles de `tiles.openfreemap.org` (IP + área da rota); `ExternalMaps.kt` abre Google Maps/Waze com origem/destino na URL | Art. 9º, V | alta |
| G4 | "Apagar tudo de uma vez" indisponível sem conta | RED | `ProfileScreen.PrivacySection`: botão `enabled = viewModel.account != null`. Quem não entrou não consegue usar o fluxo que a política (seção 2 e 11) promete | Art. 18, VI; Art. 6º, VI | alta |
| G5 | Sem opção de oposição a Analytics/Crashlytics/Performance | RED | Coleta sempre ligada em release (`FirebaseSetup.kt`); base declarada é legítimo interesse, mas não há como se opor no app | Art. 18, § 2º; Art. 10, § 2º | alta |
| G6 | Anúncios personalizados por legítimo interesse, sem consentimento | YELLOW | `MobileAds.initialize` sem UMP/consentimento; ID de publicidade usado para personalização. Legítimo interesse para perfilamento publicitário é frágil e exige LIA documentado | Art. 7º, IX; Art. 10; Art. 37 | alta — decisão jurídica |
| G7 | `AdMobGate` começa `true` | YELLOW | `AdMobGate.enabled = true` antes do Remote Config responder (default é `false`), então anúncios podem carregar mesmo com a flag desligada | Art. 6º, III (necessidade) | média |
| G8 | Exclusão de conta não limpa identificadores do Firebase | YELLOW | `clearLocalData` limpa Room/WorkManager/notificações, mas não `SharedPreferences`, `FirebaseAnalytics.resetAnalyticsData()`, `FirebaseInstallations.delete()` nem `FirebaseMessaging.deleteToken()` | Art. 16; Art. 18, VI | média |
| G9 | Sem link para a política na criação de conta | YELLOW | `AuthScreen.kt` não referencia a política; o link só existe no Perfil | Art. 9º, caput | média |
| G10 | Sem ROPA nem LIA documentados | RED | nada em `.lgpd/` ou `docs/` | Art. 37; Art. 10, § 3º | média (ATPP: forma simplificada, Res. 2/2022) |
| G11 | Faixa etária usa "13 anos" | YELLOW | Política seção 14; LGPD fala em criança (<12) e adolescente (<18) | Art. 14; Lei 15.211/2025 (avaliar "acesso provável" — baixo para app de manutenção de veículos) | baixa |
| G12 | Encarregado/canal não nomeado como tal | YELLOW | Política tem e-mail de contato, mas não o identifica como canal do titular/encarregado | Art. 41, § 1º; Res. 2/2022 Art. 11 (ATPP dispensado, canal obrigatório) | baixa |
| G13 | Chave da ORS enviada em query **e** header | YELLOW | `OrsNetwork.kt` adiciona `api_key` na URL e `Authorization`; a query fica em logs de proxy/CDN. Debug já a oculta no logcat | Art. 46 | baixa |
| — | Dados de veículos/abastecimentos só no aparelho | GREEN | Room local, sem sync | Art. 6º, III | — |
| — | Exclusão de conta no app | GREEN | `ProfileViewModel.performDelete` + reautenticação | Art. 18, VI | — |
| — | Logs HTTP/analytics só em debug | GREEN | `DebugLog` retorna `null`/tracker cru em release; headers sensíveis ocultos | Art. 46 | — |
| — | Eventos de analytics sem PII | GREEN | parâmetros são booleanos/método/chave de preferência | Art. 6º, III | — |
| — | Backup do Android declarado | GREEN | `allowBackup=true`, explicado na seção 6 | Art. 9º | — |
| — | Crashlytics desligado em debug | GREEN | `FirebaseSetup.kt` | — | — |

## Situação após a correção (2026-11-06)

| # | Situação |
|---|---|
| G1, G2, G3, G9, G11, G12 | corrigidos: política v2 (`hosting/public/privacidade.html`) + link na tela de login/cadastro |
| G4 | corrigido: "Apagar dados deste aparelho" sem conta (`ProfileViewModel.delete`) |
| G5 | corrigido: chave "Estatísticas de uso e relatórios de falha" em Preferências (`PrivacySettings.apply`) |
| G6 | corrigido: anúncios não personalizados por padrão (`npa=1`); personalizados só com opt-in; Consent Mode do Analytics segue a mesma escolha |
| G7 | corrigido: `AdMobGate` começa desligado |
| G8 | corrigido: `clearLocalData` limpa preferências e reinicia Analytics, Crashlytics, token FCM e Firebase Installation |
| G10 | corrigido: `ROPA.md` + `legal-basis.md` (LIA) |
| G13 | **mantido**: o `api_key` na query foi adicionado de propósito com o cálculo de rota; remover só depois de testar todos os endpoints da ORS só com o header (registrado em `docs/decisions.md`) |

Pendente fora do código: revisão jurídica da política, deploy do site, termos de operador
(Firebase/AdMob/HeiGIT), runbook de incidente.

## Ações imediatas (próxima semana)

1. **Corrigir a política (G1, G2, G3, G9, G11, G12)** — nova versão listando OpenRouteService
   (texto de origem/destino, Alemanha), OpenFreeMap, links para Google Maps/Waze, que rotas
   salvas ficam no aparelho; trocar "13 anos"; nomear o canal do titular. Linkar a política
   na tela de login/cadastro. — skill `lgpd-privacy-policy` (⏸ checkpoint)
2. **Liberar "Apagar dados" sem conta (G4)** — botão separado "Apagar dados deste aparelho"
   que chama `clearLocalData` mesmo com `account == null`.
3. **Corrigir `AdMobGate` (G7)** — iniciar em `false`, igual ao default do Remote Config.

## Próximas 2–4 semanas

4. **Chave em Preferências para estatísticas e relatórios de falha (G5)** —
   `setAnalyticsCollectionEnabled`, `setCrashlyticsCollectionEnabled`,
   `FirebasePerformance.isPerformanceCollectionEnabled`.
5. **Decidir base legal de anúncios (G6)** — ou consentimento via UMP (Google User Messaging
   Platform) com opção de anúncios não personalizados, ou LIA escrito. — `lgpd-legal-basis`
6. **Exclusão completa (G8)** — incluir reset de Analytics, Installations, token FCM e
   `SharedPreferences` em `clearLocalData`.
7. **ROPA simplificado + LIA (G10)** — `lgpd-data-mapping` → `lgpd-ropa`.
8. **Operadores e transferência (G2)** — ORS/HeiGIT e Google: `lgpd-vendor-audit` +
   `lgpd-international-transfer`.

## Backlog

9. Mandar a chave da ORS só no header (G13).
10. Runbook de incidente mínimo — `lgpd-incident-response`.
