# API Quick Reference

## Producer Service (Port 8081)

### Generate Data
```bash
# Generate 1,000 records (default)
curl -X POST http://localhost:8081/api/producer/generate

# Generate custom number of records
curl -X POST "http://localhost:8081/api/producer/generate?records=100000"
```

### Health Check
```bash
curl http://localhost:8081/api/producer/health
```

---

## Consumer Service (Port 8082)

### Initialize KSQLDB
**Important**: Run this first before querying data. Drops existing objects and recreates them.

```bash
curl -X POST http://localhost:8082/api/ksql/initialize
```

### Analytics Endpoints

#### Sales by Category
```bash
curl http://localhost:8082/api/ksql/analytics/by-category
```
Returns: category, transaction_count, total_revenue, avg_transaction_value, min_transaction, max_transaction

#### Sales by Region
```bash
curl http://localhost:8082/api/ksql/analytics/by-region
```
Returns: region, transaction_count, total_revenue, avg_transaction_value

#### Sales by Store
```bash
curl http://localhost:8082/api/ksql/analytics/by-store
```
Returns: store, transaction_count, total_revenue, avg_transaction_value

#### Sales by Payment Method
```bash
curl http://localhost:8082/api/ksql/analytics/by-payment-method
```
Returns: payment_method, transaction_count, total_revenue

#### High-Value Transactions
```bash
# Default: transactions > $5000
curl http://localhost:8082/api/ksql/analytics/high-value

# Custom threshold
curl "http://localhost:8082/api/ksql/analytics/high-value?minAmount=10000"
```
Returns: transaction_id, customer_id, product_name, total_amount, region, timestamp

#### Top Products
```bash
# Default: top 10 products
curl http://localhost:8082/api/ksql/analytics/top-products

# Custom limit
curl "http://localhost:8082/api/ksql/analytics/top-products?limit=20"
```
Returns: product_name, category, purchase_count, total_quantity_sold, total_revenue, avg_price

### Transaction Endpoints

#### Recent Transactions
```bash
# Default: 20 most recent
curl http://localhost:8082/api/ksql/transactions/recent

# Custom limit
curl "http://localhost:8082/api/ksql/transactions/recent?limit=50"
```
Returns: transaction_id, customer_id, product_name, category, total_amount, payment_method, region, store, timestamp

### Custom Query
```bash
curl -X POST http://localhost:8082/api/ksql/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT category, COUNT(*) as count FROM sales_stream GROUP BY category EMIT CHANGES LIMIT 10;"
  }'
```

### Health Check
```bash
curl http://localhost:8082/api/ksql/health
```

---

## Complete Test Flow

```bash
# 1. Start services
docker-compose up -d
./gradlew :kafka-producer:bootRun  # Terminal 1
./gradlew :ksql-consumer:bootRun   # Terminal 2

# 2. Initialize KSQLDB (Terminal 3)
curl -X POST http://localhost:8082/api/ksql/initialize

# 3. Generate data
curl -X POST "http://localhost:8081/api/producer/generate?records=10000"

# 4. Wait a few seconds for processing, then query
curl http://localhost:8082/api/ksql/analytics/by-category
curl http://localhost:8082/api/ksql/analytics/top-products
curl http://localhost:8082/api/ksql/transactions/recent
```

---

## Useful Docker Commands

```bash
# View logs
docker logs ksqldb-server
docker logs kafka

# Connect to KSQLDB CLI
docker exec -it ksqldb-cli ksql http://ksqldb-server:8088

# View Kafka topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic sales-transactions \
  --from-beginning \
  --max-messages 10

# List all topics
docker exec -it kafka kafka-topics \
  --bootstrap-server localhost:29092 \
  --list
```

---

## KSQLDB CLI Commands

```bash
# Connect to CLI
docker exec -it ksqldb-cli ksql http://ksqldb-server:8088

# Inside KSQLDB CLI:
SHOW STREAMS;
SHOW TABLES;
DESCRIBE sales_stream;
DESCRIBE sales_by_category;

# Query examples
SELECT * FROM sales_stream EMIT CHANGES LIMIT 10;
SELECT * FROM sales_by_category;
SELECT * FROM product_analytics WHERE purchase_count > 100;
```
