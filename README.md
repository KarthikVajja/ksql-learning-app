# KSQL Learning Application

A comprehensive application to learn and understand KSQL (Kafka Streams Query Language) through practical examples. This application demonstrates how to perform historical analysis on Kafka data using KSQLDB.

## Project Structure

```
ksql-learning-app/
├── docker-compose.yml                          # Docker setup for Kafka ecosystem
├── build.gradle                                # Gradle build configuration
├── settings.gradle                             # Gradle settings
├── .run/                                       # IntelliJ run configurations
│   ├── KafkaProducerApplication.run.xml
│   └── KsqlConsumerApplication.run.xml
└── src/main/
    ├── java/com/ksqllearning/
    │   ├── producer/                           # Producer module
    │   │   ├── KafkaProducerApplication.java
    │   │   ├── config/
    │   │   │   └── KafkaProducerConfig.java
    │   │   ├── model/
    │   │   │   └── SalesTransaction.java
    │   │   ├── service/
    │   │   │   └── DataGeneratorService.java
    │   │   └── controller/
    │   │       └── ProducerController.java
    │   └── consumer/                           # Consumer module
    │       ├── KsqlConsumerApplication.java
    │       ├── config/
    │       │   └── KsqlConfig.java
    │       ├── service/
    │       │   └── KsqlQueryService.java
    │       └── controller/
    │           └── KsqlController.java
    └── resources/
        ├── application-producer.properties
        └── application-consumer.properties
```

## Prerequisites

- **Java 17** or higher
- **Docker** and **Docker Compose**
- **IntelliJ IDEA** (recommended) or any Java IDE
- **Gradle** (wrapper included)

## Quick Start Guide

### Step 1: Start Kafka Ecosystem with Docker

1. Open a terminal in the project directory:
   ```bash
   cd ~/Desktop/ksql-learning-app
   ```

2. Start all services (Zookeeper, Kafka, KSQLDB):
   ```bash
   docker-compose up -d
   ```

3. Verify all containers are running:
   ```bash
   docker-compose ps
   ```

   You should see:
   - `zookeeper` - Port 2181
   - `kafka` - Port 9092
   - `ksqldb-server` - Port 8088
   - `ksqldb-cli` - Interactive CLI

4. Wait about 30 seconds for all services to fully initialize.

### Step 2: Build the Project

```bash
./gradlew clean build
```

### Step 3: Run the Applications in IntelliJ

#### Option A: Using IntelliJ Run Configurations (Recommended)

1. Open the project in IntelliJ IDEA
2. The run configurations are already created in `.run/` directory
3. You'll see two run configurations in the top-right dropdown:
   - **KafkaProducerApplication** (Port 8081)
   - **KsqlConsumerApplication** (Port 8082)

4. **Start Producer First:**
   - Select "KafkaProducerApplication" from dropdown
   - Click the green Run button
   - Wait for "Started KafkaProducerApplication" in console

5. **Start Consumer Second:**
   - Select "KsqlConsumerApplication" from dropdown
   - Click the green Run button
   - Wait for "Started KsqlConsumerApplication" in console

#### Option B: Using Gradle Command Line

In separate terminal windows:

**Terminal 1 - Producer:**
```bash
./gradlew bootRun --args='--spring.profiles.active=producer'
```

**Terminal 2 - Consumer:**
```bash
./gradlew bootRun --args='--spring.profiles.active=consumer'
```

### Step 4: Initialize KSQL Streams and Tables

Before querying, you need to create KSQL streams and tables:

```bash
curl -X POST http://localhost:8082/api/ksql/initialize
```

This creates:
- `SALES_STREAM` - Stream from Kafka topic
- `SALES_BY_CATEGORY` - Aggregation table by category
- `SALES_BY_REGION` - Aggregation table by region

### Step 5: Generate Sample Data

Generate 1 million records (adjust the number as needed):

```bash
curl -X POST "http://localhost:8081/api/producer/generate?records=1000000"
```

This will:
- Generate realistic sales transaction data
- Send data to Kafka topic `sales-transactions`
- Show progress and performance metrics
- Take approximately 30-60 seconds for 1M records

## Using the Application

### Producer API Endpoints

**Health Check:**
```bash
curl http://localhost:8081/api/producer/health
```

