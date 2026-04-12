#include "SettlementService.h"
#include <iostream>
#include <chrono>
#include <thread>

namespace settlement {

SettlementService::SettlementService(const KafkaSettings& settings)
    : settings_(settings), running_(false) {
    consumer_ = std::make_unique<KafkaConsumer>(settings_.brokers, settings_.consumer_group_id, settings_.topic_payment_cleared);
    producer_ = std::make_unique<KafkaProducer>(settings_.brokers);
}

SettlementService::~SettlementService() {
    stop();
}

bool SettlementService::init() {
    std::cout << "Initializing Settlement Service..." << std::endl;
    if (!producer_->start()) {
        std::cerr << "Failed to start Kafka producer." << std::endl;
        return false;
    }

    if (!consumer_->start()) {
        std::cerr << "Failed to start Kafka consumer." << std::endl;
        return false;
    }

    return true;
}

void SettlementService::start() {
    std::cout << "Settlement Service started. Waiting for events on topic: " << settings_.topic_payment_cleared << std::endl;
    running_ = true;

    while (running_) {
        // Poll and process messages
        consumer_->consume([this](const std::string& key, const std::string& payload) {
            this->handle_payment_cleared(key, payload);
        });

        // Small sleep to prevent tight loop in case of errors or stub mode
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
}

void SettlementService::stop() {
    if (running_) {
        std::cout << "Stopping Settlement Service..." << std::endl;
        running_ = false;
        consumer_->stop();
        producer_->stop();
    }
}

bool SettlementService::final_settlement_logic(const std::string& key, const std::string& payload) {
    // Here we would implement the logic for:
    // 1. Parsing the JSON payload
    // 2. Ensuring idempotency
    // 3. Updating the sharded PostgreSQL database (e.g. inserting into ledger_entries)
    // 4. Updating the transaction state

    std::cout << "Processing settlement logic for Key: " << key << std::endl;
    // simulated db work
    std::this_thread::sleep_for(std::chrono::milliseconds(50));
    
    return true; // Assume success for scaffolding
}

void SettlementService::handle_payment_cleared(const std::string& key, const std::string& payload) {
    std::cout << "Received payment_cleared event: " << key << std::endl;

    bool db_success = final_settlement_logic(key, payload);

    if (db_success) {
        // Construct payment_settled payload
        // In a real implementation we would build a JSON payload here
        std::string settled_payload = "{\"status\": \"settled\", \"original_payload\": " + (payload.empty() ? "{}" : payload) + "}";
        
        bool produced = producer_->produce(settings_.topic_payment_settled, key, settled_payload);
        if (produced) {
            std::cout << "Successfully produced payment_settled event for Key: " << key << std::endl;
        } else {
            std::cerr << "Failed to produce payment_settled event." << std::endl;
            // Handle failure, potentially publish to DLQ
        }
    } else {
        // Publish to DLQ if the final settlement logic fails critically
        producer_->produce(settings_.topic_dead_letter, key, payload);
    }
}

} // namespace settlement
