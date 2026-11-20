/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.DockerClientFactory;

/**
 * Docker-backed integration tests for {@link StartupDataGuard}.
 *
 * <p>Mirrors the structure of {@code PostgresIntegrationTests}: same
 * {@code @ActiveProfiles("postgres")}, same Docker Compose lifecycle annotations,
 * and the same {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} pattern.
 *
 * <p>Test scenarios (TC5–TC8):
 * <ul>
 *   <li>TC5 — {@code /actuator/health/readiness} returns 200/UP when all sequences are healthy.</li>
 *   <li>TC6 — Returns 503/DOWN with {@code lagging_sequences} containing "owners" after
 *       {@code setval('owners_id_seq', 1, false)}.</li>
 *   <li>TC7 — Returns 200/UP after the sequence is reset — same process, no restart.</li>
 *   <li>TC8 — Returns 200/UP with {@code guard=disabled} detail when
 *       {@code petclinic.guard.sequences=false}.</li>
 * </ul>
 *
 * @author Spring PetClinic contributors
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"spring.docker.compose.skip.in-tests=false",
		"spring.docker.compose.start.arguments=--force-recreate,--renew-anon-volumes,postgres"
})
@ActiveProfiles("postgres")
@DisabledInNativeImage
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StartupDataGuardIntegrationTest {

	@LocalServerPort
	int port;

	@Autowired
	private RestTemplateBuilder builder;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeAll
	static void available() {
		assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
	}

	/**
	 * TC5: GET /actuator/health/readiness returns HTTP 200 and the body contains
	 * {@code startupDataGuard} with status {@code UP} when all sequences are healthy.
	 *
	 * <p>Also verifies that the runtime database role has {@code SELECT} on
	 * {@code pg_sequences} (R1 permission check). If the role lacks this privilege the
	 * test fails immediately with a descriptive message.
	 */
	@Test
	@Order(1)
	void readyzUp_whenAllSequencesHealthy() {
		try {
			jdbcTemplate.queryForList(
					"SELECT sequencename FROM pg_sequences WHERE schemaname = 'public'");
		}
		catch (DataAccessException ex) {
			fail("Runtime user lacks SELECT on pg_sequences — R1 permission not granted: "
					+ ex.getMessage());
		}

		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template
				.exchange(RequestEntity.get("/actuator/health/readiness").build(), String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("startupDataGuard");
		assertThat(result.getBody()).contains("\"UP\"");
	}

	/**
	 * TC6: After {@code setval('owners_id_seq', 1, false)} the sequence's
	 * {@code last_value} falls below {@code MAX(id)} in the owners table.
	 * GET /actuator/health/readiness must return HTTP 503 with status {@code DOWN}
	 * and a {@code lagging_sequences} detail that mentions "owners".
	 */
	@Test
	@Order(2)
	void readyzDown_whenSequenceLagging() {
		// Drive owners_id_seq below MAX(id) — SELECT only, no NEXTVAL consumed
		jdbcTemplate.queryForObject("SELECT setval('owners_id_seq', 1, false)", Long.class);

		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		try {
			template.exchange(
					RequestEntity.get("/actuator/health/readiness").build(), String.class);
			fail("Expected HTTP 503 but the endpoint returned 200 — "
					+ "startupDataGuard should be DOWN when owners_id_seq < MAX(id)");
		}
		catch (HttpServerErrorException ex) {
			assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
			assertThat(ex.getResponseBodyAsString()).contains("DOWN");
			assertThat(ex.getResponseBodyAsString()).contains("owners");
		}
	}

	/**
	 * TC7: After resetting {@code owners_id_seq} to {@code MAX(id)}, the same running
	 * application process must report HTTP 200 / {@code UP} on the next readiness probe
	 * — no restart required.
	 */
	@Test
	@Order(3)
	void readyzUp_afterSequenceReset_noRestart() {
		// Reset owners_id_seq to MAX(id) — same process, no restart
		jdbcTemplate.queryForObject(
				"SELECT setval('owners_id_seq', (SELECT MAX(id) FROM owners))", Long.class);

		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template
				.exchange(RequestEntity.get("/actuator/health/readiness").build(), String.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody()).contains("\"UP\"");
	}

	/**
	 * TC8: When {@code petclinic.guard.sequences=false} the readiness endpoint returns
	 * HTTP 200 and the {@code startupDataGuard} detail contains {@code "disabled"},
	 * regardless of sequence state.
	 *
	 * <p>Uses a separate nested {@link SpringBootTest} context with the flag overridden
	 * so the outer context's sequence state does not interfere.
	 */
	@Nested
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
			"spring.docker.compose.skip.in-tests=false",
			"spring.docker.compose.start.arguments=postgres",
			"petclinic.guard.sequences=false"
	})
	@ActiveProfiles("postgres")
	@TestInstance(Lifecycle.PER_CLASS)
	class WhenGuardDisabled {

		@LocalServerPort
		int nestedPort;

		@Autowired
		private RestTemplateBuilder nestedBuilder;

		@BeforeAll
		void available() {
			assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
		}

		/**
		 * GET /actuator/health/readiness returns HTTP 200 and the response body
		 * contains {@code "disabled"} in the {@code startupDataGuard} detail.
		 */
		@Test
		void readyzUp_whenGuardDisabled() {
			RestTemplate template = nestedBuilder.baseUri("http://localhost:" + nestedPort).build();
			ResponseEntity<String> result = template
					.exchange(RequestEntity.get("/actuator/health/readiness").build(), String.class);

			assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(result.getBody()).contains("disabled");
		}

	}

}
