package com.example.Auto_Grade;

import com.example.Auto_Grade.config.MailgunProperties;
import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.enums.Role;
import com.example.Auto_Grade.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties(MailgunProperties.class)
public class AutoGradeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoGradeApplication.class, args);
	}

	@Bean
	CommandLineRunner init(
			AccountRepository repository,
			PasswordEncoder encoder,
			@Value("${admin.email}") String email,
			@Value("${admin.username}") String username,
			@Value("${admin.password}") String password
	) {

		return args -> {

			if(repository.findByEmail(email).isEmpty()) {

				Account admin = Account.builder()
						.email(email)
						.username(username)
						.password(encoder.encode(password))
						.role(Role.ADMIN)
						.locked(false)
						.build();

				repository.save(admin);
			}
		};
	}
}