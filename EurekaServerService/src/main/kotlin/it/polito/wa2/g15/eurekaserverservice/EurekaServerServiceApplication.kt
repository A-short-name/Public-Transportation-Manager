package it.polito.wa2.g15.eurekaserverservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

@SpringBootApplication
@EnableEurekaServer
class EurekaServerServiceApplication

fun main(args: Array<String>) {
	runApplication<EurekaServerServiceApplication>(*args)
}
