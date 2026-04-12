#ifndef KAFKA_PRODUCER_H
#define KAFKA_PRODUCER_H

#include <string>
#include <memory>

#if __has_include(<librdkafka/rdkafkacpp.h>)
#include <librdkafka/rdkafkacpp.h>
#else
namespace RdKafka { class Producer; class Topic; }
#endif

namespace settlement {

class KafkaProducer {
public:
    KafkaProducer(const std::string& brokers);
    ~KafkaProducer();

    bool start();
    void stop();
    bool produce(const std::string& topic, const std::string& key, const std::string& payload);

private:
    std::string brokers_;
    bool running_;

    std::unique_ptr<RdKafka::Producer> producer_;
    
    bool init_kafka();
};

} // namespace settlement

#endif // KAFKA_PRODUCER_H
