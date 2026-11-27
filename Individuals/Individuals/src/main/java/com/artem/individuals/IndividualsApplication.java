
package com.artem.individuals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IndividualsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndividualsApplication.class, args);
    }
}
