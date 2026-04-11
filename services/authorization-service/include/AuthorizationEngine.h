#pragma once
#include "PaymentModels.h"
#include "ConnectionPool.h"
#include <string>
#include <memory>

class AuthorizationEngine {
public:
    AuthorizationEngine(std::shared_ptr<ConnectionPool> pool);
    
    // Validates business rules against the database.
    // Returns true if authorized, false otherwise.
    bool authorize(const PaymentInitiatedEvent& event, std::string& out_reason);

private:
    std::shared_ptr<ConnectionPool> pool_;
};
