#include "../../include/AuthorizationEngine.h"
#include <iostream>
#include <sstream>
#include <iomanip>
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

        // Single optimized LEFT JOIN query using $1..$3 placeholders (SQL-injection safe)
        std::string query = 
            "SELECT "
            "  u.id AS user_exists, "
            "  pi.status AS intent_status, "
            "  pi.amount::text AS intent_amount, "
            "  pm.method_type, "
            "  pm.expiry_year, "
            "  pm.expiry_month, "
            // FIX 1: created_at is indexed (idx_transactions_created); updated_at is NOT
            "  (SELECT COALESCE(SUM(amount), 0) FROM transactions "
            "   WHERE created_at >= CURRENT_DATE "
            "   AND payment_intent_id IN (SELECT id FROM payment_intents WHERE user_id = $1::uuid) "
            // FIX 2: transactions.status is a custom enum type, must cast the literal
            "   AND status = 'AUTHORIZED'::transaction_status) AS total_today "
            "FROM users u "
            "LEFT JOIN payment_intents pi ON pi.id = $2::uuid AND pi.user_id = u.id "
            "LEFT JOIN payment_methods pm ON pm.id = $3::uuid AND pm.user_id = u.id "
            "WHERE u.id = $1::uuid";

        pqxx::result r = tx.exec_params(query,
            event.user_id,
            event.payment_intent_id,
            event.payment_method_id
        );
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

        // FIX 3: amount is numeric(15,2) in DB. std::to_string(double) uses
        // %g-style formatting which can produce "13644.959999" for a value that
        // should be "13644.96". The old naive truncation (cutting after 2dp) could
        // yield the wrong digit if the last digit wasn't rounded up.
        // Fix: use std::fixed + std::setprecision(2) which correctly rounds.
        std::string db_amount_str = row["intent_amount"].as<std::string>("");
        std::ostringstream oss;
        oss << std::fixed << std::setprecision(2) << event.amount;
        std::string event_amount_str = oss.str();
        if (db_amount_str != event_amount_str) {
            out_reason = "Payment amount mismatch with intent";
            return false;
        }

        // 3. Verify Payment Method belongs to user
        if (row["method_type"].is_null()) {
            out_reason = "Payment method not found or does not belong to user";
            return false;
        }

        // 4. Expiration check for cards using DB-side current year/month
        std::string method_type = row["method_type"].as<std::string>("");
        if (method_type == "CARD") {
            pqxx::result date_r = tx.exec("SELECT EXTRACT(YEAR FROM CURRENT_DATE)::int AS yr, EXTRACT(MONTH FROM CURRENT_DATE)::int AS mo");
            int cur_year  = date_r[0]["yr"].as<int>();
            int cur_month = date_r[0]["mo"].as<int>();
            int exp_year  = row["expiry_year"].as<int>(0);
            int exp_month = row["expiry_month"].as<int>(0);
            if (exp_year < cur_year || (exp_year == cur_year && exp_month < cur_month)) {
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
        out_reason = std::string("Database error during validation: ") + e.what();
        return false;
    } catch (const std::exception& e) {
        std::cerr << "DB Error during authorization: " << e.what() << std::endl;
        out_reason = std::string("Internal verification error: ") + e.what();
        return false;
    }
}
