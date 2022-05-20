package it.polito.wa2.g15.lab3
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling // Enable scheduling
@EnableAsync      // Allow the execution of the functions on another thread
// In order to disable the scheduler (i.e. in the test environment)
// set a variable called "scheduler.enable" in application.properties
// (if you want to disable it only in the test the application.properties file must be located inside the test folder)
@ConditionalOnProperty(name = ["scheduler.enabled"], matchIfMissing = true)
class SchedulerConfig
