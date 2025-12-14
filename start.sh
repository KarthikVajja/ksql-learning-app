#!/bin/bash

echo "======================================"
echo "KSQL Learning App - Setup Script"
echo "======================================"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker Desktop first."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Start Docker Compose
echo "🚀 Starting Kafka ecosystem (Zookeeper, Kafka, KSQLDB)..."
docker-compose up -d

if [ $? -ne 0 ]; then
    echo "❌ Error: Failed to start Docker containers"
    exit 1
fi

echo ""
echo "⏳ Waiting for services to be ready (30 seconds)..."
sleep 30

echo ""
echo "✅ All services started successfully!"
echo ""
echo "======================================"
echo "Service Status:"
echo "======================================"
docker-compose ps
echo ""

echo "======================================"
echo "Services Ready:"
echo "======================================"
echo "📦 Zookeeper:     localhost:2181"
echo "📦 Kafka:         localhost:9092"
echo "📦 KSQLDB Server: localhost:8088"
echo ""

echo "======================================"
echo "Next Steps:"
echo "======================================"
echo "1. Build the project:"
echo "   ./gradlew clean build"
echo ""
echo "2. Open the project in IntelliJ IDEA"
echo ""
echo "3. Run configurations (in order):"
echo "   a. KafkaProducerApplication (Port 8081)"
echo "   b. KsqlConsumerApplication (Port 8082)"
echo ""
echo "4. Initialize KSQL:"
echo "   curl -X POST http://localhost:8082/api/ksql/initialize"
echo ""
echo "5. Generate sample data:"
echo "   curl -X POST 'http://localhost:8081/api/producer/generate?records=1000000'"
echo ""
echo "6. Run queries:"
echo "   curl http://localhost:8082/api/ksql/analytics/by-category"
echo ""
echo "======================================"
echo "For full documentation, see README.md"
echo "======================================"
