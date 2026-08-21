package com.example.gateway.config;

import com.example.gateway.filter.RateLimitFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {

    @Bean
    public RouteLocator routes(
            RouteLocatorBuilder builder,
            RateLimitFilter rateLimitFilter) {

        return builder.routes()
                .route("customer-service", route -> route
                        .path("/api/customers/**")
                        .filters(filters -> filters
                                .stripPrefix(1)
                                .filter(rateLimitFilter))
                        .uri("http://localhost:8081"))
                .build();
    }
}
