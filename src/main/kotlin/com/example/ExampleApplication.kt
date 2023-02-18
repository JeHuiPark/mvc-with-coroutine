package com.example

import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult
import java.util.concurrent.Callable
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import kotlin.system.measureTimeMillis

@RestController
@SpringBootApplication
class ExampleApplication {

    /**
     * will work in asynchronous mode
     *
     * wrapped by [org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitterReturnValueHandler]
     */
    @GetMapping("/ping1")
    suspend fun ping(): String {
        delay(500)
        return "pong"
    }

    /**
     * will work in asynchronous mode
     *
     * wrapped by [org.springframework.web.servlet.mvc.method.annotation.CallableMethodReturnValueHandler]
     */
    @GetMapping("/ping2")
    fun ping2(): Callable<String> {
        return Callable<String> {
            Thread.sleep(500)
            "pong"
        }
    }

    /**
     * will work in asynchronous mode
     *
     * wrapped by [org.springframework.web.servlet.mvc.method.annotation.DeferredResultMethodReturnValueHandler]
     */
    @GetMapping("/ping3")
    fun ping3(): DeferredResult<String> {
        val deferredResult = DeferredResult<String>()
        Thread.sleep(500)
        deferredResult.setResult("pong")
        return deferredResult
    }

    @GetMapping("/ping4")
    fun ping4(): String {
        Thread.sleep(500)
        return "pong"
    }

    @Order(Int.MIN_VALUE)
    @Component
    class ExampleFilter : Filter {

        private val logger = LoggerFactory.getLogger(ExampleFilter::class.java)

        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            val httpServletRequest = request as HttpServletRequest
            val uri = httpServletRequest.requestURI
            val elapsedTimeMillis = measureTimeMillis { chain.doFilter(httpServletRequest, response) }
            if (httpServletRequest.isAsyncStarted) {
                httpServletRequest.asyncContext.addListener(ExampleAsyncContextListener(uri))
            } else {
                logger.info("$uri elapsed time = $elapsedTimeMillis ms, response is committed ${response.isCommitted}")
            }
        }
    }
}

class ExampleAsyncContextListener(
    private val uri: String
) : AsyncListener {

    private val logger = LoggerFactory.getLogger(ExampleAsyncContextListener::class.java)

    private var startTime: Long = System.currentTimeMillis()

    override fun onComplete(event: AsyncEvent) {
        logger.info("$uri elapsed time = ${System.currentTimeMillis() - startTime} ms")
    }

    override fun onTimeout(event: AsyncEvent?) {
    }

    override fun onError(event: AsyncEvent?) {
    }

    override fun onStartAsync(event: AsyncEvent) {
    }
}

fun main() {
    SpringApplication.run(ExampleApplication::class.java)
}


