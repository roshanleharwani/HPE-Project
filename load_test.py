import concurrent.futures
import requests
import time
import sys
import collections

# Replace with your actual endpoint
TARGET_URL = "http://18.60.209.69/api/v1/payment-methods"
# Number of concurrent threads sending requests
CONCURRENT_THREADS = 50
# Total number of requests to send per thread
REQUESTS_PER_THREAD = 100

def send_requests(thread_id):
    results = collections.Counter()
    
    for _ in range(REQUESTS_PER_THREAD):
        try:
            # We use a custom User-Agent to easily identify this traffic in NGINX logs
            headers = {'User-Agent': 'LoadTester/1.0'}
            response = requests.get(TARGET_URL, headers=headers, timeout=5)
            
            # Record the exact status code
            results[f"HTTP {response.status_code}"] += 1
                
        except requests.exceptions.Timeout:
            results["Timeout (5s)"] += 1
        except requests.exceptions.ConnectionError as e:
            results["Connection Error"] += 1
        except requests.exceptions.RequestException as e:
            results[f"Other Error: {type(e).__name__}"] += 1

    return results

def main():
    print(f"Starting Layer 7 HTTP Flood simulation on {TARGET_URL}")
    print(f"Threads: {CONCURRENT_THREADS}, Total Requests: {CONCURRENT_THREADS * REQUESTS_PER_THREAD}")
    
    start_time = time.time()
    
    total_results = collections.Counter()

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_THREADS) as executor:
        futures = [executor.submit(send_requests, i) for i in range(CONCURRENT_THREADS)]
        
        for future in concurrent.futures.as_completed(futures):
            thread_results = future.result()
            total_results.update(thread_results)

    end_time = time.time()
    
    print("\n--- Test Results ---")
    print(f"Time taken: {end_time - start_time:.2f} seconds")
    
    print("\n--- Breakdown of Responses ---")
    for key, count in total_results.most_common():
        print(f"{key}: {count} requests")
    print("------------------------------")
    
    if total_results.get("HTTP 200", 0) == 0:
        print("\nNote: 0 successful HTTP 200 responses. Check the breakdown to see if it was NGINX blocking (e.g. 503 for Rate Limiting, 403 for WAF) or a connection issue.")

if __name__ == "__main__":
    main()
