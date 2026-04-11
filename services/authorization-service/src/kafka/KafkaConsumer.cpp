#include "../../include/KafkaConsumer.h"
#include <iostream>

KafkaConsumer::KafkaConsumer(const std::string& brokers, const std::string& group_id, const std::string& topic) : topic_(topic) {
    std::string errstr;
    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    conf->set("bootstrap.servers", brokers, errstr);
    conf->set("group.id", group_id, errstr);
    conf->set("auto.offset.reset", "earliest", errstr);

    RdKafka::KafkaConsumer *consumer = RdKafka::KafkaConsumer::create(conf, errstr);
    if (!consumer) {
        std::cerr << "Failed to create consumer: " << errstr << std::endl;
        delete conf;
        throw std::runtime_error("Kafka Consumer creation failed");
    }
    consumer_.reset(consumer);
    delete conf;
}

KafkaConsumer::~KafkaConsumer() {
    if (consumer_) {
        consumer_->close();
    }
}

void KafkaConsumer::start(MessageCallback callback, const volatile bool& running) {
    std::vector<std::string> topics = { topic_ };
    RdKafka::ErrorCode err = consumer_->subscribe(topics);
    if (err != RdKafka::ERR_NO_ERROR) {
        std::cerr << "Failed to subscribe to " << topic_ << ": " << RdKafka::err2str(err) << std::endl;
        return;
    }

    std::cout << "Consuming messages from " << topic_ << std::endl;

    while (running) {
        RdKafka::Message *msg = consumer_->consume(1000);
        
        switch (msg->err()) {
            case RdKafka::ERR__TIMED_OUT:
                break;
            case RdKafka::ERR_NO_ERROR:
                callback(std::string(static_cast<const char*>(msg->payload()), msg->len()));
                break;
            default:
                std::cerr << "Consume error: " << msg->errstr() << std::endl;
                break;
        }
        delete msg;
    }
}
