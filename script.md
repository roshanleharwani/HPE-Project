# Slide 8 – High-Level Design (≈1.5 min)

### Script

> "This slide shows the high-level architecture of our Payment Gateway and Processor System.
> 
> 
> Our system follows a **microservices** and **event-driven architecture**. Instead of processing everything inside one application, every major responsibility is separated into an independent service.
> 
> The request flow starts from the client, which can be a web application, mobile application, or third-party API. Every request first reaches the **API Gateway**, which acts as the single entry point.
> 
> The gateway validates incoming requests, performs routing, authentication, and idempotency checks to avoid duplicate payments, and then forwards the request.
> 
> If the request has already been received, the duplicate request will be stopped at the Auth Service itself.
> 
> After that, instead of directly calling every downstream service synchronously, we use **Apache Kafka** as the communication backbone. Kafka enables asynchronous processing, so services are loosely coupled and can process requests independently.
> 
> The payment then moves through three stages:
> 
> - Authorization
> - Clearing
> - Settlement
> 
> Finally, the transaction is stored in a **PostgreSQL sharded cluster** (3 shards, distributed by transaction ID), while **Redis** is used for idempotency checks and rate limiting to improve performance.
> 
> This architecture allows independent scaling, better fault tolerance, and higher throughput, helping us achieve our performance target of **10,000 requests per second**."
> 

---

## Possible Cross Questions

### Q1. Why microservices instead of monolithic?

**Answer**

"A monolithic application becomes difficult to scale because every component scales together. In microservices, only the bottleneck service can be scaled independently."

---

### Q2. Why Kafka?

**Answer**

"Kafka decouples services through asynchronous messaging. Even if one service becomes temporarily unavailable, messages remain in Kafka until they are consumed."

---

### Q3. Why asynchronous processing?

**Answer**

"Because synchronous communication makes every service wait for the next one. Asynchronous processing reduces latency and improves throughput."

---

# Slide 9 – Low-Level Design (≈2 min)

### Script

> "This slide explains the detailed internal workflow.
> 
> 
> Our architecture is divided into two subnets — a **Private Subnet** and a **Public Subnet** — to prevent users from directly accessing our API Gateway.
> 
> In our Private Subnet, all the microservices are deployed, and the entire business logic resides there.
> 
> In our Public Subnet, we have deployed **NGINX** — the firewall for our system — which acts as a reverse proxy.
> 
> It handles **rate limiting** and has **ModSecurity WAF** enabled, which protects our system from:
> 
> - SQL Injection
> - Cross-Site Scripting (XSS)
> - Path Traversal
> - Malicious Payloads
> 
> Furthermore, NGINX is also responsible for **load balancing** and **proxy buffering**, which helps free backend connections sooner and smooths out slow client downloads.
> 
> After all these checks pass, the request enters the API Gateway.
> 
> When a payment request reaches the API Gateway, it is first validated and assigned an idempotency key to prevent duplicate transactions.
> 
> If a request already exists in Redis, we stop that duplicate request to prevent the user from being charged again for the same transaction.
> 
> The request is then forwarded to the Authorization Service, where payment details such as card information, account balance, and transaction validity are verified.
> 
> Once authorization succeeds, an event is published to the Kafka topic **payment_authorized**.
> 
> The Clearing Service consumes this event, performs financial calculations, enriches the transaction with settlement-related information, and publishes another event called **payment_cleared**.
> 
> The Settlement Service consumes this event and completes the final payment process.
> 
> After settlement, the transaction status is updated and another event is published for persistence and reporting.
> 
> At any point, if a request fails for any reason, we have implemented a **retry mechanism** using Kafka's **Dead Letter Queue (DLQ)**. The system retries up to **3 times** with exponential backoff before routing the failed message to a Dead Letter Topic for manual analysis. This ensures that no transaction is silently lost, even in case of transient failures like network timeouts or temporary service unavailability.
> 
> Since every service communicates through Kafka, the system remains loosely coupled and highly fault tolerant."
> 

---

## Cross Questions

### Why separate Authorization and Settlement?

**Answer**

"Authorization only verifies whether payment is allowed, while settlement actually completes the transfer of funds. Separating them follows real-world banking workflows."

---

### What happens if Settlement crashes?

**Answer**

"Kafka retains the message. After the service recovers, it consumes the pending message, preventing transaction loss."

---

### Why event-driven architecture?

**Answer**

