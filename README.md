# AIQO Cloud (aggregatore)

Repository aggregatore per i moduli AIQO.

## Struttura
- `aiqo-ai-submitter`: servizio di submit.
- `aiqo-collector`: servizio di raccolta.
- `aiqo-persistence`: servizio di persistenza.
- `aiqo-infra`: stack infrastrutturale (Docker Compose).

## Avvio rapido (infra)
Esegui lo stack da `aiqo-infra/`:

```sh
docker compose up -d
```

## Note
- I singoli moduli hanno il proprio codice e (se presenti) README dedicati.
