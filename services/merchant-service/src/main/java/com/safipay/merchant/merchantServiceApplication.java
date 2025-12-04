package com.safipay.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.safipay.merchant.client")
public class merchantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(merchantServiceApplication.class, args);
    }
}
