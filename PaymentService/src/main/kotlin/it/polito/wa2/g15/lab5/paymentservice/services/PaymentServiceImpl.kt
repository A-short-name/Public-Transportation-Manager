package it.polito.wa2.g15.lab5.paymentservice.services

import it.polito.wa2.g15.lab5.paymentservice.dtos.TransactionDTO
import it.polito.wa2.g15.lab5.paymentservice.dtos.toDTO
import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import it.polito.wa2.g15.lab5.paymentservice.kafka.OrderInformationMessage
import it.polito.wa2.g15.lab5.paymentservice.kafka.OrderProcessedMessage
import it.polito.wa2.g15.lab5.paymentservice.kafka.PaymentInfo
import it.polito.wa2.g15.lab5.paymentservice.repositories.TransactionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.validation.constraints.NotBlank

@Service
class PaymentServiceImpl : PaymentService {
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, OrderProcessedMessage>

    @Value("\${kafka.topics.produce}")
    private lateinit var topic :String

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getTransactionsByUser(username: String) : Flow<TransactionDTO> {
        return transactionRepository.getTransactionsByUsername(username).map { it.toDTO() }
    }

    override fun getAllTransactions() : Flow<TransactionDTO> {
        return transactionRepository.findAll().map { it.toDTO() }
    }
    @KafkaListener(topics = ["\${kafka.topics.consume}"], groupId = "ppr")
    fun performPayment(message: OrderInformationMessage) {
        logger.info("Message received {}", message)
        CoroutineScope(CoroutineName("Obliged coroutines")).also { it.launch { performPaymentSuspendable(message) } }
    }

    suspend fun performPaymentSuspendable(message: OrderInformationMessage) {

        /*  Pagamento accordato
        * Capire se bisogna accettare randomicamente oppure accordare sempre
        * */
        val transaction: Transaction
        try {
            val (creditCardNumber, exp, csv, cardHolder)= message.billingInfo
            transaction = transactionRepository.save(Transaction(username = message.username, creditCardNumber = creditCardNumber, exp = exp, csv = csv, cardHolder = cardHolder, totalCost = message.totalCost, orderId = message.orderId))
        } catch (e: Exception) {
            val messageToSend: Message<OrderProcessedMessage> = MessageBuilder
                            .withPayload(OrderProcessedMessage(false, -1, "Payment declined, transaction not created", message.orderId))
                            .setHeader(KafkaHeaders.TOPIC, topic)
                            .setHeader("X-Custom-Header", "Custom header here")
                            .build()
            kafkaTemplate.send(messageToSend)
            logger.info("Message sent with success on topic: $topic")
            throw Exception("Failed saving transaction info: ${e.message}")
        }


        val messageToSend: Message<OrderProcessedMessage> = MessageBuilder
                .withPayload( OrderProcessedMessage(true, transaction.id!!, "Payment successful, transaction created", transaction.orderId))
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("X-Custom-Header", "Custom header here")
                .build()
        kafkaTemplate.send(messageToSend)
        logger.info("Message sent with success on topic: $topic")
    }
}