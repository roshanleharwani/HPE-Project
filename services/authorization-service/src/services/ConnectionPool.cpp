#include "../../include/ConnectionPool.h"

ConnectionPool::ConnectionPool(const std::string& conn_str, size_t pool_size) : conn_str_(conn_str) {
    for (size_t i = 0; i < pool_size; ++i) {
        pool_.push_back(std::make_shared<pqxx::connection>(conn_str_));
    }
}

std::shared_ptr<pqxx::connection> ConnectionPool::get_connection() {
    std::unique_lock<std::mutex> lock(mutex_);
    condition_.wait(lock, [this] { return !pool_.empty(); });
    
    auto conn = pool_.back();
    pool_.pop_back();
    lock.unlock();
    
    // Check if the connection is alive, reconnect if dead
    try {
        if (!conn || !conn->is_open()) {
            conn = std::make_shared<pqxx::connection>(conn_str_);
        }
    } catch (...) {
        // If we fail to reconnect, return it anyway and let the engine handle the error
    }
    
    return conn;
}

void ConnectionPool::return_connection(std::shared_ptr<pqxx::connection> conn) {
    std::unique_lock<std::mutex> lock(mutex_);
    pool_.push_back(conn);
    lock.unlock();
    condition_.notify_one();
}