**Generate Data:**
```bash
# Generate 1 million records (default)
curl -X POST http://localhost:8081/api/producer/generate

# Generate custom number of records
curl -X POST "http://localhost:8081/api/producer/generate?records=500000"
```

### Consumer API Endpoints (KSQL Queries)

**Health Check:**
```bash
curl http://localhost:8082/api/ksql/health
```

**Initialize KSQL (Required First):**
```bash
curl -X POST http://localhost:8082/api/ksql/initialize
```

**Pre-built Analytics Queries:**

1. **Sales by Category:**
```bash
curl http://localhost:8082/api/ksql/analytics/by-category
```

2. **Sales by Region:**
```bash
curl http://localhost:8082/api/ksql/analytics/by-region
```

3. **High-Value Transactions:**
```bash
# Transactions over $5000 (default)
curl http://localhost:8082/api/ksql/analytics/high-value

# Custom threshold
curl "http://localhost:8082/api/ksql/analytics/high-value?minAmount=10000"
```

4. **Transactions by Payment Method:**
```bash
curl http://localhost:8082/api/ksql/analytics/by-payment-method
```

5. **Top Products:**
```bash
# Top 10 products (default)
curl http://localhost:8082/api/ksql/analytics/top-products

# Top 20 products
curl "http://localhost:8082/api/ksql/analytics/top-products?limit=20"
```

**Custom KSQL Queries:**
```bash
curl -X POST http://localhost:8082/api/ksql/query \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "SELECT category, COUNT(*) as count FROM sales_stream GROUP BY category EMIT CHANGES LIMIT 10;"
  }'
```

## Understanding KSQL Concepts

### 1. Streams vs Tables

**STREAM** (`SALES_STREAM`):
- Represents an unbounded sequence of events
- Think of it as a log of all transactions
- Each record is a distinct event
- Use for: Real-time event processing, filtering, transformations

**TABLE** (`SALES_BY_CATEGORY`, `SALES_BY_REGION`):
- Represents the current state of aggregations
- Think of it as a materialized view
- Records are upserted (update or insert)
- Use for: Aggregations, current state queries

### 2. Key KSQL Query Patterns

**Simple SELECT (Historical Analysis):**
```sql
SELECT * FROM sales_stream EMIT CHANGES LIMIT 100;
```

**Filtering:**
```sql
SELECT * FROM sales_stream 
WHERE totalAmount > 5000 
EMIT CHANGES;
```

**Aggregations:**
```sql
SELECT 
    category,
    COUNT(*) AS transaction_count,
    SUM(totalAmount) AS total_revenue,
    AVG(totalAmount) AS avg_transaction
FROM sales_stream
GROUP BY category
EMIT CHANGES;
```

**Time Windows:**
```sql
SELECT 
    category,
    WINDOWSTART AS window_start,
    WINDOWEND AS window_end,
    COUNT(*) AS count
FROM sales_stream
WINDOW TUMBLING (SIZE 1 HOUR)
GROUP BY category
EMIT CHANGES;
```

**Joins:**
```sql
-- Join stream with table for enrichment
SELECT 
    s.transactionId,
    s.productName,
    s.totalAmount,
    c.total_revenue AS category_total
FROM sales_stream s
JOIN sales_by_category c ON s.category = c.category
EMIT CHANGES;
```

### 3. Data Model

Each sales transaction contains:
- `transactionId`: Unique identifier
- `customerId`: Customer reference
- `productId` & `productName`: Product details
- `category`: Product category (Electronics, Computers, Audio, etc.)
- `price`: Unit price
- `quantity`: Number of items
- `totalAmount`: Total transaction value
- `paymentMethod`: Payment type
- `region`: Geographic region (North, South, East, West, Central)
- `store`: Store identifier
- `timestamp`: Transaction timestamp

## Learning Exercises

### Exercise 1: Basic Queries
1. Find all transactions over $10,000
2. Count transactions by payment method
3. List the top 5 most expensive products

### Exercise 2: Aggregations
1. Calculate total revenue by region
2. Find average transaction value by category
3. Count transactions per store

### Exercise 3: Advanced Analysis
1. Create a stream for high-value customers (total purchases > $50,000)
2. Build a table showing hourly sales trends
3. Identify products with highest profit margins

