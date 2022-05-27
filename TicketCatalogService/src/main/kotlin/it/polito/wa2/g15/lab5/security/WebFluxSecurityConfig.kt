package it.polito.wa2.g15.lab5.security


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class WebFluxSecurityConfig {

//    @Autowired
//    private lateinit var jwtFilter: JWTAuthenticationWebFilter

    @Autowired
    private lateinit var authenticationManager: AuthenticationManager
    @Autowired
    private lateinit var securityContextRepository: SecurityContextRepository

    @Bean
    fun csrfTokenRepository(): ServerCsrfTokenRepository {
        // A CsrfTokenRepository that persists the CSRF token in a cookie named "XSRF-TOKEN" and reads from the header
        // "X-XSRF-TOKEN" following the conventions of AngularJS. When using with AngularJS be sure to use
        // withHttpOnlyFalse().
        return CookieServerCsrfTokenRepository.withHttpOnlyFalse()
    }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {
        return http
            .exceptionHandling()
            .authenticationEntryPoint { swe: ServerWebExchange, _: AuthenticationException? ->
                Mono.fromRunnable { swe.response.statusCode = HttpStatus.UNAUTHORIZED }
            }.accessDeniedHandler { swe: ServerWebExchange, _: AccessDeniedException? ->
                Mono.fromRunnable { swe.response.statusCode = HttpStatus.FORBIDDEN }
            }.and()
            .csrf().disable()
            //.csrf().csrfTokenRepository(csrfTokenRepository())
            //.and()
            .formLogin().disable()
            .httpBasic().disable()
            .authenticationManager(authenticationManager)
            .securityContextRepository(securityContextRepository)
            .authorizeExchange()
//            .pathMatchers(HttpMethod.OPTIONS).permitAll()
//            .pathMatchers("/login").permitAll()
            .anyExchange().permitAll()
            .and().build()
    }

}