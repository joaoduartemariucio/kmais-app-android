# LGPD Audit Status

**Projeto**: Rodaro (Android)
**Cenário**: B — Legacy retrofit
**Início**: 2026-11-06
**Última atualização**: 2026-11-06
**Encarregado**: João Vitor Duarte Mariucio — canal publicado na política v2, seção 17 (ATPP: designação formal dispensada pela Res. CD/ANPD nº 2/2022 Art. 11)

## Pipeline atual
- [x] L0 — Setup `.lgpd/`
- [x] L1 — Gap analysis (`gaps.md`)
- [x] L2 — Data mapping (dentro do `ROPA.md`)
- [x] L3 — Legal basis + LIA (`legal-basis.md`)
- [ ] L4 — Vendor audit (aceitar termos de operador do Firebase/AdMob; verificar termos da HeiGIT)
- [x] L5 — Retention/erasure (exclusão pelo app com ou sem conta, reset de identificadores)
- [-] L6 — Anonymization — N/A (sem pipeline de analytics próprio)
- [x] L7 — DSAR (autoatendimento no app + e-mail com prazo de 15 dias)
- [ ] L8 — Incident response (runbook)
- [x] L9 — Privacy policy v2 (`hosting/public/privacidade.html`, cópia em `policies/`) ⏸ revisão jurídica antes do deploy
- [-] L10 — ECA Digital — baixo risco: app não direcionado a menores; política ajustada
- [-] L11 — RIPD — N/A: sem dado sensível, sem larga escala, sem decisão automatizada
- [x] L12 — Encarregado / canal
- [ ] L13 — Plano final

## Artefatos gerados
- `.lgpd/gaps.md` — v1, 2026-11-06 (status de cada gap atualizado)
- `.lgpd/ROPA.md` — v1, 2026-11-06
- `.lgpd/legal-basis.md` — v1, 2026-11-06
- `.lgpd/policies/privacy-policy-v2.html` — 2026-11-06

## Próximo passo
1. Revisar a política v2 e publicar com `firebase deploy --only hosting`.
2. Publicar o app com as novas opções de privacidade.
3. `lgpd-vendor-audit` e `lgpd-incident-response`.
