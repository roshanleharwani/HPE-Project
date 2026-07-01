package com.payments.clearing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.payments.clearing.producer.PaymentClearedProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ClearingService {

    private static final Logger log = LoggerFactory.getLogger(ClearingService.class);
    private final PaymentClearedProducer producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Standard platform fee: 2.9% + $0.30
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.029");
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.30");

    public ClearingService(PaymentClearedProducer producer) {
        this.producer = producer;
    }

    public void process(String message) {
        try {
            // 1. Parse the incoming JSON
            JsonNode root = objectMapper.readTree(message);
            String transactionId = root.path("transactionId").asText();
            BigDecimal originalAmount = new BigDecimal(root.path("amount").asText());

            // 2. Calculate the fees
            BigDecimal variableFee = originalAmount.multiply(FEE_PERCENTAGE);
            BigDecimal totalFee = variableFee.add(FIXED_FEE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal settlementAmount = originalAmount.subtract(totalFee);

            log.info("Clearing completed for Tx {}. Fee: ${}, Settlement: ${}", 
                     transactionId, totalFee, settlementAmount);

            // 3. Inject the calculated amounts back into the JSON
            ((ObjectNode) root).put("feeAmount", totalFee);
            ((ObjectNode) root).put("settlementAmount", settlementAmount);
            ((ObjectNode) root).put("status", "CLEARED");

            // 4. Send the enriched JSON downstream
            producer.send(root.toString());

        } catch (Exception e) {
            log.error("Failed to process clearing event: {}", e.getMessage());
            throw new RuntimeException("Clearing event processing failed", e);
        }
    }
}