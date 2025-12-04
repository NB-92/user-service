package com.prpo.chat.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.prpo.chat")
@EnableMongoRepositories(basePackages = "com.prpo.chat.service.repository")
public class App {

  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
