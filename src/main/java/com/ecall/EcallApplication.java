package com.ecall;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EcallApplication {
    public static void main(String[] args) {
        // .env 파일을 로드하여 시스템 프로퍼티로 설정 (Spring Boot 시작 전)
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );

            System.out.println(".env 파일 로드 완료");
        } catch (Exception e) {
            System.err.println(".env 파일 로드 실패: " + e.getMessage());
        }

        SpringApplication.run(EcallApplication.class, args);
    }
}