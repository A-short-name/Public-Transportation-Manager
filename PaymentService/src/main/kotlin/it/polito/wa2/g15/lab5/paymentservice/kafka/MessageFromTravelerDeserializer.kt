package it.polito.wa2.g15.lab5.paymentservice.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import kotlin.text.Charsets.UTF_8


class MessageFromTravelerDeserializer : Deserializer<OrderInformationMessage> {
    private val objectMapper = ObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun deserialize(topic: String?, data: ByteArray?): OrderInformationMessage? {
        objectMapper.findAndRegisterModules()
        log.info("Deserializing...")
        return objectMapper.readValue(
                String(
                        data ?: throw SerializationException("Error when deserializing byte[] to Product"), UTF_8
                ), OrderInformationMessage::class.java
        )
    }

    override fun close() {}

}