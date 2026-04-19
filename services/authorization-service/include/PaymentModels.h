#pragma once
#include <string>
#include <nlohmann/json.hpp>
#include <iostream>

struct PaymentInitiatedEvent {
    std::string transaction_id;
    std::string payment_intent_id;
    std::string payment_method_id;
    std::string user_id;
    double amount;
    std::string currency;
};

inline void from_json(const nlohmann::json& j, PaymentInitiatedEvent& p) {
    p.transaction_id = j.value("transaction_id", j.value("transactionId", ""));
    p.payment_intent_id = j.value("payment_intent_id", j.value("paymentIntentId", ""));
    p.payment_method_id = j.value("payment_method_id", j.value("paymentMethodId", ""));
    p.user_id = j.value("user_id", j.value("userId", ""));
    
    // Safely parse the amount whether it is a double or string
    auto amount_key = j.contains("amount") ? "amount" : "";
    if (amount_key != "") {
        if (j.at(amount_key).is_string()) {
            p.amount = std::stod(j.at(amount_key).get<std::string>());
        } else {
            p.amount = j.at(amount_key).get<double>();
        }
    } else {
        p.amount = 0.0;
    }
    
    p.currency = j.value("currency", "");
}

inline void to_json(nlohmann::json& j, const PaymentInitiatedEvent& p) {
    j = nlohmann::json{
        {"transactionId", p.transaction_id},
        {"paymentIntentId", p.payment_intent_id},
        {"paymentMethodId", p.payment_method_id},
        {"userId", p.user_id},
        {"amount", p.amount},
        {"currency", p.currency}
    };
}

struct PaymentAuthorizedEvent {
    std::string transaction_id;
    std::string payment_intent_id;
    std::string payment_method_id;
    std::string user_id;
    double amount;
    std::string currency;
    std::string status; // "AUTHORIZED" or "FAILED"
    std::string reason; // Detailed reason if failed
};

inline void from_json(const nlohmann::json& j, PaymentAuthorizedEvent& p) {
    p.transaction_id = j.value("transaction_id", j.value("transactionId", ""));
    p.payment_intent_id = j.value("payment_intent_id", j.value("paymentIntentId", ""));
    p.payment_method_id = j.value("payment_method_id", j.value("paymentMethodId", ""));
    p.user_id = j.value("user_id", j.value("userId", ""));
    if (j.contains("amount")) {
        if (j.at("amount").is_string()) {
            p.amount = std::stod(j.at("amount").get<std::string>());
        } else {
            p.amount = j.at("amount").get<double>();
        }
    } else {
        p.amount = 0.0;
    }
    p.currency = j.value("currency", "");
    p.status = j.value("status", "");
    p.reason = j.value("reason", "");
}

inline void to_json(nlohmann::json& j, const PaymentAuthorizedEvent& p) {
    j = nlohmann::json{
        {"transactionId", p.transaction_id},
        {"paymentIntentId", p.payment_intent_id},
        {"paymentMethodId", p.payment_method_id},
        {"userId", p.user_id},
        {"amount", p.amount},
        {"currency", p.currency},
        {"status", p.status},
        {"reason", p.reason}
    };
}
