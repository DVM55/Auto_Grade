package com.example.Auto_Grade;

import com.example.Auto_Grade.entity.Account;
import com.example.Auto_Grade.enums.Role;
import com.example.Auto_Grade.repository.AccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableAsync
@SpringBootApplication
public class AutoGradeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoGradeApplication.class, args);
	}

	@Bean
	CommandLineRunner init(AccountRepository repository, PasswordEncoder encoder) {
		return args -> {
			if(repository.findByEmail("hifpow002@gmail.com").isEmpty()) {
				Account admin = Account.builder()
						.email("hifpow002@gmail.com")
						.username("admin")
						.password(encoder.encode("123456"))
						.role(Role.ADMIN)
						.locked(false)
						.build();

				repository.save(admin);
			}
		};
	}

}
