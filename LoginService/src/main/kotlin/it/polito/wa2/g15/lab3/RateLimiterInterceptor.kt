package it.polito.wa2.g15.lab3

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class RateLimiterInterceptor: HandlerInterceptorAdapter() {
    @Value("\${ratelimiter.capacity}")
    var CAPACITY: Long = -1
    @Value("\${ratelimiter.refilltime}")
    var REFILL_TIME: Long = -1    //fixed rate at which tokens are added to the bucket
    //val CAPACITY: Long = 10

    val cache: ConcurrentHashMap<String, Bucket> = ConcurrentHashMap()

    fun createBucket(): Bucket {
        //Refill.greedy is better but test are too difficult because bucket is not refilled on elapsed time
        val limit = Bandwidth.classic(CAPACITY, Refill.intervally(CAPACITY, Duration.ofSeconds(REFILL_TIME)))
        val bucket = Bucket4j.builder()
                .addLimit(limit)
                .build()
        return bucket
    }


    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        //return super.preHandle(request, response, handler)
        //API key is a special token that the client needs to provide when making API calls
        //val apiKey = request.getHeader("X-api-key")
        //Scegliamo di usare il csrf token come chiave della mappa che associa a ogni client un diverso bucket
        //Per implementare un rate limiter che agisca limitando il singolo client che invoca una specifica API
        //L'importante è che la key sia univoca per ogni client, e avendo già crsf token, risulta comodo
        val csrfToken = request.getHeader("X-XSRF-TOKEN")
        if(csrfToken == null || csrfToken.isEmpty()) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Missing Header: X-XSRF-TOKEN")
            return false
        }
        val tokenBucket = cache.computeIfAbsent(csrfToken){createBucket()}
        //val tokenBucket = createBucket(CAPACITY)
        val probe = tokenBucket.tryConsumeAndReturnRemaining(1)
        return if (probe.isConsumed) {
            response.addHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            true
        } else {
            val waitForRefill = probe.nanosToWaitForRefill / 1000000000
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", waitForRefill.toString())
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "You have exhausted your API Request Quota")
            false
        }
    }
   /* In order to enhance the client experience of the API, we'll use the following additional response headers to send information about the rate limit:
    X-Rate-Limit-Remaining: number of tokens remaining in the current time window
    X-Rate-Limit-Retry-After-Seconds: remaining time, in seconds, until the bucket is refilled
    We can call ConsumptionProbe methods getRemainingTokens and getNanosToWaitForRefill,
    to get the count of the remaining tokens in the bucket and the time remaining until the next refill, respectively.
    The getNanosToWaitForRefill method returns 0 if we are able to consume the token successfully.*/
}