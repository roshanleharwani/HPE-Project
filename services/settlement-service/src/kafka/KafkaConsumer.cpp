#include "KafkaConsumer.h"
#include <iostream>

#if __has_include(<librdkafka/rdkafkacpp.h>)
#define HAS_LIBRDKAFKA 1
#endif

namespace settlement {

KafkaConsumer::KafkaConsumer(const std::string& brokers, const std::string& group_id, const std::string& topic)
    : brokers_(brokers), group_id_(group_id), topic_(topic), running_(false) {
}

KafkaConsumer::~KafkaConsumer() {
    stop();
}

bool KafkaConsumer::init_kafka() {
#ifdef HAS_LIBRDKAFKA
    std::string errstr;

    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    conf->set("bootstrap.servers", brokers_, errstr);
    conf->set("group.id", group_id_, errstr);

    consumer_.reset(RdKafka::Consumer::create(conf, errstr));
    delete conf;

    if (!consumer_) {
        std::cerr << "Failed to create consumer: " << errstr << std::endl;
        return false;
    }

    std::vector<std::string> topics;
    topics.push_back(topic_);
    RdKafka::ErrorCode err = consumer_->subscribe(topics);
    if (err) {
        std::cerr << "Failed to subscribe to " << topic_ << ": " << RdKafka::err2str(err) << std::endl;
        return false;
    }
    return true;
#else
    std::cerr << "[STUB] KafkaConsumer::init_kafka called. librdkafka not found." << std::endl;
    return true;
#endif
}

bool KafkaConsumer::start() {
    if (!init_kafka()) return false;
    running_ = true;
    return true;
}

void KafkaConsumer::stop() {
    running_ = false;
#ifdef HAS_LIBRDKAFKA
    if (consumer_) {
        consumer_->close();
    }
#endif
}

void KafkaConsumer::consume(MessageCallback callback) {
    if (!running_) return;

#ifdef HAS_LIBRDKAFKA
    std::unique_ptr<RdKafka::Message> msg(consumer_->consume(1000));
    
    switch (msg->err()) {
        case RdKafka::ERR_NO_ERROR: // Real message
            if (callback) {
                std::string key = msg->key() ? *msg->key() : "";
                std::string payload = msg->payload() ? std::string(static_cast<const char*>(msg->payload()), msg->len()) : "";
                callback(key, payload);
            }
            break;
        case RdKafka::ERR__TIMED_OUT: // No message within timeout
            break;
        default:
            std::cerr << "Consume failed: " << msg->errstr() << std::endl;
            break;
    }
#else
    // Stub implementation
    // Simulate some event loop logic without hogging CPU
#endif
}

} // namespace settlement
