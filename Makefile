# === COPY-THIS BOILERPLATE ============================================
# One-liners for local dev. Extend targets as your workflow evolves.
# ======================================================================

.PHONY: up down build sim agg api logs reset-db

up:         ## start infra
	docker compose up -d

down:       ## stop infra
	docker compose down

build:      ## build all jars
	./mvnw -q -DskipTests package

sim:        ## run simulator
	java -jar simulator/target/simulator-0.1.0.jar

agg:        ## run aggregator
	java -jar flow-agg/target/flow-agg-0.1.0.jar

api:        ## run API
	java -jar copilot-api/target/copilot-api-0.1.0.jar

logs:       ## follow Postgres logs
	docker logs -f postgres

reset-db:   ## wipe DB volume (⚠️ destroys data)
	 -docker compose down -v --remove-orphans
	 -docker rm -f redpanda redpanda-console postgres adminer 2>/dev/null || true
	 -docker volume rm $$(docker volume ls -q | grep -E 'pgdata$$') 2>/dev/null || true
	 docker compose up -d	