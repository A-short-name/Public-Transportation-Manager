package it.polito.wa2.g15.lab5.paymentservice.services

import it.polito.wa2.g15.lab5.paymentservice.entities.Transaction
import it.polito.wa2.g15.lab5.paymentservice.kafka.OrderInformationMessage
import it.polito.wa2.g15.lab5.paymentservice.kafka.PaymentInfo
import it.polito.wa2.g15.lab5.paymentservice.repositories.TransactionRepository
import kotlinx.coroutines.flow.Flow
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

@Service
class PaymentServiceImpl : PaymentService {
    @Autowired
    lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, OrderInformationMessage>

    @Value("\${kafka.topics.produce}")
    private lateinit var topic :String

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getTransactionsByUser(username: String) : Flow<Transaction> {
        return transactionRepository.getTransactionsByUsername(username)
    }

    override fun getAllTransactions() : Flow<Transaction> {
        return transactionRepository.findAll()
    }

    @KafkaListener(topics = ["\${kafka.topics.consume}"], groupId = "ppr")
    fun consumeMessage(message: OrderInformationMessage/*, ack: Acknowledgment*/) {
        logger.info("Message received {}", message)
        //ack.acknowledge()

        val messageToSend: Message<OrderInformationMessage> = MessageBuilder
                .withPayload(OrderInformationMessage(PaymentInfo("prr", LocalDate.now(), "prr"), 9999.999))
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader("X-Custom-Header", "Custom header here")
                .build()
        kafkaTemplate.send(messageToSend)
        logger.info("Message sent with success on topic: $topic")
    }
}