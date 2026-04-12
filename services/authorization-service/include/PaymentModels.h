#pragma once
#include <string>
#include <nlohmann/json.hpp>

struct PaymentInitiatedEvent {
    std::string transaction_id;
    std::string payment_intent_id;
    std::string payment_method_id;
    std::string user_id;
    double amount;
    std::string currency;

    // This macro adds serialization and deserialization magic
    NLOHMANN_DEFINE_TYPE_INTRUSIVE(PaymentInitiatedEvent, transaction_id, payment_intent_id, payment_method_id, user_id, amount, currency)
};

struct PaymentAuthorizedEvent {
    std::string transaction_id;
    std::string status; // "AUTHORIZED" or "FAILED"
    std::string reason; // Detailed reason if failed

    NLOHMANN_DEFINE_TYPE_INTRUSIVE(PaymentAuthorizedEvent, transaction_id, status, reason)
};
