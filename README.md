# ai-playground

Trade Flow Copilot — Starter (v2)

## To bring up the 3 services

```bash
make up && make build 

make sim
make agg
make api
```

## Start Grafana dashboard for realtime

```dash
docker compose up -d grafana
```

Open Grafana → <http://localhost:3000> (login: admin / admin)

## Sanity check

### Adminer

```sql
SELECT symbol, window_start, trade_count, total_qty, buy_qty, sell_qty, vwap, block_trades, imbalance
FROM trades_agg_1m
ORDER BY window_start DESC
LIMIT 10;
```

### Colipot api

```bash
curl -s localhost:8080/health
curl -s -X POST localhost:8080/summarize -H "Content-Type: application/json" \
  -d '{"symbol":"NVDA","interval":"1m"}' | jq .
curl -s -X GET localhost:8080/latest?symbol=AAPL&interval=1m -H "Content-Type: application/json" | jq .
```

## 90-minute “deep understanding” plan

1. Read flow-agg/App.java top to bottom and draw the topology boxes

1. Open AggState.add() and map each counter to a DB column

1. Run make sim + make agg and watch Console (lag) + Adminer (rows) + aggregator logs.

1. Call /latest a few times; change thresholds; see JSON change.

1. Change the window to 5s (temporarily), rebuild, and see faster DB updates—tie it back to commit interval.
