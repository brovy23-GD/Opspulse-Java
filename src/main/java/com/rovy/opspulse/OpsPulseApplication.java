package com.rovy.opspulse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class OpsPulseApplication {
    public static void main(String[] args) { SpringApplication.run(OpsPulseApplication.class, args); }
}