#include "../../include/AuthorizationEngine.h"
#include <iostream>
#include <pqxx/pqxx>

class ConnectionGuard {
    std::shared_ptr<ConnectionPool> pool_;
    std::shared_ptr<pqxx::connection> conn_;
public:
    ConnectionGuard(std::shared_ptr<ConnectionPool> pool, std::shared_ptr<pqxx::connection> conn) : pool_(pool), conn_(conn) {}
    ~ConnectionGuard() { pool_->return_connection(conn_); }
    pqxx::connection& get() { return *conn_; }
};

AuthorizationEngine::AuthorizationEngine(std::shared_ptr<ConnectionPool> pool) : pool_(pool) {}

bool AuthorizationEngine::authorize(const PaymentInitiatedEvent& event, std::string& out_reason) {
    ConnectionGuard guard(pool_, pool_->get_connection());
    auto& conn = guard.get();

    try {
        pqxx::nontransaction tx(conn);

        // Single optimized LEFT JOIN query cutting down 4 round-trips to 1
        std::string query = 
            "SELECT "
            "  u.id AS user_exists, "
            "  pi.status AS intent_status, "
            "  pi.amount AS intent_amount, "
            "  pm.method_type, "
            "  pm.expiry_year, "
            "  pm.expiry_month, "
            "  (SELECT COALESCE(SUM(amount), 0) FROM transactions "
            "   WHERE updated_at >= CURRENT_DATE "
            "   AND payment_intent_id IN (SELECT id FROM payment_intents WHERE user_id = " + tx.quote(event.user_id) + ") "
            "   AND status = 'AUTHORIZED') AS total_today "
            "FROM users u "
            "LEFT JOIN payment_intents pi ON pi.id = " + tx.quote(event.payment_intent_id) + " AND pi.user_id = u.id "
            "LEFT JOIN payment_methods pm ON pm.id = " + tx.quote(event.payment_method_id) + " AND pm.user_id = u.id "
            "WHERE u.id = " + tx.quote(event.user_id);

        pqxx::result r = tx.exec(query);
        if (r.empty()) {
            out_reason = "User not found";
            return false;
        }

        auto row = r[0];

        // 2. Verify payment intent exists and matches user
        if (row["intent_status"].is_null()) {
            out_reason = "Payment intent not found or mismatch";
            return false;
        }
        
        double intent_amount = row["intent_amount"].as<double>(0.0);
        if (intent_amount != event.amount) {
            out_reason = "Payment amount mismatch with intent";
            return false;
        }

        // 3. Verify Payment Method belongs to user
        if (row["method_type"].is_null()) {
            out_reason = "Payment method not found or does not belong to user";
            return false;
        }

        // 4. Expiration check for cards
        std::string method_type = row["method_type"].as<std::string>("");
        if (method_type == "CARD") {
            int exp_year = row["expiry_year"].as<int>(0);
            int exp_month = row["expiry_month"].as<int>(0);
            if (exp_year < 2024 || (exp_year == 2024 && exp_month < 4)) {
                out_reason = "Payment method expired";
                return false;
            }
        }

        // 5. Fraud/Limit Check
        double total_today = row["total_today"].as<double>(0.0);
        if (total_today + event.amount > 100000.0) {
            out_reason = "Fraud simulation: Daily transaction limit exceeded for this user";
            return false;
        }

        return true; 
    } catch (const pqxx::sql_error& e) {
        std::cerr << "SQL error: " << e.what() << "\nQuery was: " << e.query() << std::endl;
        out_reason = "Database error during validation";
        return false;
    } catch (const std::exception& e) {
        std::cerr << "DB Error during authorization: " << e.what() << std::endl;
        out_reason = "Internal verification error";
        return false;
    }
}
