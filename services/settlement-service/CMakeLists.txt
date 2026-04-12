cmake_minimum_required(VERSION 3.14)
project(SettlementService CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Find threads
find_package(Threads REQUIRED)

# Try to find librdkafka via pkg-config or standard ways
# In a real environment, you might use Conan, vcpkg, or system packages.
# Assuming librdkafka++ is installed and findable:
find_path(RDKAFKA_INCLUDE_DIR NAMES librdkafka/rdkafkacpp.h)
find_library(RDKAFKA_LIBRARY NAMES rdkafka++)
find_library(RDKAFKA_C_LIBRARY NAMES rdkafka)

if (RDKAFKA_INCLUDE_DIR AND RDKAFKA_LIBRARY)
    message(STATUS "Found librdkafka++: ${RDKAFKA_LIBRARY}")
else()
    message(WARNING "librdkafka++ not found. Please ensure it is installed if building. Creating a stub executable for scaffolding.")
endif()

include_directories(src)
if (RDKAFKA_INCLUDE_DIR)
    include_directories(${RDKAFKA_INCLUDE_DIR})
endif()

file(GLOB_RECURSE SOURCES "src/*.cpp")

add_executable(settlement_service ${SOURCES})

target_link_libraries(settlement_service PRIVATE Threads::Threads)
if (RDKAFKA_LIBRARY AND RDKAFKA_C_LIBRARY)
    target_link_libraries(settlement_service PRIVATE ${RDKAFKA_LIBRARY} ${RDKAFKA_C_LIBRARY})
endif()
