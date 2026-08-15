package com.rafaelasoares;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Público para poder ser importado pelos testes dos domínios
// (com.rafaelasoares.acesso, etc.), que ficam em outros pacotes.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	// Mesma imagem do docker-compose. Testar contra `postgres:latest` esconderia
	// diferença de comportamento entre a versão de teste e a que roda de verdade.
	@Bean
	@ServiceConnection
	public PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

}
