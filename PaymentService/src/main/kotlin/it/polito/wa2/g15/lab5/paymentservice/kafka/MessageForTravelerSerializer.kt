package it.polito.wa2.g15.lab5.paymentservice.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory
import kotlin.text.Charsets.UTF_8


class MessageForTravelerSerializer : Serializer<OrderInformationMessage> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun serialize(topic: String?, data: OrderInformationMessage?): ByteArray? {
        objectMapper.findAndRegisterModules()
        log.info("Serializing... to send to topic $topic")
        return objectMapper.writeValueAsBytes(
                data ?: throw SerializationException("Error when serializing OrderInformationForPayment to ByteArray[]")
        )
    }

    override fun close() {}
}