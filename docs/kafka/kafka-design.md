# Apache Kafka Event Design & Topic Topology

## 1. Kafka Broker Architecture (KRaft Mode)
This project uses **Apache Kafka in KRaft (Kafka Raft Metadata) mode** instead of legacy ZooKeeper:
- **ZooKeeper-less Operation**: Kafka handles its own quorum and controller elections via the Raft consensus protocol.
- **Lower Resource Footprint**: Eliminates the memory and operational overhead of maintaining a separate ZooKeeper cluster.
- **Modern Production Standard**: KRaft is the official standard in modern Apache Kafka.

## 2. Topic Catalog & Partitioning Strategy

| Topic Name | Partitions | Key Strategy | Retention | Consumers |
| :--- | :--- | :--- | :--- | :--- |
| `hms.booking.events` | 3 | `bookingId` | 7 Days | `billing-service`<br/>`notification-service` |
| `hms.payment.events` | 3 | `bookingId` | 7 Days | `booking-service`<br/>`notification-service` |
| `hms.food.events` | 3 | `orderId` | 7 Days | `billing-service`<br/>`inventory-service` |
| `hms.roomservice.events` | 3 | `requestId` | 7 Days | `billing-service`<br/>`inventory-service`<br/>`notification-service` |
| `hms.inventory.events` | 3 | `itemCode` | 7 Days | `notification-service` |

### Why Partitions = 3?
- Partitions allow horizontal scaling across multiple consumer instances within the same consumer group.
- With 3 partitions, up to 3 parallel Spring Boot consumer instances can process events concurrently without partition contention.
- By using entity IDs (such as `bookingId`) as the Kafka message key, Kafka guarantees **strict ordering of events for any given booking** (all events with the same key hash to the exact same partition).

## 3. Kafka UI for Demos & Debugging
- Access the web dashboard at `http://localhost:8095` to inspect topics, verify message payloads, review consumer group lag, and observe real-time event streaming.
