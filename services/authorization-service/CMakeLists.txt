cmake_minimum_required(VERSION 3.14)
project(authorization-service)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# Fetch nlohmann/json so we don't rely on host package
FetchContent_Declare(
    json
    URL https://github.com/nlohmann/json/releases/download/v3.11.2/json.tar.xz
)
FetchContent_MakeAvailable(json)

find_package(PkgConfig REQUIRED)
pkg_check_modules(RDKAFKA REQUIRED rdkafka++)
pkg_check_modules(PQXX REQUIRED libpqxx)

# Include directories
include_directories(include)
include_directories(${RDKAFKA_INCLUDE_DIRS})
include_directories(${PQXX_INCLUDE_DIRS})

# Source files (we will add them as we create them)
file(GLOB SOURCES "src/*.cpp" "src/kafka/*.cpp" "src/services/*.cpp" "src/models/*.cpp")

add_executable(authorization_service ${SOURCES})

target_link_libraries(authorization_service
    PRIVATE
    ${RDKAFKA_LIBRARIES}
    ${PQXX_LIBRARIES}
    nlohmann_json::nlohmann_json
)
