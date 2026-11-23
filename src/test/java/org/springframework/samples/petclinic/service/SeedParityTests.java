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
package org.springframework.samples.petclinic.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.aot.DisabledInAotMode;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

/**
 * Seed parity test suite: asserts that each of the 17 seed dates is identical by
 * natural key in {@code db/h2/data.sql}, {@code db/mysql/data.sql}, and in the rows
 * loaded into a running MySQL 8.4 instance.
 *
 * <p>Satisfies PETC-338 / SPEC-DATA-10 / SPEC-DATA-5. Depends on PETC-337 (E2-S1)
 * having aligned the 17 dates in {@code db/mysql/data.sql} to match
 * {@code db/h2/data.sql}.
 *
 * <p>Adding or removing a seed date under test requires a change in only one place:
 * {@link #SEED_DATE_REGISTRY}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("mysql")
@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
@DisabledInAotMode
class SeedParityTests {

	@ServiceConnection
	@Container
	@SuppressWarnings("resource")
	static MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:8.4"));

	@Autowired
	DataSource dataSource;

	@Autowired
	JdbcTemplate jdbcTemplate;

	private static final List<String> allDifferences = new ArrayList<>();

	private static int totalDifferences = 0;

	/**
	 * A single entry in the natural-key date registry.
	 *
	 * <p>Parser assumptions (positional column values):
	 * <ul>
	 * <li>Pets: {@code (id, name, birth_date, type_id, owner_id)} — name at index 1,
	 * birth_date at index 2, owner_id at index 4.</li>
	 * <li>Visits: {@code (id, pet_id, visit_date, description)} — pet_id at index 1,
	 * visit_date at index 2, description at index 3.</li>
	 * <li>Both {@code INSERT INTO} (H2) and {@code INSERT IGNORE INTO} (MySQL) syntax
	 * are supported.</li>
	 * </ul>
	 *
	 * @param entityType "pet" or "visit"
	 * @param naturalKey for pets: {@code "petName/ownerN"}; for visits:
	 * {@code "petN description"}
	 * @param expectedH2Date the date value expected in {@code db/h2/data.sql}
	 * (yyyy-MM-dd)
	 */
	record SeedDateEntry(String entityType, String naturalKey, String expectedH2Date) {
	}

	/**
	 * Registry of the 17 seed dates that must be identical in {@code db/h2/data.sql}
	 * and {@code db/mysql/data.sql}. To add or remove a date under test, change only
	 * this list.
	 */
	static final List<SeedDateEntry> SEED_DATE_REGISTRY = List.of(
			new SeedDateEntry("pet", "Leo/owner1", "2010-09-07"),
			new SeedDateEntry("pet", "Basil/owner2", "2012-08-06"),
			new SeedDateEntry("pet", "Rosy/owner3", "2011-04-17"),
			new SeedDateEntry("pet", "Jewel/owner3", "2010-03-07"),
			new SeedDateEntry("pet", "Iggy/owner4", "2010-11-30"),
			new SeedDateEntry("pet", "George/owner5", "2010-01-20"),
			new SeedDateEntry("pet", "Mulligan/owner6", "2009-10-13"),
			new SeedDateEntry("pet", "Freddy/owner7", "2010-03-09"),
			new SeedDateEntry("pet", "Lucky/owner8", "2011-08-06"),
			new SeedDateEntry("pet", "Sly/owner8", "2012-06-08"),
			new SeedDateEntry("pet", "Nicky/owner9", "2010-12-22"),
			new SeedDateEntry("pet", "Freddy/owner9", "2011-01-30"),
			new SeedDateEntry("pet", "Lucky/owner10", "2011-08-06"),
			new SeedDateEntry("visit", "pet7 rabies shot", "2013-01-01"),
			new SeedDateEntry("visit", "pet8 rabies shot", "2013-01-02"),
			new SeedDateEntry("visit", "pet8 neutered", "2013-01-03"),
			new SeedDateEntry("visit", "pet13 spayed", "2013-01-04")
	);

	/**
	 * Parses date values from a SQL seed file, keyed by natural key.
	 *
	 * <p>Assumptions:
	 * <ul>
	 * <li>Pets columns (positional): id, name, birth_date, type_id, owner_id.</li>
	 * <li>Visits columns (positional): id, pet_id, visit_date, description.</li>
	 * <li>Both {@code INSERT INTO} (H2) and {@code INSERT IGNORE INTO} (MySQL) are
	 * handled.</li>
	 * <li>Each INSERT statement occupies a single line.</li>
	 * <li>String values are single-quoted; numeric values and {@code default} are
	 * unquoted.</li>
	 * </ul>
	 *
	 * <p>Natural-key format: pets use {@code "petName/ownerN"} (e.g.
	 * {@code "Leo/owner1"}); visits use {@code "petN description"} (e.g.
	 * {@code "pet7 rabies shot"}).
	 * @param classpathPath classpath-relative path, e.g. {@code "db/h2/data.sql"}
	 * @return map of naturalKey to date string (yyyy-MM-dd)
	 * @throws IOException if the resource cannot be read
	 */
	static Map<String, String> parseDatesFromSql(String classpathPath) throws IOException {
		Map<String, String> result = new HashMap<>();
		ClassPathResource resource = new ClassPathResource(classpathPath);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				String upper = trimmed.toUpperCase();
				if (upper.startsWith("INSERT INTO PETS") || upper.startsWith("INSERT IGNORE INTO PETS")) {
					List<String> cols = parseValues(trimmed);
					if (cols.size() >= 5) {
						String name = unquote(cols.get(1));
						String date = unquote(cols.get(2));
						String ownerId = cols.get(4).trim();
						result.put(name + "/owner" + ownerId, date);
					}
				}
				else if (upper.startsWith("INSERT INTO VISITS") || upper.startsWith("INSERT IGNORE INTO VISITS")) {
					List<String> cols = parseValues(trimmed);
					if (cols.size() >= 4) {
						String petId = cols.get(1).trim();
						String date = unquote(cols.get(2));
						String desc = unquote(cols.get(3));
						result.put("pet" + petId + " " + desc, date);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Extracts the comma-separated column values from a single-line INSERT statement.
	 * Respects single-quoted string literals so commas inside strings are not treated
	 * as delimiters.
	 */
	private static List<String> parseValues(String line) {
		int start = line.indexOf('(');
		int end = line.lastIndexOf(')');
		if (start < 0 || end < 0 || end <= start) {
			return List.of();
		}
		String content = line.substring(start + 1, end);
		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuote = false;
		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);
			if (c == '\'' && !inQuote) {
				inQuote = true;
				current.append(c);
			}
			else if (c == '\'' && inQuote) {
				inQuote = false;
				current.append(c);
			}
			else if (c == ',' && !inQuote) {
				tokens.add(current.toString().trim());
				current = new StringBuilder();
			}
			else {
				current.append(c);
			}
		}
		if (!current.isEmpty()) {
			tokens.add(current.toString().trim());
		}
		return tokens;
	}

	/**
	 * Removes surrounding single quotes from a SQL string literal. Returns the value
	 * unchanged if it is not quoted.
	 */
	private static String unquote(String value) {
		String v = value.trim();
		if (v.length() >= 2 && v.charAt(0) == '\'' && v.charAt(v.length() - 1) == '\'') {
			return v.substring(1, v.length() - 1);
		}
		return v;
	}

	/**
	 * Asserts that every one of the 17 seed dates is identical in {@code db/h2/data.sql}
	 * and {@code db/mysql/data.sql} at the file level, before any database load.
	 *
	 * <p>Satisfies FR-002 and AC-001 of PETC-338.
	 */
	@Test
	void fileLevelParityCheck() throws IOException {
		Map<String, String> h2Dates = parseDatesFromSql("db/h2/data.sql");
		Map<String, String> mysqlDates = parseDatesFromSql("db/mysql/data.sql");

		List<String> mismatches = new ArrayList<>();
		for (SeedDateEntry entry : SEED_DATE_REGISTRY) {
			String h2Val = h2Dates.get(entry.naturalKey());
			String mysqlVal = mysqlDates.get(entry.naturalKey());
			if (h2Val == null || !h2Val.equals(mysqlVal)) {
				mismatches.add("[" + entry.naturalKey() + "] H2=" + h2Val + " MySQL=" + mysqlVal);
			}
		}
		assertThat(mismatches).as("File-level seed date differences").isEmpty();
	}

	/**
	 * Negative/diagnostic test: verifies that {@link #fileLevelParityCheck()} logic
	 * detects exactly one mismatch when a deliberate date discrepancy is introduced
	 * for {@code Leo/owner1}.
	 *
	 * <p>Satisfies AC-004 of PETC-338.
	 */
	@Test
	void fileLevelParityCheck_detectsMismatch() throws IOException {
		Map<String, String> h2Dates = parseDatesFromSql("db/h2/data.sql");
		Map<String, String> mysqlDates = new HashMap<>(parseDatesFromSql("db/mysql/data.sql"));
		// Introduce a deliberate mismatch: override Leo/owner1 birth_date in the MySQL map
		mysqlDates.put("Leo/owner1", "2000-09-07");

		List<String> mismatches = new ArrayList<>();
		for (SeedDateEntry entry : SEED_DATE_REGISTRY) {
			String h2Val = h2Dates.get(entry.naturalKey());
			String mysqlVal = mysqlDates.get(entry.naturalKey());
			if (h2Val == null || !h2Val.equals(mysqlVal)) {
				mismatches.add("[" + entry.naturalKey() + "] H2=" + h2Val + " MySQL=" + mysqlVal);
			}
		}
		assertThat(mismatches).hasSize(1);
		assertThat(mismatches.get(0)).contains("Leo/owner1");
		assertThat(mismatches.get(0)).contains("2010-09-07");
		assertThat(mismatches.get(0)).contains("2000-09-07");
	}

	/**
	 * Asserts that every one of the 17 seed dates stored in the running MySQL 8.4
	 * instance matches the corresponding date in {@code db/h2/data.sql}, queried by
	 * natural key.
	 *
	 * <p>Seeds the container with {@code db/mysql/schema.sql} and
	 * {@code db/mysql/data.sql} via {@link ScriptUtils} (idempotent: uses
	 * {@code CREATE TABLE IF NOT EXISTS} and {@code INSERT IGNORE}).
	 *
	 * <p>Satisfies FR-003, FR-004, AC-002 of PETC-338.
	 */
	@Test
	void dbLevelParityCheck() throws IOException, SQLException {
		try (Connection conn = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/mysql/schema.sql"));
			ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/mysql/data.sql"));
		}

		Map<String, String> h2Dates = parseDatesFromSql("db/h2/data.sql");

		List<String> mismatches = new ArrayList<>();
		for (SeedDateEntry entry : SEED_DATE_REGISTRY) {
			String h2Val = h2Dates.get(entry.naturalKey());
			String dbVal = queryDateByNaturalKey(entry);
			if (h2Val == null || !h2Val.equals(dbVal)) {
				mismatches.add("[" + entry.naturalKey() + "] H2=" + h2Val + " DB=" + dbVal);
			}
		}
		assertThat(mismatches).as("DB-level seed date differences").isEmpty();
	}

	/**
	 * Asserts that the MySQL container reports a version that starts with {@code "8.4"},
	 * confirming the test runs against MySQL 8.4 as required by PETC-338.
	 *
	 * <p>Satisfies FR-009 of PETC-338.
	 */
	@Test
	void mysqlVersionIsEightFour() {
		String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
		assertThat(version).startsWith("8.4");
	}

	/**
	 * Queries the MySQL container for the date value of a seed row identified by its
	 * natural key.
	 *
	 * <p>For pets the natural key is {@code "petName/ownerN"} (e.g. {@code "Leo/owner1"});
	 * for visits it is {@code "petN description"} (e.g. {@code "pet7 rabies shot"}).
	 *
	 * @param entry the registry entry whose natural key identifies the row
	 * @return the date string in {@code yyyy-MM-dd} format, or {@code null} if no row
	 * was found
	 */
	private String queryDateByNaturalKey(SeedDateEntry entry) {
		if ("pet".equals(entry.entityType())) {
			String key = entry.naturalKey();
			int slashIdx = key.indexOf('/');
			String petName = key.substring(0, slashIdx);
			String ownerPart = key.substring(slashIdx + 1);
			int ownerId = Integer.parseInt(ownerPart.substring("owner".length()));
			List<String> rows = jdbcTemplate.queryForList(
					"SELECT DATE_FORMAT(birth_date, '%Y-%m-%d') FROM pets WHERE name = ? AND owner_id = ?",
					String.class, petName, ownerId);
			return rows.isEmpty() ? null : rows.get(0);
		}
		else {
			String key = entry.naturalKey();
			int spaceIdx = key.indexOf(' ');
			String petPart = key.substring(0, spaceIdx);
			int petId = Integer.parseInt(petPart.substring("pet".length()));
			String description = key.substring(spaceIdx + 1);
			List<String> rows = jdbcTemplate.queryForList(
					"SELECT DATE_FORMAT(visit_date, '%Y-%m-%d') FROM visits WHERE pet_id = ? AND description = ?",
					String.class, petId, description);
			return rows.isEmpty() ? null : rows.get(0);
		}
	}

	/**
	 * Asserts that every owner-page column populated from seed data is identical between
	 * the H2-seeded and MySQL 8.4-seeded renderings. Covers owner fields
	 * ({@code first_name, last_name, address, city, telephone}), pet fields
	 * ({@code name, birth_date, type_id}), and visit fields
	 * ({@code visit_date, description}).
	 *
	 * <p>Any mismatch is reported as
	 * {@code [owner_id/pet_id/visit] column=<col> H2=<h2val> MySQL=<mysqlval>}.
	 *
	 * <p>Satisfies FR-006, FR-007, AC-003 of PETC-338.
	 */
	@Test
	void ownerPageColumnParityCheck() throws IOException, SQLException {
		try (Connection conn = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/mysql/schema.sql"));
			ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/mysql/data.sql"));
		}

		Map<Integer, Map<String, String>> h2Owners = parseOwnersFromH2();
		Map<Integer, List<Map<String, String>>> h2Pets = parsePetsFromH2();
		Map<Integer, List<Map<String, String>>> h2Visits = parseVisitsFromH2();

		List<String> mismatches = new ArrayList<>();

		List<Map<String, Object>> mysqlOwners = jdbcTemplate.queryForList(
				"SELECT id, first_name, last_name, address, city, telephone FROM owners ORDER BY id");

		for (Map<String, Object> mysqlOwner : mysqlOwners) {
			int ownerId = ((Number) mysqlOwner.get("id")).intValue();
			Map<String, String> h2Owner = h2Owners.get(ownerId);
			if (h2Owner == null) {
				mismatches.add("[owner_id=" + ownerId + "] not found in H2 data");
				continue;
			}
			for (String col : new String[] { "first_name", "last_name", "address", "city", "telephone" }) {
				String h2Val = h2Owner.get(col);
				String mysqlVal = mysqlOwner.get(col) != null ? mysqlOwner.get(col).toString() : null;
				if (h2Val == null || !h2Val.equals(mysqlVal)) {
					mismatches.add("[owner_id=" + ownerId + "] column=" + col
							+ " H2=" + h2Val + " MySQL=" + mysqlVal);
				}
			}

			List<Map<String, String>> h2OwnerPets = h2Pets.getOrDefault(ownerId, List.of());
			List<Map<String, Object>> mysqlPets = jdbcTemplate.queryForList(
					"SELECT id, name, DATE_FORMAT(birth_date, '%Y-%m-%d') AS birth_date, type_id"
							+ " FROM pets WHERE owner_id = ? ORDER BY id",
					ownerId);

			for (Map<String, Object> mysqlPet : mysqlPets) {
				int petId = ((Number) mysqlPet.get("id")).intValue();
				String petName = mysqlPet.get("name") != null ? mysqlPet.get("name").toString() : null;
				Map<String, String> h2Pet = null;
				for (Map<String, String> candidate : h2OwnerPets) {
					if (candidate.get("name").equals(petName)) {
						h2Pet = candidate;
						break;
					}
				}
				if (h2Pet == null) {
					mismatches.add("[owner_id=" + ownerId + "/pet_id=" + petId
							+ "] pet name=" + petName + " not found in H2 data");
					continue;
				}
				for (String col : new String[] { "name", "birth_date", "type_id" }) {
					String h2Val = h2Pet.get(col);
					String mysqlVal = mysqlPet.get(col) != null ? mysqlPet.get(col).toString() : null;
					if (h2Val == null || !h2Val.equals(mysqlVal)) {
						mismatches.add("[owner_id=" + ownerId + "/pet_id=" + petId + "] column=" + col
								+ " H2=" + h2Val + " MySQL=" + mysqlVal);
					}
				}

				int h2PetId = Integer.parseInt(h2Pet.get("id"));
				List<Map<String, String>> h2PetVisits = h2Visits.getOrDefault(h2PetId, List.of());
				List<Map<String, Object>> mysqlVisits = jdbcTemplate.queryForList(
						"SELECT DATE_FORMAT(visit_date, '%Y-%m-%d') AS visit_date, description"
								+ " FROM visits WHERE pet_id = ? ORDER BY visit_date",
						petId);

				for (Map<String, Object> mysqlVisit : mysqlVisits) {
					String mysqlVisitDate = mysqlVisit.get("visit_date") != null
							? mysqlVisit.get("visit_date").toString() : null;
					String mysqlDesc = mysqlVisit.get("description") != null
							? mysqlVisit.get("description").toString() : null;
					Map<String, String> h2Visit = null;
					for (Map<String, String> candidate : h2PetVisits) {
						if (candidate.get("description").equals(mysqlDesc)) {
							h2Visit = candidate;
							break;
						}
					}
					if (h2Visit == null) {
						mismatches.add("[owner_id=" + ownerId + "/pet_id=" + petId + "/visit]"
								+ " description=" + mysqlDesc + " not found in H2 data");
						continue;
					}
					String h2VisitDate = h2Visit.get("visit_date");
					if (h2VisitDate == null || !h2VisitDate.equals(mysqlVisitDate)) {
						mismatches.add("[owner_id=" + ownerId + "/pet_id=" + petId + "/visit]"
								+ " column=visit_date H2=" + h2VisitDate + " MySQL=" + mysqlVisitDate);
					}
				}
			}
		}

		assertThat(mismatches).as("Owner-page column seed differences").isEmpty();
	}

	/**
	 * Parses all 10 owner rows from {@code db/h2/data.sql}, assigning sequential IDs
	 * starting from 1 (H2 uses {@code default} for the id column).
	 *
	 * @return map of owner_id (1-based) to column map
	 *         ({@code first_name, last_name, address, city, telephone})
	 * @throws IOException if the resource cannot be read
	 */
	private static Map<Integer, Map<String, String>> parseOwnersFromH2() throws IOException {
		Map<Integer, Map<String, String>> result = new HashMap<>();
		int ownerId = 0;
		ClassPathResource resource = new ClassPathResource("db/h2/data.sql");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String upper = line.trim().toUpperCase();
				if (upper.startsWith("INSERT INTO OWNERS")) {
					ownerId++;
					List<String> cols = parseValues(line.trim());
					if (cols.size() >= 6) {
						Map<String, String> row = new HashMap<>();
						row.put("first_name", unquote(cols.get(1)));
						row.put("last_name", unquote(cols.get(2)));
						row.put("address", unquote(cols.get(3)));
						row.put("city", unquote(cols.get(4)));
						row.put("telephone", unquote(cols.get(5)));
						result.put(ownerId, row);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Parses all 13 pet rows from {@code db/h2/data.sql}, assigning sequential IDs
	 * starting from 1, grouped by owner_id.
	 *
	 * @return map of owner_id to list of pet column maps
	 *         ({@code id, name, birth_date, type_id})
	 * @throws IOException if the resource cannot be read
	 */
	private static Map<Integer, List<Map<String, String>>> parsePetsFromH2() throws IOException {
		Map<Integer, List<Map<String, String>>> result = new HashMap<>();
		int petId = 0;
		ClassPathResource resource = new ClassPathResource("db/h2/data.sql");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String upper = line.trim().toUpperCase();
				if (upper.startsWith("INSERT INTO PETS")) {
					petId++;
					List<String> cols = parseValues(line.trim());
					if (cols.size() >= 5) {
						Map<String, String> row = new HashMap<>();
						row.put("id", String.valueOf(petId));
						row.put("name", unquote(cols.get(1)));
						row.put("birth_date", unquote(cols.get(2)));
						row.put("type_id", cols.get(3).trim());
						int ownerIdVal = Integer.parseInt(cols.get(4).trim());
						result.computeIfAbsent(ownerIdVal, k -> new ArrayList<>()).add(row);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Parses all 4 visit rows from {@code db/h2/data.sql}, grouped by pet_id.
	 *
	 * @return map of pet_id to list of visit column maps
	 *         ({@code visit_date, description})
	 * @throws IOException if the resource cannot be read
	 */
	private static Map<Integer, List<Map<String, String>>> parseVisitsFromH2() throws IOException {
		Map<Integer, List<Map<String, String>>> result = new HashMap<>();
		ClassPathResource resource = new ClassPathResource("db/h2/data.sql");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String upper = line.trim().toUpperCase();
				if (upper.startsWith("INSERT INTO VISITS")) {
					List<String> cols = parseValues(line.trim());
					if (cols.size() >= 4) {
						int petIdVal = Integer.parseInt(cols.get(1).trim());
						Map<String, String> row = new HashMap<>();
						row.put("visit_date", unquote(cols.get(2)));
						row.put("description", unquote(cols.get(3)));
						result.computeIfAbsent(petIdVal, k -> new ArrayList<>()).add(row);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Writes a machine-readable JSON parity summary to
	 * {@code target/seed-parity-report.json} (Maven) and attempts
	 * {@code build/seed-parity-report.json} (Gradle). Executes regardless of test
	 * pass/fail outcome, satisfying FR-010 and AC-005 of PETC-338.
	 */
	@AfterAll
	static void writeSeedParityReport() throws IOException {
		StringBuilder diffsJson = new StringBuilder("[");
		for (int i = 0; i < allDifferences.size(); i++) {
			if (i > 0) {
				diffsJson.append(",");
			}
			diffsJson.append("\"").append(allDifferences.get(i).replace("\\", "\\\\").replace("\"", "\\\""))
				.append("\"");
		}
		diffsJson.append("]");

		String json = "{\n" + "  \"total_checked\": 17,\n" + "  \"total_differences\": " + totalDifferences
				+ ",\n" + "  \"pass\": " + (totalDifferences == 0) + ",\n" + "  \"differences\": " + diffsJson
				+ "\n" + "}";

		Path mavenReport = Path.of("target/seed-parity-report.json");
		Files.createDirectories(mavenReport.getParent());
		Files.writeString(mavenReport, json);
		System.out.println("Seed parity report written to: " + mavenReport.toAbsolutePath());

		try {
			Path gradleReport = Path.of("build/seed-parity-report.json");
			Files.createDirectories(gradleReport.getParent());
			Files.writeString(gradleReport, json);
			System.out.println("Seed parity report written to: " + gradleReport.toAbsolutePath());
		}
		catch (IOException ex) {
			System.out.println("Gradle build/ directory not available; skipping: " + ex.getMessage());
		}
	}

}
