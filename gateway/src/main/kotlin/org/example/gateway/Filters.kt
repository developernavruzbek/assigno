package org.example.gateway


import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory
import org.springframework.cloud.gateway.route.Route
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.regex.Pattern
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR

@Component
@Configuration
@ConfigurationProperties(prefix = "spring.cloud.gateway")
class RestrictedPathProperties {
    lateinit var restrictedPaths: List<String>
}


@Component
class LoggingGlobalFilter : GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(LoggingGlobalFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        if (exchange.attributes.putIfAbsent(this.javaClass.getIsRoutedKey(), true) != null)
            return chain.filter(exchange)
        val request = exchange.request
        val headers = request.headers.toString()
        val originalUri = exchange.request.uri.path
        val route = exchange.getAttribute<Route>(GATEWAY_ROUTE_ATTR)
        val routeUri = route?.uri
        logger.info("----------Incoming request----------")
        logger.info("ID: ${request.id}")
        logger.info("METHOD: ${request.method}")
        logger.info("ORIGINAL URI: $originalUri")
        logger.info("ROUTE: $route")
        logger.info("ROUTE URI: $routeUri")
        logger.info("HEADERS: $headers")
        return chain.filter(exchange)
    }

    override fun getOrder(): Int = -1
}


@Component
class RestrictedPathFilter(
    restrictedPaths: RestrictedPathProperties
) : GlobalFilter, Ordered {
    private val restrictedPatterns: List<Pattern> = restrictedPaths.restrictedPaths.map { Pattern.compile(it) }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        if (exchange.attributes.putIfAbsent(this.javaClass.getIsRoutedKey(), true) != null)
            return chain.filter(exchange)

        val originalPath = exchange.request.uri.path

        restrictedPatterns.forEach { restrictedPattern ->
            if (restrictedPattern.matcher(originalPath).matches()) {
                exchange.response.statusCode = HttpStatus.FORBIDDEN
                return exchange.response.setComplete()
            }
        }

        return chain.filter(exchange)
    }

    override fun getOrder() = -1
}


@Component
class RequestIdHeaderGlobalFilter : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        var requestId: String? = exchange.attributes[this.javaClass.getIsRoutedKey()] as? String
        var startTime = exchange.getAttribute<Long>(START_TIME_HEADER_KEY)

        if (requestId == null) {
            requestId = java.util.UUID.randomUUID().toString()
            exchange.attributes[this.javaClass.getIsRoutedKey()] = requestId
        }

        if (startTime == null) {
            startTime = System.currentTimeMillis()
            exchange.attributes[START_TIME_HEADER_KEY] = startTime
        }

        val request: ServerHttpRequest = exchange.request
            .mutate()
            .header(REQUEST_ID_HEADER, requestId)
            .header(START_TIME_HEADER_KEY, startTime.toString())
            .build()

        val mutatedExchange: ServerWebExchange = exchange.mutate().request(request).build()

        return chain.filter(mutatedExchange)
    }

    override fun getOrder() = -2

}


@Component
@ConditionalOnBean(RequestIdHeaderGlobalFilter::class)
class RequestBodyRewriteGlobalFilter(
    private val modifyRequestBodyFilter: ModifyRequestBodyGatewayFilterFactory,
    private val requestBodyRewrite: RequestBodyRewrite
) : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        if (exchange.attributes.putIfAbsent(this.javaClass.getIsRoutedKey(), true) != null)
            return chain.filter(exchange)
        val contentType = exchange.request.headers.contentType?.toString() ?: ""

        return if (contentType.contains("application/json", true)) {
            modifyRequestBodyFilter.apply(
                ModifyRequestBodyGatewayFilterFactory.Config()
                    .setRewriteFunction(String::class.java, String::class.java, requestBodyRewrite)
            ).filter(exchange, chain)
        } else {
            exchange.attributes[CACHED_REQUEST_BODY_KEY] = "Content-Type not equals to application/json or absent"
            chain.filter(exchange)
        }
    }

    override fun getOrder() = -1
}
