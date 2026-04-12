#pragma once
#include <librdkafka/rdkafkacpp.h>
#include <string>
#include <memory>
#include <functional>
#include <vector>

class KafkaConsumer {
public:
    using MessageCallback = std::function<void(const std::string&)>;

    KafkaConsumer(const std::string& brokers, const std::string& group_id, const std::string& topic);
    ~KafkaConsumer();

    // Starts consuming messages. Blocks until running is set to false.
    void start(MessageCallback callback, const volatile bool& running);

private:
    std::unique_ptr<RdKafka::KafkaConsumer> consumer_;
    std::string topic_;
};
