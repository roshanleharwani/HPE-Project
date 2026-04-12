#include "../../include/KafkaProducer.h"
#include <iostream>

KafkaProducer::KafkaProducer(const std::string& brokers) {
    std::string errstr;
    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    conf->set("bootstrap.servers", brokers, errstr);

    RdKafka::Producer *producer = RdKafka::Producer::create(conf, errstr);
    if (!producer) {
        std::cerr << "Failed to create producer: " << errstr << std::endl;
        delete conf;
        throw std::runtime_error("Kafka Producer creation failed");
    }
    producer_.reset(producer);
    delete conf;
}

KafkaProducer::~KafkaProducer() {
    if (producer_) {
        producer_->flush(10000);
    }
}

bool KafkaProducer::publish(const std::string& topic, const std::string& payload) {
    RdKafka::ErrorCode err = producer_->produce(
        topic, RdKafka::Topic::PARTITION_UA,
        RdKafka::Producer::RK_MSG_COPY,
        const_cast<char*>(payload.c_str()), payload.size(),
        NULL, 0, 0, NULL, NULL);

    if (err != RdKafka::ERR_NO_ERROR) {
        std::cerr << "Failed to produce to topic " << topic << ": " << RdKafka::err2str(err) << std::endl;
        return false;
    }
    producer_->poll(0);
    return true;
}
