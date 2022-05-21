package it.polito.wa2.g15.lab4.security


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired
    private lateinit var jwtFilterForUser: JWTAuthenticationFilterUser
    @Autowired
    private lateinit var jwtFilterForServices: JWTAuthenticationFilterServices

    @Bean
    fun csrfTokenRepository(): CsrfTokenRepository {
        // A CsrfTokenRepository that persists the CSRF token in a cookie named "XSRF-TOKEN" and reads from the header
        // "X-XSRF-TOKEN" following the conventions of AngularJS. When using with AngularJS be sure to use
        // withHttpOnlyFalse().
        return CookieCsrfTokenRepository.withHttpOnlyFalse()
    }

    //Used in test
    fun generateCsrfHeader(csrfTokenRepository:CsrfTokenRepository): HttpHeaders {
        
        val headers = HttpHeaders()
        val csrfToken: CsrfToken = csrfTokenRepository.generateToken(null)

        headers.add(csrfToken.headerName, csrfToken.token)
        headers.add("Cookie", "XSRF-TOKEN=" + csrfToken.token)
        
        return headers
    }
    
    override fun configure(http: HttpSecurity) {

        http.csrf().csrfTokenRepository(csrfTokenRepository())
                .and()
                .addFilterBefore(jwtFilterForUser, UsernamePasswordAuthenticationFilter::class.java)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                //.antMatcher("/catalog/**")
                // .authorizeRequests() //
                //.anyRequest().authenticated() //
                //.and()
                //.addFilterBefore(jwtFilterForServices, UsernamePasswordAuthenticationFilter::class.java)
                .authorizeRequests().anyRequest().authenticated()


    }

}

//https://stackoverflow.com/questions/60280684/how-to-have-different-filters-for-different-request-paths-on-spring-security

/*
@Configuration
class FilterConfiguration {
    @Bean
    fun forUser(): FilterRegistrationBean<JWTAuthenticationFilterUser> {
        val registrationBean: FilterRegistrationBean<JWTAuthenticationFilterUser> = FilterRegistrationBean()
        registrationBean.setFilter(JWTAuthenticationFilterUser())
        registrationBean.urlPatterns("/*")
        //registrationBean.order = ORDERED.HIGHEST_PRECEDENCE
        return registrationBean
    }

    @Bean
    fun forServices(): FilterRegistrationBean<JWTAuthenticationFilterServices> {
        val registrationBean: FilterRegistrationBean<JWTAuthenticationFilterServices> = FilterRegistrationBean()
        registrationBean.setFilter(JWTAuthenticationFilterServices())
        registrationBean.addUrlPattern("/catalog/*")
        //registrationBean.order = ORDERED.HIGHEST_PRECEDENCE
        return registrationBean
    }
}
*/


@Configuration
@Order(1)
class ServiceSecurity : WebSecurityConfigurerAdapter() {
    @Throws(Exception::class)
    override fun configure(httpSecurity: HttpSecurity) {

        http.csrf().csrfTokenRepository(csrfTokenRepository())
                .and()
                .antMatcher("/catalog/**")
                .addFilterBefore(jwtFilterForServices, UsernamePasswordAuthenticationFilter::class.java)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().authenticated()
    }
}

@Configuration
@Order(2)
class GeneralSecurity : WebSecurityConfigurerAdapter() {
    @Throws(Exception::class)
    override fun configure(httpSecurity: HttpSecurity) {
        http.csrf().csrfTokenRepository(csrfTokenRepository())
                .and()
                .antMatcher("/**")
                .addFilterBefore(jwtFilterForUser, UsernamePasswordAuthenticationFilter::class.java)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().authenticated()
    }
}
