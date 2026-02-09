package org.example.organization

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.Locale
import kotlin.apply

@Configuration
class WebMvcConfig : WebMvcConfigurer {
    @Bean
    fun localeResolver() = SessionLocaleResolver().apply { setDefaultLocale(Locale("ru")) }

    @Bean
    fun errorMessageSource() = ResourceBundleMessageSource().apply {
        setDefaultEncoding(Charsets.UTF_8.name())
        setBasename("error")
    }
}


@Configuration
class MyConfiguration {
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()
}