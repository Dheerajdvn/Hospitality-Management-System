#!/bin/bash
# =====================================================================
# Hospitality Management System - Kafka Topic Initialization
# =====================================================================

BOOTSTRAP_SERVER=${1:-"localhost:9092"}

echo "Waiting for Kafka broker at ${BOOTSTRAP_SERVER}..."
cub kafka-ready -b "${BOOTSTRAP_SERVER}" 1 60 2>/dev/null || sleep 10

echo "Creating HMS event topics..."

TOPICS=(
  "hms.booking.events:3:1"
  "hms.payment.events:3:1"
  "hms.food.events:3:1"
  "hms.roomservice.events:3:1"
  "hms.inventory.events:3:1"
)

for topic_config in "${TOPICS[@]}"; do
  IFS=':' read -r topic partitions rf <<< "${topic_config}"
  echo "Creating topic: ${topic} (partitions=${partitions}, rf=${rf})..."
  kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor "${rf}"
done

echo "Listing all created Kafka topics:"
kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" --list
