#include "../include/KafkaConsumer.h"
#include "../include/KafkaProducer.h"
#include "../include/AuthorizationEngine.h"
#include "../include/PaymentModels.h"
#include "../include/ThreadPool.h"
#include "../include/ConnectionPool.h"
#include "../include/Config.h"
#include <nlohmann/json.hpp>

#include <iostream>
#include <string>
#include <csignal>
#include <cstdlib>
#include <memory>

volatile bool running = true;

void signal_handler(int sig) {
    if (running) {
        std::cout << "Stopping consumer..." << std::endl;
        running = false;
    } else {
        std::exit(sig);
    }
}

int main() {
    std::signal(SIGINT, signal_handler);
    std::signal(SIGTERM, signal_handler);

    std::string kafka_brokers = Config::getKafkaBrokers();
    std::string db_conn = Config::getEnv("DB_CONN", "postgresql://postgres:postgres@localhost:5432/postgres");

    std::cout << "Starting Authorization Service (High Throughput 10k RPS)..." << std::endl;
    std::cout << "Kafka Brokers: " << kafka_brokers << std::endl;
    
    try {
        // Initialize ThreadPool & DB Connection Pool
        size_t THREADS = 50;
        size_t DB_CONNS = 50;
        
        ThreadPool tpool(THREADS);
        auto db_pool = std::make_shared<ConnectionPool>(db_conn, DB_CONNS);
        
        // Wrap these so workers can access them
        auto engine = std::make_shared<AuthorizationEngine>(db_pool);
        auto producer = std::make_shared<KafkaProducer>(kafka_brokers);

        KafkaConsumer consumer(kafka_brokers, "authorization-service-group", "payment_initiated");

        auto message_handler = [&tpool, engine, producer](const std::string& payload) {
            // Immediately Enqueue to free up the Consumer thread
            tpool.enqueue([payload, engine, producer]() {
                try {
                    auto j = nlohmann::json::parse(payload);
                    PaymentInitiatedEvent req = j;

                    std::string reason;
                    bool is_authorized = engine->authorize(req, reason);

                    PaymentAuthorizedEvent res;
                    res.transaction_id = req.transaction_id;
                    res.payment_intent_id = req.payment_intent_id;
                    res.payment_method_id = req.payment_method_id;
                    res.user_id = req.user_id;
                    res.amount = req.amount;
                    res.currency = req.currency;
                    
                    std::string out_topic;
                    if (is_authorized) {
                        res.status = "AUTHORIZED";
                        res.reason = "Clear";
                        out_topic = "payment_authorized";
                    } else {
                        res.status = "FAILED";
                        res.reason = reason;
                        out_topic = "payment_failed";
                    }

                    // Serialize and Broadcast
                    nlohmann::json j_res = res;
                    producer->publish(out_topic, j_res.dump());

                } catch (const std::exception& e) {
                    std::cerr << "Processing error within worker thread: " << e.what() << std::endl;
                }
            });
        };

        consumer.start(message_handler, running);

    } catch (const std::exception& e) {
        std::cerr << "Startup error: " << e.what() << std::endl;
        return 1;
    }

    std::cout << "Authorization Service shutdown complete." << std::endl;
    return 0;
}
