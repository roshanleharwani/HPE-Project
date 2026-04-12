#ifndef KAFKA_SETTINGS_H
#define KAFKA_SETTINGS_H

#include <string>

namespace settlement {

struct KafkaSettings {
    std::string brokers = "localhost:9092";
    std::string consumer_group_id = "settlement-service-group";
    std::string topic_payment_cleared = "payment_cleared";
    std::string topic_payment_settled = "payment_settled";
    std::string topic_dead_letter = "payment_dead_letter_queue";
};

} // namespace settlement

#endif // KAFKA_SETTINGS_H
