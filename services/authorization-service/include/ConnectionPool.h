#pragma once
#include <pqxx/pqxx>
#include <string>
#include <vector>
#include <memory>
#include <mutex>
#include <condition_variable>

class ConnectionPool {
public:
    ConnectionPool(const std::string& conn_str, size_t pool_size);
    ~ConnectionPool() = default;

    std::shared_ptr<pqxx::connection> get_connection();
    void return_connection(std::shared_ptr<pqxx::connection> conn);

private:
    std::string conn_str_;
    std::vector<std::shared_ptr<pqxx::connection>> pool_;
    std::mutex mutex_;
    std::condition_variable condition_;
};
