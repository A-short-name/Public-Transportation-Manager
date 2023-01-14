package it.polito.wa2.g15.springcloudgateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringCloudGatewayApplication

fun main(args: Array<String>) {
    runApplication<SpringCloudGatewayApplication>(*args)
}
