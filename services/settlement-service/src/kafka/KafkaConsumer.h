#ifndef KAFKA_CONSUMER_H
#define KAFKA_CONSUMER_H

#include <string>
#include <functional>
#include <memory>
#if __has_include(<librdkafka/rdkafkacpp.h>)
#include <librdkafka/rdkafkacpp.h>
#else
namespace RdKafka { class KafkaConsumer; class Message; class Conf; }
#endif

namespace settlement {

class KafkaConsumer {
public:
    using MessageCallback = std::function<void(const std::string& key, const std::string& payload)>;

    KafkaConsumer(const std::string& brokers, const std::string& group_id, const std::string& topic);
    ~KafkaConsumer();

    bool start();
    void stop();
    void consume(MessageCallback callback);

private:
    std::string brokers_;
    std::string group_id_;
    std::string topic_;
    bool running_;

    std::unique_ptr<RdKafka::KafkaConsumer> consumer_;
    
    bool init_kafka();
};

} // namespace settlement

#endif // KAFKA_CONSUMER_H

