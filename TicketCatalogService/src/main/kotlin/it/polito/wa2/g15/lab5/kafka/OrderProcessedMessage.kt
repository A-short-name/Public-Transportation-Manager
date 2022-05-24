package it.polito.wa2.g15.lab5.kafka

import com.fasterxml.jackson.annotation.JsonProperty

data class OrderProcessedMessage (
        val accepted: Boolean,
        @JsonProperty("transaction_id")
        val transactionId: Long,
        val message: String,
        @JsonProperty("order_id")
        val orderId: Long
)