### Exercise 4: Real-time Monitoring
1. Create an alert stream for transactions > $15,000
2. Build a dashboard showing live sales by region
3. Track payment method trends over time

## KSQL CLI Access

For interactive KSQL exploration:

```bash
# Access KSQL CLI
docker exec -it ksqldb-cli ksql http://ksqldb-server:8088

# Inside KSQL CLI:
SHOW STREAMS;
SHOW TABLES;
DESCRIBE sales_stream;
SELECT * FROM sales_stream EMIT CHANGES LIMIT 10;
```

## Monitoring and Management

### Check Kafka Topics:
```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### View Topic Contents:
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic sales-transactions \
  --from-beginning \
  --max-messages 10
```

### View Logs:
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f ksqldb-server
docker-compose logs -f kafka
```

## Performance Tuning

### Producer Settings (Already Optimized):
- Batch size: 16KB
- Linger time: 10ms
- Compression: Snappy
- Buffer memory: 32MB
- Async sending with 10 threads

### Expected Performance:
- 1M records: ~30-60 seconds
- Throughput: 15,000-35,000 records/second
- Depends on your machine's CPU and disk I/O

## Troubleshooting

### Issue: Cannot connect to Kafka
**Solution:**
```bash
# Restart Docker containers
docker-compose restart

# Check Kafka is ready
docker-compose logs kafka | grep "started"
```

### Issue: KSQL queries return no data
**Solution:**
1. Verify data was sent: Check producer logs
2. Reinitialize KSQL: `curl -X POST http://localhost:8082/api/ksql/initialize`
3. Wait a few seconds for data to flow through

### Issue: Out of memory
**Solution:**
- Reduce batch size in producer
- Generate fewer records at once
- Increase Docker memory allocation

### Issue: Port already in use
**Solution:**
```bash
# Find and kill process using port
lsof -ti:8081 | xargs kill -9
lsof -ti:8082 | xargs kill -9
```

## Stopping the Application

1. Stop Spring Boot applications (Ctrl+C in terminals or Stop in IntelliJ)
2. Stop Docker containers:
```bash
docker-compose down
```

3. To remove all data and start fresh:
```bash
docker-compose down -v  # Removes volumes too
```

## Advanced Topics

### Creating Custom Streams
```sql
CREATE STREAM filtered_sales AS
SELECT * FROM sales_stream
WHERE totalAmount > 1000
EMIT CHANGES;
```

### Creating Custom Tables
```sql
CREATE TABLE customer_stats AS
SELECT 
    customerId,
    COUNT(*) AS purchase_count,
    SUM(totalAmount) AS total_spent,
    AVG(totalAmount) AS avg_transaction
FROM sales_stream
GROUP BY customerId
EMIT CHANGES;
```

### Pull Queries (Point-in-time)
```sql
SELECT * FROM sales_by_category WHERE category = 'Electronics';
```

### Push Queries (Continuous)
```sql
SELECT * FROM sales_stream EMIT CHANGES;
```

## Resources

- [KSQLDB Documentation](https://docs.ksqldb.io/)
- [Confluent KSQL Tutorial](https://kafka-tutorials.confluent.io/)
- [Kafka Documentation](https://kafka.apache.org/documentation/)

## Common KSQL Commands Reference

```sql
-- Show all streams
SHOW STREAMS;

-- Show all tables
SHOW TABLES;

-- Describe a stream
DESCRIBE sales_stream;

-- Describe a table
DESCRIBE sales_by_category;

-- Drop a stream
DROP STREAM sales_stream DELETE TOPIC;

-- Drop a table
DROP TABLE sales_by_category DELETE TOPIC;

-- Show running queries
SHOW QUERIES;

-- Terminate a query
TERMINATE query_id;
```

## Next Steps

1. Experiment with different query patterns
2. Create custom aggregations
3. Try windowed operations
4. Build real-time dashboards
5. Explore stream-table joins
6. Implement complex event processing patterns

## Support

For issues or questions:
1. Check the logs: `docker-compose logs`
2. Verify all services are running: `docker-compose ps`
3. Review the KSQL documentation
4. Check Kafka topic contents

Enjoy learning KSQL! 🚀
