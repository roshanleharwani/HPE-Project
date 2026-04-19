cmake_minimum_required(VERSION 3.14)
project(authorization-service)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# nlohmann/json is installed via apt (nlohmann-json3-dev) in the Dockerfile
find_package(nlohmann_json 3.2.0 REQUIRED)

find_package(PkgConfig REQUIRED)
pkg_check_modules(RDKAFKA REQUIRED rdkafka++)
pkg_check_modules(PQXX REQUIRED libpqxx)

# Include directories
include_directories(include)
include_directories(${RDKAFKA_INCLUDE_DIRS})
include_directories(${PQXX_INCLUDE_DIRS})

# Source files
file(GLOB SOURCES "src/*.cpp" "src/kafka/*.cpp" "src/services/*.cpp" "src/config/*.cpp" "src/models/*.cpp")

add_executable(authorization_service ${SOURCES})

target_link_libraries(authorization_service
    PRIVATE
    ${RDKAFKA_LIBRARIES}
    ${PQXX_LIBRARIES}
    nlohmann_json::nlohmann_json
    pthread
)

target_compile_options(authorization_service PRIVATE ${RDKAFKA_CFLAGS_OTHER} ${PQXX_CFLAGS_OTHER})
