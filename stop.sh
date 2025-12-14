#!/bin/bash

echo "======================================"
echo "KSQL Learning App - Shutdown Script"
echo "======================================"
echo ""

echo "🛑 Stopping all Docker containers..."
docker-compose down

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All services stopped successfully!"
    echo ""
    echo "To remove all data and volumes, run:"
    echo "   docker-compose down -v"
else
    echo ""
    echo "❌ Error: Failed to stop containers"
    exit 1
fi
