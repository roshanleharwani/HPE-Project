FROM ubuntu:22.04 AS builder

ENV DEBIAN_FRONTEND=noninteractive

# Install build dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    librdkafka-dev \
    libpqxx-dev \
    nlohmann-json3-dev \
    libssl-dev \
    pkg-config

WORKDIR /app
COPY . .

# Build the project
RUN rm -rf build && mkdir build && cd build && cmake .. && make -j$(nproc)

FROM ubuntu:22.04 AS runtime

ENV DEBIAN_FRONTEND=noninteractive

# Install runtime dependencies. 
# libpqxx-dev is included here to ensure the .so files are present regardless of exact version string, 
# although in a true prod image we'd pin the specific runtime e.g. libpqxx-6.4
RUN apt-get update && apt-get install -y \
    librdkafka1 \
    librdkafka++1 \
    libpq5 \
    libpqxx-dev \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/authorization_service /app/authorization_service

ENTRYPOINT ["/app/authorization_service"]
