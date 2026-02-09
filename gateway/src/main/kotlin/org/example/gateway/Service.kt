package org.example.gateway



import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class RequestBodyRewrite : RewriteFunction<String?, String> {
    override fun apply(exchange: ServerWebExchange, body: String?): Publisher<String> {
        exchange.attributes[CACHED_URL_KEY] = exchange.request.uri
        exchange.attributes[CACHED_REQUEST_BODY_KEY] = body ?: return Mono.empty()
        return body.run { Mono.just(this) }
    }
}
