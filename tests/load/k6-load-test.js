import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'; // To generate unique transaction IDs

export const options = {
  discardResponseBodies: true, // Discard response bodies to save memory and CPU on the load generator
  scenarios: {
    constant_request_rate: {
      executor: 'constant-arrival-rate',
      rate: 10000, // 10,000 requests per second
      timeUnit: '1s',
      duration: '2m', // Run the test for 2 minutes
      preAllocatedVUs: 2000, // Pre-allocate Virtual Users to handle the initial burst
      maxVUs: 10000, // Max VUs to scale up to if response times are slow
    },
  },
};

export default function () {
  // Replace with the internal DNS or IP of your API Gateway Load Balancer
  const url = 'http://18.60.209.69:8080/api/v1/payments';
  
  const payload = JSON.stringify({
    transactionId: uuidv4(), // Ensures no 409 Conflict from duplicate transactions
    userId: 'user-' + Math.floor(Math.random() * 10000),
    paymentMethodId: 'pm-123456',
    amount: (Math.random() * 1000).toFixed(2),
    currency: 'USD',
    description: 'k6 load test payment'
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  const res = http.post(url, payload, params);
  
  check(res, {
    'is status 200': (r) => r.status === 200,
  });
}