"It improves scalability, reduces dependencies between services, and increases fault tolerance."

---

# Slide 10 – Components Used (≈1 min)

*(The slide title says "it is used", so explain the technologies.)*

### Script

> To implement this architecture and to be free from **vendor lock-in**, we have not used AWS's built-in managed services. Instead, we have used **open-source technologies**, so that in the future, if we want to migrate from one cloud provider to another — or even deploy on bare metal — we can do so seamlessly.

> "These are the major technologies used in our project.
> 
> 
> For the firewall, AWS provides **AWS WAF**, but we are not using it. As we discussed in our LLD, we are using **NGINX with ModSecurity** enabled, which can be manually configured and provides more flexibility to define custom firewall rules.
> 
> Next, for the **Application Load Balancer**, instead of relying on Kubernetes' default round-robin mechanism, we are using **NGINX** with the **Exponentially Weighted Moving Average (EWMA)** load balancing algorithm. This completely bypasses Kubernetes' built-in load balancing to achieve better traffic distribution based on real-time response times.
> 
> For the **API Gateway**, we are using NGINX combined with a **Spring Boot backend service** for API routing — to send each request to the correct microservice for processing.
> 
> For caching, AWS offers **ElastiCache**, but again we are not using it. Instead, we are running a **self-hosted Redis cluster** for **rate limiting** and **idempotency checks**.
> 
> Now comes the backbone of our architecture — **Apache Kafka**. Instead of using Amazon MSK, we are using self-hosted Apache Kafka to enable communication between different microservices, which are written in different languages — Java and C++. Apache Kafka enables this cross-language communication seamlessly. This allows our system to achieve a high throughput of **10,000 requests per second**.
> 
> After the transaction is settled, to make the record persistent, we are using a **PostgreSQL cluster** with **sharding** and **read replicas**. We have **3 shards**, distributed using a consistent hash on the transaction ID, for better read and write performance.
> 
> For monitoring, AWS provides **CloudWatch**, but to get deeper insights and identify bottlenecks, we are using **Prometheus** for metrics collection and **Grafana** for visualization and monitoring. Grafana also allows us to set up **alert triggers** — for example, if Redis memory exceeds 1.8 GB, if any deployment scales beyond 55 pods, or if a pod enters a crash loop, these alerts automatically notify the development team.
> 
> Lastly, for **auto-scaling**, we are using Kubernetes **HPA (Horizontal Pod Autoscaler)**. Based on predefined conditions such as CPU and memory utilization, it automatically replicates pods to prevent bottlenecks in our system.
> 
> Now, let us explain how each microservice works in detail. I'll hand it over to my teammate, **Avipsa**, who will walk you through the API Gateway Service."
> 

---

## Cross Questions

### Why PostgreSQL?

**Answer**

"Financial systems require ACID transactions. PostgreSQL guarantees consistency and reliability."

---

### Why Redis?

**Answer**

"Redis is an in-memory database with extremely low latency, making it suitable for caching frequently accessed data."

---

### Why Kubernetes?

**Answer**

"It automates deployment, scaling, health checks, and recovery."

---

# Slide 19 – Deployment Architecture (≈1.5 min)

### Script

> "This slide shows our deployment architecture.
> 
> 
> We deployed the application on AWS using Kubernetes.
> 
> Every microservice runs inside Docker containers managed by Kubernetes.
> 
> We have **1 master node** and **4 worker nodes**, giving us a total of **5 nodes** across the Kubernetes cluster.
> 
> Horizontal Pod Autoscaler increases or decreases the number of running pods based on traffic, allowing the system to scale automatically.
> 
> Initially, each service starts with a set minimum number of replicas — for example, the API Gateway starts with **20 pods** and can scale up to **100 pods**, while the Authorization Service starts with **15 pods** and scales up to **60 pods**. The HPA monitors CPU and memory utilization — when CPU usage exceeds the target threshold (for instance, 500m average for the API Gateway), the HPA triggers a scale-up. It can scale aggressively, increasing by up to **100% of current pods** or **10 additional pods** every 15 seconds. On the way down, it scales conservatively with a **120-second stabilization window** to avoid flapping, removing only 2 pods per minute. During our load testing, we observed the API Gateway scale dynamically up to **80 pods** to handle peak traffic.
> 
> Kubernetes continuously monitors the health of every pod. If any pod fails, it automatically restarts it.
> 
> Kafka brokers are distributed across the cluster to remove single points of failure.
> 
> The API Gateway receives all incoming traffic and forwards requests to backend services."
> 

