package it.polito.wa2.g15.lab5.kafka

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin

@Configuration
class KafkaTopicConfig(
        @Value("\${spring.kafka.bootstrap-servers}")
        private val servers: String,
        @Value("\${kafka.topics.listOfTopics}")
        private val listOfTopics :List<String>
) {

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs: MutableMap<String, Any?> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = servers
        return KafkaAdmin(configs)
    }

    @Bean
    fun topics(): List<NewTopic> {
        return listOfTopics.map { NewTopic(it, 1, 1.toShort()) }
    }
}