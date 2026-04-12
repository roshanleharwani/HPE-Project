#include "services/SettlementService.h"
#include <iostream>
#include <csignal>
#include <cstdlib>

// Global pointer for signal handler
settlement::SettlementService* g_service = nullptr;

void signal_handler(int signal) {
    std::cout << "\nReceived signal " << signal << ", initiating graceful shutdown..." << std::endl;
    if (g_service) {
        g_service->stop();
    }
}

int main() {
    std::cout << "Starting Settlement Service..." << std::endl;

    // Load configuration (typically from environment variables or a config file)
    settlement::KafkaSettings settings;
    
    // In a real environment, you'd override defaults like this:
    if (const char* env_brokers = std::getenv("KAFKA_BROKERS")) {
        settings.brokers = env_brokers;
    }

    settlement::SettlementService service(settings);
    g_service = &service;

    // Register signal handlers for graceful shutdown
    std::signal(SIGINT, signal_handler);
    std::signal(SIGTERM, signal_handler);

    // Initialize and start service
    if (service.init()) {
        service.start();
    } else {
        std::cerr << "Failed to initialize Settlement Service. Exiting." << std::endl;
        return EXIT_FAILURE;
    }

    std::cout << "Settlement Service shutdown complete." << std::endl;
    return EXIT_SUCCESS;
}
