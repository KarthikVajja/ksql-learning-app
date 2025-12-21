# KSQL Learning App with Protobuf

This project demonstrates using Protocol Buffers with Kafka instead of a traditional Schema Registry.

## Architecture

- **Kafka Producer**: Sends Protobuf-serialized messages to Kafka
- **Local Proto Files**: The `protos/` directory acts as a "schema registry" mounted to containers
- **KSQLDB**: Reads Protobuf messages from Kafka using the local proto files

## Setup

### 1. Build the project (this will generate Java classes from .proto files)

```bash
./gradlew clean build
```

### 2. Start Kafka and KSQLDB

```bash
docker-compose up -d
```

### 3. Run the Producer

```bash
./gradlew :kafka-producer:bootRun
```

### 4. Generate test data

```bash
curl -X POST "http://localhost:8080/api/producer/generate?records=1000"
```

## Protobuf Schema

The schema is defined in `protos/sales_transaction.proto` and is shared between:
- The producer application (compiled into Java classes at build time)
- The Kafka container (mounted volume)
- The KSQLDB server (mounted volume)

## How it works without Schema Registry

1. **Producer Side**: 
   - Gradle protobuf plugin generates Java classes from `.proto` files at build time
   - KafkaProtobufSerializer serializes messages and embeds schema information
   - Messages are sent to Kafka with the schema embedded

2. **Consumer Side** (KSQLDB):
   - KSQLDB has access to the proto files via mounted volume
   - When creating streams/tables, KSQLDB can reference the proto files directly
   - Example KSQLDB query:
   ```sql
   CREATE STREAM sales_stream 
   WITH (KAFKA_TOPIC='sales-transactions', 
         VALUE_FORMAT='PROTOBUF',
         VALUE_PROTOBUF_FILE='/opt/ksqldb/protos/sales_transaction.proto',
         VALUE_PROTOBUF_MESSAGE='SalesTransaction');
   ```

## Advantages of this approach

- No Schema Registry infrastructure needed
- Proto files are version-controlled with the code
- Simpler local development setup
- Schema changes are explicit in source control

## Disadvantages

- No centralized schema management
- No schema compatibility checks across services
- Each service needs access to proto files
