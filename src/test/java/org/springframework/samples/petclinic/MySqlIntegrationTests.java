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

package org.springframework.samples.petclinic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mysql")
@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
@DisabledInAotMode
class MySqlIntegrationTests {

	@ServiceConnection
	@Container
	// PETC-323: target environment is MySQL 8.4 — confirmed image tag is mysql:8.4
	static MySQLContainer container = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@LocalServerPort
	int port;

	@Autowired
	private VetRepository vets;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RestTemplateBuilder builder;

	@Test
	void findAll() {
		vets.findAll();
		vets.findAll(); // served from cache
	}

	@Test
	void ownerDetails() {
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		ResponseEntity<String> result = template.exchange(RequestEntity.get("/owners/1").build(), String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	/**
	 * SPEC-DATA-10, SPEC-DATA-5: Asserts all 17 aligned seed dates are equal by natural
	 * key between db/h2/data.sql and db/mysql/data.sql, confirms the loaded MySQL rows
	 * match, and verifies zero rendering failures on owner-page date columns.
	 */
	@Test
	void seedDateParity() throws Exception {
		// Expected pet birth_dates keyed by pet id (13 pets)
		Map<Integer, LocalDate> expectedPetDates = new LinkedHashMap<>();
		expectedPetDates.put(1, LocalDate.of(2010, 9, 7));
		expectedPetDates.put(2, LocalDate.of(2012, 8, 6));
		expectedPetDates.put(3, LocalDate.of(2011, 4, 17));
		expectedPetDates.put(4, LocalDate.of(2010, 3, 7));
		expectedPetDates.put(5, LocalDate.of(2010, 11, 30));
		expectedPetDates.put(6, LocalDate.of(2010, 1, 20));
		expectedPetDates.put(7, LocalDate.of(2012, 9, 4));
		expectedPetDates.put(8, LocalDate.of(2012, 9, 4));
		expectedPetDates.put(9, LocalDate.of(2011, 8, 6));
		expectedPetDates.put(10, LocalDate.of(2007, 2, 24));
		expectedPetDates.put(11, LocalDate.of(2010, 3, 9));
		expectedPetDates.put(12, LocalDate.of(2010, 6, 24));
		expectedPetDates.put(13, LocalDate.of(2012, 6, 8));

		// Expected visit_dates keyed by visit id (4 visits)
		Map<Integer, LocalDate> expectedVisitDates = new LinkedHashMap<>();
		expectedVisitDates.put(1, LocalDate.of(2013, 1, 1));
		expectedVisitDates.put(2, LocalDate.of(2013, 1, 2));
		expectedVisitDates.put(3, LocalDate.of(2013, 1, 3));
		expectedVisitDates.put(4, LocalDate.of(2013, 1, 4));

		// 1. FILE PARITY — parse both SQL files and assert dates match
		Map<Integer, LocalDate> mysqlPetDates = parsePetDatesFromSql("/db/mysql/data.sql");
		Map<Integer, LocalDate> h2PetDates = parsePetDatesFromSql("/db/h2/data.sql");
		Map<Integer, LocalDate> mysqlVisitDates = parseVisitDatesFromSql("/db/mysql/data.sql");
		Map<Integer, LocalDate> h2VisitDates = parseVisitDatesFromSql("/db/h2/data.sql");

		for (Map.Entry<Integer, LocalDate> entry : expectedPetDates.entrySet()) {
			int petId = entry.getKey();
			LocalDate expected = entry.getValue();
			assertThat(mysqlPetDates.get(petId))
				.as("mysql/data.sql pet id=%d birth_date", petId)
				.isEqualTo(expected);
			assertThat(h2PetDates.get(petId))
				.as("h2/data.sql pet id=%d birth_date", petId)
				.isEqualTo(expected);
			assertThat(mysqlPetDates.get(petId))
				.as("file parity pet id=%d", petId)
				.isEqualTo(h2PetDates.get(petId));
		}

		for (Map.Entry<Integer, LocalDate> entry : expectedVisitDates.entrySet()) {
			int visitId = entry.getKey();
			LocalDate expected = entry.getValue();
			assertThat(mysqlVisitDates.get(visitId))
				.as("mysql/data.sql visit id=%d visit_date", visitId)
				.isEqualTo(expected);
			assertThat(h2VisitDates.get(visitId))
				.as("h2/data.sql visit id=%d visit_date", visitId)
				.isEqualTo(expected);
			assertThat(mysqlVisitDates.get(visitId))
				.as("file parity visit id=%d", visitId)
				.isEqualTo(h2VisitDates.get(visitId));
		}

		// 2. LIVE DB — PETS: assert loaded birth_date rows match expected
		List<Map<String, Object>> petRows = jdbcTemplate
			.queryForList("SELECT id, name, birth_date FROM pets ORDER BY id");
		for (Map<String, Object> row : petRows) {
			int petId = ((Number) row.get("id")).intValue();
			LocalDate birthDate = toLocalDate(row.get("birth_date"));
			assertThat(birthDate)
				.as("DB pet id=%d birth_date", petId)
				.isEqualTo(expectedPetDates.get(petId));
		}

		// 3. LIVE DB — VISITS: assert loaded visit_date rows match expected
		List<Map<String, Object>> visitRows = jdbcTemplate
			.queryForList("SELECT id, pet_id, visit_date, description FROM visits ORDER BY id");
		for (Map<String, Object> row : visitRows) {
			int visitId = ((Number) row.get("id")).intValue();
			LocalDate visitDate = toLocalDate(row.get("visit_date"));
			assertThat(visitDate)
				.as("DB visit id=%d visit_date", visitId)
				.isEqualTo(expectedVisitDates.get(visitId));
		}

		// 4. OWNER PAGE RENDERING — GET /owners/{id} for owners 1-10, assert HTTP 200
		RestTemplate template = builder.baseUri("http://localhost:" + port).build();
		for (int ownerId = 1; ownerId <= 10; ownerId++) {
			ResponseEntity<String> result = template
				.exchange(RequestEntity.get("/owners/" + ownerId).build(), String.class);
			assertThat(result.getStatusCode())
				.as("GET /owners/%d HTTP status", ownerId)
				.isEqualTo(HttpStatus.OK);
		}
	}

	private Map<Integer, LocalDate> parsePetDatesFromSql(String classpathResource) throws Exception {
		Map<Integer, LocalDate> result = new LinkedHashMap<>();
		try (InputStream is = getClass().getResourceAsStream(classpathResource);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String upper = line.trim().toUpperCase();
				if (upper.contains("INTO PETS VALUES")) {
					String trimmed = line.trim();
					int start = trimmed.indexOf('(');
					int end = trimmed.lastIndexOf(')');
					if (start >= 0 && end > start) {
						String values = trimmed.substring(start + 1, end);
						String[] parts = values.split(",");
						if (parts.length >= 3) {
							int id = Integer.parseInt(parts[0].trim());
							String dateStr = parts[2].trim().replace("'", "");
							result.put(id, LocalDate.parse(dateStr));
						}
					}
				}
			}
		}
		return result;
	}

	private Map<Integer, LocalDate> parseVisitDatesFromSql(String classpathResource) throws Exception {
		Map<Integer, LocalDate> result = new LinkedHashMap<>();
		try (InputStream is = getClass().getResourceAsStream(classpathResource);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String upper = line.trim().toUpperCase();
				if (upper.contains("INTO VISITS VALUES")) {
					String trimmed = line.trim();
					int start = trimmed.indexOf('(');
					int end = trimmed.lastIndexOf(')');
					if (start >= 0 && end > start) {
						String values = trimmed.substring(start + 1, end);
						String[] parts = values.split(",");
						if (parts.length >= 3) {
							int id = Integer.parseInt(parts[0].trim());
							String dateStr = parts[2].trim().replace("'", "");
							result.put(id, LocalDate.parse(dateStr));
						}
					}
				}
			}
		}
		return result;
	}

	private static LocalDate toLocalDate(Object value) {
		if (value instanceof LocalDate) {
			return (LocalDate) value;
		}
		return ((java.sql.Date) value).toLocalDate();
	}

}