---

## Cross Questions

### Why Kubernetes instead of Docker only?

**Answer**

"Docker only creates containers. Kubernetes manages thousands of containers, handles scaling, networking, and automatic recovery."

---

### How is fault tolerance achieved?

**Answer**

"Multiple pod replicas, distributed Kafka brokers, Kubernetes self-healing, and asynchronous communication."

---

# Slide 20 – Deployment & AWS Services (≈1 min)

### Script

> "For deployment, we used five AWS EC2 instances of type **C7i.4xlarge**.
> 
> 
> Each instance provides **16 vCPUs** and **32 GB memory**, giving us a total cluster capacity of **80 vCPUs** and **160 GB RAM**. We chose compute-optimized instances because payment processing is a CPU-intensive workload.
> 
> For performance testing, we used Apache JMeter version 5.6.3.
> 
> For monitoring, we used Prometheus version 3.12 and Grafana version 13."
> 

*(Say "Grafana", not "Graphana".)*

---

## Cross Questions

### Why C7i instances?

**Answer**

"They provide newer Intel processors with higher compute performance suitable for CPU-intensive workloads."

---

### Why multiple EC2 instances?

**Answer**

"To distribute services across nodes and improve scalability and availability."

---

# Slide 21 – Monitoring & Observability (≈2 min)

### Script

> "Monitoring is essential because achieving high throughput alone is not enough—we also need to observe system health.
> 
> 
> Prometheus continuously collects metrics such as CPU usage, memory utilization, request count, response time, and Kafka consumer lag.
> 
> Grafana visualizes these metrics using dashboards, allowing us to quickly identify performance bottlenecks. We also track **Kafka consumer lag** as a key metric — if lag increases, it indicates that downstream services are falling behind, which could lead to delayed payment processing.
> 
> For logging, we use the **ELK Stack** (Elasticsearch, Logstash, Kibana) to collect and aggregate logs from all microservices and Kubernetes pods.
> 
> Centralized logging helps us debug failures, identify exceptions, and trace transaction flow across services."
> 

---

## Cross Questions

### Difference between Prometheus and Grafana?

**Answer**

"Prometheus collects metrics. Grafana visualizes them."

---

### What is Kafka Consumer Lag?

**Answer**

"It is the difference between produced messages and consumed messages. High lag indicates consumers cannot keep up."

---

### Why centralized logging?

**Answer**

"Without centralized logging, debugging distributed systems becomes extremely difficult because logs are spread across multiple services."

---

# Slide 22 – JMeter Load Testing (≈1.5 min)

### Script

> "To validate system performance, we performed load testing using Apache JMeter.
> 
> 
> JMeter generated thousands of concurrent payment requests to simulate real-world traffic.
> 
> During testing, we monitored throughput, average response time, CPU utilization, memory usage, and Kafka consumer lag across all brokers.
> 
> The objective was to verify that the system could handle close to or above **10,000 requests per second** while maintaining acceptable latency and system stability."
> 

---

## Cross Questions

### Why JMeter?

**Answer**

"It supports concurrent users, distributed load generation, scripting, and performance reporting."

---

### What metrics did you monitor?

**Answer**

"Throughput, average response time, error rate, CPU usage, memory usage, and Kafka consumer lag."

---

### Why is throughput important?

**Answer**

"It measures how many requests the system successfully processes every second."

---

# Slide 23 – DoS Attack Simulation (≈1.5 min)

### Script

> "Besides performance testing, we also evaluated the system against DoS-like traffic.
> 
> 
> Large numbers of requests were generated to simulate abnormal traffic.
> 
> The objective was to verify that legitimate payment requests continued to be processed while malicious traffic was controlled.
> 
> Security mechanisms such as NGINX, ModSecurity, request filtering, and rate limiting helped protect backend services from overload.
> 
> This demonstrates that the system remains available even under heavy traffic conditions."
> 

---

## Cross Questions

### Difference between DoS and DDoS?

**Answer**

- **DoS:** Attack originates from one source.
- **DDoS:** Attack originates from many distributed systems simultaneously.

---

### How does rate limiting help?

**Answer**

"It limits the number of requests allowed from a client within a time window, preventing server overload."

---

### Why use NGINX?

**Answer**

"It acts as a reverse proxy, distributes traffic efficiently, and allows firewall rules and request filtering."