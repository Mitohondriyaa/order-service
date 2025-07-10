package io.github.mitohondriyaa.order.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor authInterceptor() {
        return requestTemplate -> {
            RequestAttributes ra = RequestContextHolder.getRequestAttributes();

            if (ra instanceof ServletRequestAttributes requestAttributes) {
                HttpServletRequest request = requestAttributes.getRequest();
                String authorization = request.getHeader("Authorization");

                if (authorization != null) {
                    requestTemplate.header("Authorization", authorization);
                }
            }
        };
    }
}