// src/config/Config.cpp
#include "Config.h"
#include <cstdlib>

std::string Config::getEnv(const std::string& key, const std::string& default_value) {
    const char* val = std::getenv(key.c_str());
    if (val == nullptr) {
        return default_value; // Fallback if the variable doesn't exist
    }
    return std::string(val);
}

// These strictly match the variables in our authorization.yaml!
std::string Config::getKafkaBrokers() { return getEnv("KAFKA_BROKERS", "localhost:9092"); }
std::string Config::getDbHost()       { return getEnv("DB_HOST", "localhost"); }
std::string Config::getDbPort()       { return getEnv("DB_PORT", "5432"); }
std::string Config::getDbUser()       { return getEnv("DB_USER", "postgres"); }
std::string Config::getDbPassword()   { return getEnv("DB_PASSWORD", "postgres"); }
std::string Config::getDbName()       { return getEnv("DB_NAME", "postgres"); }