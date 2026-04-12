package com.example.Web_IDE_Project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class WebIdeProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebIdeProjectApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		System.out.println("====================================================");
		System.out.println("🚀 Web IDE Backend Server Started Successfully!");
		System.out.println("📝 Swagger UI: http://localhost:8080/swagger-ui/index.html");
		System.out.println("====================================================");
	}
}