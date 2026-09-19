package com.coldchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = {
        "com.coldchain.bootstrap",
        "com.coldchain.shared",
        "com.coldchain.delivery"
})
@ConfigurationPropertiesScan
public class ColdChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColdChainApplication.class, args);
    }

}
