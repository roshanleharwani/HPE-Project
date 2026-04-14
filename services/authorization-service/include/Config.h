// include/Config.h
#pragma once
#include <string>

class Config {
public:
    // Core function to read an environment variable, with a fallback
    static std::string getEnv(const std::string& key, const std::string& default_value = "");
    
    // Specific getters for our AWS Infrastructure
    static std::string getKafkaBrokers();
    static std::string getDbHost();
    static std::string getDbPort();
    static std::string getDbUser();
    static std::string getDbPassword();
    static std::string getDbName();
};