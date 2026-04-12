#include "KafkaProducer.h"
#include <iostream>

#if __has_include(<librdkafka/rdkafkacpp.h>)
#define HAS_LIBRDKAFKA 1
#endif

namespace settlement {

KafkaProducer::KafkaProducer(const std::string& brokers)
    : brokers_(brokers), running_(false) {
}

KafkaProducer::~KafkaProducer() {
    stop();
}

bool KafkaProducer::init_kafka() {
#ifdef HAS_LIBRDKAFKA
    std::string errstr;

    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    conf->set("bootstrap.servers", brokers_, errstr);

    producer_.reset(RdKafka::Producer::create(conf, errstr));
    delete conf;

    if (!producer_) {
        std::cerr << "Failed to create producer: " << errstr << std::endl;
        return false;
    }

    return true;
#else
    std::cerr << "[STUB] KafkaProducer::init_kafka called. librdkafka not found." << std::endl;
    return true;
#endif
}

bool KafkaProducer::start() {
    if (!init_kafka()) return false;
    running_ = true;
    return true;
}

void KafkaProducer::stop() {
    running_ = false;
#ifdef HAS_LIBRDKAFKA
    if (producer_) {
        // flush before giving up
        producer_->flush(5000);
    }
#endif
}

bool KafkaProducer::produce(const std::string& topic, const std::string& key, const std::string& payload) {
    if (!running_) return false;

#ifdef HAS_LIBRDKAFKA
    RdKafka::ErrorCode err = producer_->produce(
        topic,
        RdKafka::Topic::PARTITION_UA,
        RdKafka::Producer::RK_MSG_COPY,
        const_cast<char *>(payload.c_str()), payload.size(),
        key.c_str(), key.size(),
        0, NULL, NULL);

    if (err != RdKafka::ERR_NO_ERROR) {
        std::cerr << "Failed to produce to topic " << topic << ": " << RdKafka::err2str(err) << std::endl;
        return false;
    }

    producer_->poll(0); // Poll for events like delivery reports
    return true;
#else
    std::cout << "[STUB] KafkaProducer produced message to " << topic << " | Key: " << key << " | Payload: " << payload << std::endl;
    return true;
#endif
}

} // namespace settlement
