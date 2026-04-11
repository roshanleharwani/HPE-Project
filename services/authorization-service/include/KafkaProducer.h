#pragma once
#include <librdkafka/rdkafkacpp.h>
#include <string>
#include <memory>
#include <iostream>

class KafkaProducer {
public:
    KafkaProducer(const std::string& brokers);
    ~KafkaProducer();

    bool publish(const std::string& topic, const std::string& payload);

private:
    std::unique_ptr<RdKafka::Producer> producer_;
};
