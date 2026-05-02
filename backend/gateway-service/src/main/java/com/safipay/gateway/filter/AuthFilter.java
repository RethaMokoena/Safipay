// package com.safipay.gateway.filter;

// import com.safipay.gateway.security.JwtUtil;
// import lombok.RequiredArgsConstructor;
// import org.springframework.cloud.gateway.filter.GatewayFilter;
// import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
// import org.springframework.http.*;
// import org.springframework.stereotype.Component;
// import org.springframework.util.StringUtils;

// @Component
// @RequiredArgsConstructor
// public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {
//     private final JwtUtil jwtUtil;

//     public AuthFilter() { super(Config.class); this.jwtUtil = null; }

//     @Override
//     public GatewayFilter apply(Config config) {
//         return (exchange, chain) -> {
//             String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//             if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
//                 exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                 return exchange.getResponse().setComplete();
//             }
//             String token = header.substring(7);
//             if (!jwtUtil.validate(token)) {
//                 exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//                 return exchange.getResponse().setComplete();
//             }
//             var mutated = exchange.getRequest().mutate().header("X-User-Id", jwtUtil.extractUserId(token)).build();
//             return chain.filter(exchange.mutate().request(mutated).build());
//         };
//     }
//     public static class Config {}
// }




package com.safipay.gateway.filter;

import com.safipay.gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final JwtUtil jwtUtil;

    public AuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            String header = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = header.substring(7);

            if (!jwtUtil.validate(token)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            var mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", jwtUtil.extractUserId(token))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {}
}