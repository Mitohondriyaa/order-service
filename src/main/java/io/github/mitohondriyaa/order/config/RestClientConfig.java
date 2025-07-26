package io.github.mitohondriyaa.order.config;

import io.github.mitohondriyaa.order.client.InventoryClient;
import io.github.mitohondriyaa.order.client.ProductClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;


@Configuration
public class RestClientConfig {
    @Value("${inventory.url}")
    private String inventoryUrl;
    @Value("${product.url}")
    private String productUrl;

    @Bean
    public ClientHttpRequestInterceptor authHeaderInterceptor() {
        return (request, body, execution) -> {
            RequestAttributes ra =  RequestContextHolder.getRequestAttributes();

            if (ra instanceof ServletRequestAttributes requestAttributes) {
                HttpServletRequest servletRequest = requestAttributes.getRequest();
                String authHeader = servletRequest.getHeader("Authorization");

                if (authHeader != null) {
                    request.getHeaders().add("Authorization", authHeader);
                }
            }

            return execution.execute(request, body);
        };
    }

    @Bean
    public InventoryClient inventoryClient(ClientHttpRequestInterceptor authHeaderInterceptor) {
        RestClient restClient = RestClient.builder()
            .baseUrl(inventoryUrl)
            .requestInterceptor(authHeaderInterceptor)
            .requestFactory(requestFactory())
            .build();
        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(restClientAdapter).build();
        return httpServiceProxyFactory.createClient(InventoryClient.class);
    }

    @Bean
    public ProductClient productClient(ClientHttpRequestInterceptor authHeaderInterceptor) {
        RestClient restClient = RestClient.builder()
            .baseUrl(productUrl)
            .requestInterceptor(authHeaderInterceptor)
            .requestFactory(requestFactory())
            .build();
        RestClientAdapter restClientAdapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(restClientAdapter).build();
        return httpServiceProxyFactory.createClient(ProductClient.class);
    }

    private ClientHttpRequestFactory requestFactory() {
        ClientHttpRequestFactorySettings clientHttpRequestFactorySettings
            = ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(3))
            .withReadTimeout(Duration.ofSeconds(3));

        return ClientHttpRequestFactoryBuilder.jdk().build(clientHttpRequestFactorySettings);
    }
}