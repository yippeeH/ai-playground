-- === LEARN-BY-BUILDING ===============================================
-- Schema is a product decision. Keys, types, and PKs impact latency & correctness.
-- TODO: consider partial indexes or BRIN on window_start if data grows.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS trades_raw (
  ts TIMESTAMPTZ NOT NULL,
  symbol TEXT NOT NULL,
  side TEXT NOT NULL,
  qty BIGINT NOT NULL,
  price NUMERIC(18,6) NOT NULL,
  aggressor BOOLEAN NOT NULL,
  PRIMARY KEY (ts, symbol, side, qty, price)
);

CREATE TABLE IF NOT EXISTS trades_agg_1m (
  window_start TIMESTAMPTZ NOT NULL,
  symbol TEXT NOT NULL,
  trade_count BIGINT NOT NULL,
  total_qty BIGINT NOT NULL,
  buy_qty BIGINT NOT NULL,
  sell_qty BIGINT NOT NULL,
  vwap NUMERIC(18,6),
  block_trades BIGINT NOT NULL,
  imbalance DOUBLE PRECISION NOT NULL,
  PRIMARY KEY (window_start, symbol)
);

CREATE TABLE IF NOT EXISTS trades_agg_5s (
  window_start TIMESTAMPTZ NOT NULL,
  symbol TEXT NOT NULL,
  trade_count BIGINT NOT NULL,
  total_qty BIGINT NOT NULL,
  buy_qty BIGINT NOT NULL,
  sell_qty BIGINT NOT NULL,
  vwap NUMERIC(18,6),
  block_trades BIGINT NOT NULL,
  imbalance DOUBLE PRECISION NOT NULL,
  PRIMARY KEY (window_start, symbol)
);

CREATE TABLE IF NOT EXISTS trades_agg_5m (
  window_start TIMESTAMPTZ NOT NULL,
  symbol TEXT NOT NULL,
  trade_count BIGINT NOT NULL,
  total_qty BIGINT NOT NULL,
  buy_qty BIGINT NOT NULL,
  sell_qty BIGINT NOT NULL,
  vwap NUMERIC(18,6),
  block_trades BIGINT NOT NULL,
  imbalance DOUBLE PRECISION NOT NULL,
  PRIMARY KEY (window_start, symbol)
);

CREATE TABLE IF NOT EXISTS signals_flow (
  ts TIMESTAMPTZ NOT NULL,
  symbol TEXT NOT NULL,
  signal TEXT NOT NULL,
  details JSONB,
  PRIMARY KEY (ts, symbol, signal)
);
