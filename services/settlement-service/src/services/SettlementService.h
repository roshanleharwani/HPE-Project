#ifndef SETTLEMENT_SERVICE_H
#define SETTLEMENT_SERVICE_H

#include "../config/KafkaSettings.h"
#include "../kafka/KafkaConsumer.h"
#include "../kafka/KafkaProducer.h"
#include <memory>
#include <string>

namespace settlement {

class SettlementService {
public:
    SettlementService(const KafkaSettings& settings);
    ~SettlementService();

    bool init();
    void start();
    void stop();

private:
    KafkaSettings settings_;
    std::unique_ptr<KafkaConsumer> consumer_;
    std::unique_ptr<KafkaProducer> producer_;
    bool running_;

    void handle_payment_cleared(const std::string& key, const std::string& payload);
    bool final_settlement_logic(const std::string& key, const std::string& payload);
};

} // namespace settlement

#endif // SETTLEMENT_SERVICE_H
