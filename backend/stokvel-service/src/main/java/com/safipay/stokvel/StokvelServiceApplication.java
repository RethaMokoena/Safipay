package com.safipay.stokvel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication @EnableFeignClients
public class StokvelServiceApplication {
    public static void main(String[] args) { SpringApplication.run(StokvelServiceApplication.class, args); }
}
