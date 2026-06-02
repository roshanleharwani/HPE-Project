#include "SettlementService.h"
#include <iostream>
#include <chrono>
#include <thread>
#include <nlohmann/json.hpp>

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
    // 1. Parse the incoming payment_cleared JSON payload
    nlohmann::json root;
    try {
        root = nlohmann::json::parse(payload);
    } catch (const nlohmann::json::parse_error& e) {
        std::cerr << "[Settlement] Failed to parse payment_cleared payload: " << e.what() << std::endl;
        return false;
    }

    // 2. Validate required fields are present
    // Accept both camelCase (from auth service serializer) and snake_case defensively
    std::string txn_id = root.value("transactionId", root.value("transaction_id", ""));
    if (txn_id.empty()) {
        std::cerr << "[Settlement] Rejected event with empty transactionId for key: " << key << std::endl;
        return false;
    }

    std::string status = root.value("status", "");
    double amount = 0.0;
    if (root.contains("amount")) {
        if (root["amount"].is_string()) {
            amount = std::stod(root["amount"].get<std::string>());
        } else {
            amount = root["amount"].get<double>();
        }
    }

    std::cout << "[Settlement] Processing txn=" << txn_id
              << " status=" << status
              << " amount=" << amount << std::endl;

    // 3. Only settle payments that were CLEARED
    if (status != "CLEARED") {
        std::cerr << "[Settlement] Unexpected status '" << status << "' for txn " << txn_id
                  << " — expected CLEARED. Skipping." << std::endl;
        return false;
    }

    return true;
}

void SettlementService::handle_payment_cleared(const std::string& key, const std::string& payload) {
    std::cout << "[Settlement] Received payment_cleared event, key=" << key << std::endl;

    bool db_success = final_settlement_logic(key, payload);

    if (db_success) {
        nlohmann::json inner;
        try {
            inner = nlohmann::json::parse(payload);
        } catch (...) {
            inner = nlohmann::json::object();
        }

        nlohmann::json settled_json;
        settled_json["status"] = "settled";
        settled_json["original_payload"] = inner;  

        std::string settled_payload = settled_json.dump();

        bool produced = producer_->produce(settings_.topic_payment_settled, key, settled_payload);
        if (produced) {
            std::cout << "[Settlement] Successfully produced payment_settled for txn key=" << key << std::endl;
        } else {
            std::cerr << "[Settlement] Failed to produce payment_settled event." << std::endl;
        }
    } else {
        // Publish to DLQ if the settlement logic fails
        producer_->produce(settings_.topic_dead_letter, key, payload);
        std::cerr << "[Settlement] Sent to DLQ: " << key << std::endl;
    }
}

} // namespace settlement
