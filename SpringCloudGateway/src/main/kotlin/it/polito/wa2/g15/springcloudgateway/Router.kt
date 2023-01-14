package it.polito.wa2.g15.springcloudgateway

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Router {

    @Bean
    fun routes(builder:RouteLocatorBuilder): RouteLocator {
        return builder.routes()
                .route("traveler-service") {
                    it.path("/traveler-service/**")
                            .filters { f -> f.rewritePath("/traveler-service/(?<segment>.*)", "/\${segment}") }
                            .uri("lb://traveler")
                }
                .route("ticket-catalog-service") {
                    it.path("/ticket-catalog-service/**")
                            .filters { f -> f.rewritePath("/ticket-catalog-service/(?<segment>.*)", "/\${segment}") }
                            .uri("lb://ticket-catalog")
                }
                .route("payment-service") {
                    it.path("/payment-service/**")
                            .filters { f -> f.rewritePath("/payment-service/(?<segment>.*)", "/\${segment}") }
                            .uri("lb://payment")
                }
                .route("login-service") {
                    it.path("/login-service/**")
                        .filters { f -> f.rewritePath("/login-service/(?<segment>.*)", "/\${segment}") }
                        .uri("lb://login")
                }
                .route("validator-service") {
                    it.path("/validator-service/**")
                        .filters { f -> f.rewritePath("/validator-service/(?<segment>.*)", "/\${segment}") }
                        .uri("lb://validator")
                }
                .build()
    }
}