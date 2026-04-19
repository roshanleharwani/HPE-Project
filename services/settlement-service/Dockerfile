FROM ubuntu:22.04 AS builder

ENV DEBIAN_FRONTEND=noninteractive

# Install build dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    librdkafka-dev \
    libssl-dev \
    pkg-config

WORKDIR /app
COPY . .

# Build the project
RUN rm -rf build && mkdir build && cd build && cmake .. && make -j$(nproc)

FROM ubuntu:22.04 AS runtime

ENV DEBIAN_FRONTEND=noninteractive

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    librdkafka1 \
    librdkafka++1 \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/settlement_service /app/settlement_service

ENTRYPOINT ["/app/settlement_service"]
