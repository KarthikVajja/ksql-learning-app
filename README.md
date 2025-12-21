# KSQL Learning App with Protobuf

This project demonstrates using Protocol Buffers with Kafka and KSQLDB without a Schema Registry (using PROTOBUF_NOSR format).

## Architecture

- **Kafka Producer**: Sends Protobuf-serialized messages to Kafka
- **Local Proto Files**: The `protos/` directory acts as a "schema registry" mounted to containers
- **KSQLDB**: Reads Protobuf messages from Kafka using PROTOBUF_NOSR format (no Schema Registry required)

## Setup

### 1. Build the project (this will generate Java classes from .proto files)

```bash
./gradlew clean build
```

### 2. Start Kafka and KSQLDB

```bash
docker-compose up -d
```

Wait for containers to be healthy (~30 seconds):
```bash
docker-compose ps
```

### 3. Run the Producer

```bash
./gradlew :kafka-producer:bootRun
```

Producer will start on port **8081**

### 4. Run the Consumer (in another terminal)

```bash
./gradlew :ksql-consumer:bootRun
```

Consumer will start on port **8082**

## Testing the Application

### Step 1: Initialize KSQLDB Streams and Tables

This will create all necessary streams and tables. **Note**: This will drop any existing objects first.

```bash
curl -X POST http://localhost:8082/api/ksql/initialize
```

Expected response:
```json
{
  "status": "success",
  "message": "KSQL streams and tables initialized successfully. All previous objects were cleaned up."
}
```

### Step 2: Generate Test Data

Generate 1000 sales transactions:

```bash
curl -X POST "http://localhost:8081/api/producer/generate?records=1000"
```

For more records (e.g., 100,000):
```bash
curl -X POST "http://localhost:8081/api/producer/generate?records=100000"
```

### Step 3: Query the Data

**Get Sales by Category:**
```bash
curl http://localhost:8082/api/ksql/analytics/by-category
```

**Get Sales by Region:**
```bash
curl http://localhost:8082/api/ksql/analytics/by-region
```

**Get Sales by Store:**
```bash
curl http://localhost:8082/api/ksql/analytics/by-store
```

**Get High-Value Transactions (> $5000):**
```bash
curl "http://localhost:8082/api/ksql/analytics/high-value?minAmount=5000"
```

**Get Sales by Payment Method:**
```bash
curl http://localhost:8082/api/ksql/analytics/by-payment-method
```

**Get Top Products:**
```bash
curl "http://localhost:8082/api/ksql/analytics/top-products?limit=10"
```

**Get Recent Transactions:**
```bash
curl "http://localhost:8082/api/ksql/transactions/recent?limit=20"
```

**Execute Custom KSQL Query:**
```bash
curl -X POST http://localhost:8082/api/ksql/query \
  -H "Content-Type: application/json" \
  -d '{"sql": "SELECT * FROM sales_stream EMIT CHANGES LIMIT 10;"}'
```

## What Gets Created

When you run the initialization, the following are created:

1. **Stream: sales_stream**
   - Raw stream of all sales transactions from Kafka topic
   - Uses PROTOBUF_NOSR format (no Schema Registry needed)

2. **Table: sales_by_category**
   - Aggregates: count, total revenue, avg/min/max transaction value
   - Grouped by: category

3. **Table: sales_by_region**
   - Aggregates: count, total revenue, avg transaction value
   - Grouped by: region

4. **Table: sales_by_payment_method**
   - Aggregates: count, total revenue
   - Grouped by: payment_method

5. **Table: product_analytics**
   - Aggregates: purchase count, total quantity, total revenue, avg price
   - Grouped by: product_name, product_id, category

## Protobuf Schema

The schema is defined in `protos/sales_transaction.proto`:

```protobuf
message SalesTransaction {
  string transaction_id = 1;
  string customer_id = 2;
  string product_id = 3;
  string product_name = 4;
  string category = 5;
  double price = 6;
  int32 quantity = 7;
  double total_amount = 8;
  string payment_method = 9;
  string region = 10;
  string store = 11;
  int64 timestamp = 12;
}
```

## How PROTOBUF_NOSR Works

1. **Producer Side**: 
   - Gradle protobuf plugin generates Java classes from `.proto` files
   - KafkaProtobufSerializer serializes messages with embedded schema
   - Messages are sent to Kafka with self-describing schema

2. **KSQLDB Side**:
   - KSQLDB uses `VALUE_FORMAT='PROTOBUF_NOSR'`
   - No Schema Registry connection needed
   - KSQLDB deserializes using the embedded schema in each message
   - Field names in KSQL use snake_case to match proto naming

## Key Differences from JSON Format

- **Field Names**: Proto uses snake_case (e.g., `transaction_id`) instead of camelCase
- **No Schema Registry**: Schema is embedded in each message
- **Better Performance**: Protobuf is more compact than JSON
- **Type Safety**: Strong typing enforced by protocol buffers

## Troubleshooting

**If initialization fails:**
```bash
# Check KSQLDB logs
docker logs ksqldb-server

# Check if Kafka is running
docker-compose ps
```

**If queries return no data:**
1. Make sure you've generated data using the producer
2. Wait a few seconds for KSQL to process the data
3. Check the Kafka topic has messages:
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic sales-transactions \
  --from-beginning \
  --max-messages 5
```

**To re-initialize everything:**
Just call the initialize endpoint again - it will drop all existing objects and recreate them:
```bash
curl -X POST http://localhost:8082/api/ksql/initialize
```

## Cleanup

Stop all services:
```bash
docker-compose down -v
```

The `-v` flag removes volumes, ensuring a clean slate next time.
