package it.polito.wa2.g15.lab3

import it.polito.wa2.g15.lab3.security.jwt.AuthEntryPointJwt
import it.polito.wa2.g15.lab3.security.services.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository


@Configuration
@EnableWebSecurity
class SecurityConfiguration : WebSecurityConfigurerAdapter() {
    @Autowired
    lateinit var userDetailsService: UserDetailsServiceImpl

    @Autowired
    private val unauthorizedHandler: AuthEntryPointJwt? = null
    
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder())
    }

    //With this bean controller can access authentication Manager
    @Bean
    @Throws(Exception::class)
    override fun authenticationManagerBean(): AuthenticationManager? {
        return super.authenticationManagerBean()
    }
    
    
    @Bean
    fun csrfTokenRepository(): CsrfTokenRepository {
        //A CsrfTokenRepository that persists the CSRF token in a cookie named "XSRF-TOKEN" and reads from the header
        // "X-XSRF-TOKEN" following the conventions of AngularJS. When using with AngularJS be sure to use
        // withHttpOnlyFalse().
        return CookieCsrfTokenRepository.withHttpOnlyFalse()
    }

    fun generateCsrfHeader(csrfTokenRepository:CsrfTokenRepository): HttpHeaders {
        
        val headers = HttpHeaders()
        val csrfToken: CsrfToken = csrfTokenRepository.generateToken(null)
        
        headers.add(csrfToken.headerName, csrfToken.token)
        headers.add("Cookie", "XSRF-TOKEN=" + csrfToken.token)
        
        return headers
    }
    
    override fun configure(http: HttpSecurity) {
        http.csrf()
            .csrfTokenRepository(csrfTokenRepository())
            .and()
            .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
            .authorizeRequests()
            .anyRequest()
            .permitAll()

/*        http.csrf().disable().authorizeRequests()
                .antMatchers("/forgetPassword").permitAll()
                .antMatchers("/registerUser").permitAll()
                .antMatchers("/login").permitAll()
                .anyRequest().authenticated()
                .and().exceptionHandling().and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and().formLogin().loginPage("/login").loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html").failureUrl("/login?error")
                .and().logout().logoutUrl("/logout")

        http.addFilterBefore(this.jwtFilterRequest, UsernamePasswordAuthenticationFilter::class.java)*/
    }

    override fun configure(web: WebSecurity) {
        super.configure(web)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}