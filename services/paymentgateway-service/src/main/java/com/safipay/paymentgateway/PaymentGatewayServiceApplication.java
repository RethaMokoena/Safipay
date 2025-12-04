package com.safipay.paymentgateway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication
@EnableFeignClients
public class PaymentGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayServiceApplication.class, args);
    }
}
