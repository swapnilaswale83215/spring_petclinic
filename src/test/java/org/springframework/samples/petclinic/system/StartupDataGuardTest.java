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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Unit tests for {@link StartupDataGuard}.
 *
 * <p>Uses Mockito to mock {@link JdbcTemplate}; no Docker or real database is required.
 *
 * @author Spring PetClinic contributors
 */
@ExtendWith(MockitoExtension.class)
@DisabledInNativeImage
class StartupDataGuardTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	private PetclinicGuardProperties guardProperties;

	private StartupDataGuard startupDataGuard;

	@BeforeEach
	void setUp() {
		guardProperties = new PetclinicGuardProperties();
		startupDataGuard = new StartupDataGuard(jdbcTemplate, guardProperties);
	}

	/**
	 * TC1: When all sequences have last_value >= MAX(id), health() returns UP
	 * and the detail map does not contain 'lagging_sequences'.
	 */
	@Test
	void allSequencesHealthy_returnsUp() {
		given(jdbcTemplate.queryForList(anyString())).willReturn(buildSequenceRows(100L));
		given(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).willReturn(50L);

		Health health = startupDataGuard.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().containsKey("lagging_sequences")).isFalse();
	}

	/**
	 * TC2: When the 'owners' sequence last_value is below MAX(id), health() returns DOWN
	 * and the 'lagging_sequences' detail contains an entry mentioning "owners".
	 */
	@Test
	void oneSequenceLagging_returnsDown() {
		List<Map<String, Object>> rows = buildSequenceRows(100L);
		for (Map<String, Object> row : rows) {
			if ("owners_id_seq".equals(row.get("sequencename"))) {
				row.put("last_value", 5L);
			}
		}
		given(jdbcTemplate.queryForList(anyString())).willReturn(rows);
		given(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).willReturn(1L);
		given(jdbcTemplate.queryForObject(eq("SELECT COALESCE(MAX(id), 0) FROM owners"),
				eq(Long.class))).willReturn(10L);

		Health health = startupDataGuard.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails().get("lagging_sequences").toString()).contains("owners");
	}

	/**
	 * TC3: When sequences=false, health() returns UP with guard=disabled
	 * and JdbcTemplate is never invoked.
	 */
	@Test
	void guardDisabled_returnsUpWithoutQuery() {
		PetclinicGuardProperties disabledProps = new PetclinicGuardProperties();
		disabledProps.setSequences(false);
		StartupDataGuard disabledGuard = new StartupDataGuard(jdbcTemplate, disabledProps);

		Health health = disabledGuard.health();

		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails().get("guard")).isEqualTo("disabled");
		verify(jdbcTemplate, never()).queryForList(anyString());
	}

	/**
	 * TC4: A freshly instantiated PetclinicGuardProperties defaults sequences to true.
	 */
	@Test
	void propertyBinding_defaultsToTrue() {
		PetclinicGuardProperties props = new PetclinicGuardProperties();
		assertThat(props.isSequences()).isTrue();
	}

	/**
	 * Builds a list of pg_sequences rows for all six application-managed sequences,
	 * each with the supplied {@code lastValue}.
	 */
	private List<Map<String, Object>> buildSequenceRows(long lastValue) {
		String[] sequenceNames = {
			"vets_id_seq", "specialties_id_seq", "types_id_seq",
			"owners_id_seq", "pets_id_seq", "visits_id_seq"
		};
		List<Map<String, Object>> rows = new ArrayList<>();
		for (String seqName : sequenceNames) {
			Map<String, Object> row = new HashMap<>();
			row.put("sequencename", seqName);
			row.put("last_value", lastValue);
			rows.add(row);
		}
		return rows;
	}

}
