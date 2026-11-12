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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Readiness health indicator that verifies every PostgreSQL identity-column sequence
 * is ahead of the current {@code MAX(id)} in its corresponding table.
 *
 * <p>When any sequence's {@code last_value} is less than {@code MAX(id)} of its table
 * the indicator returns {@link org.springframework.boot.actuate.health.Status#DOWN DOWN}
 * with a {@code lagging_sequences} detail listing each offending sequence, its current
 * value, the table's maximum id, and the numeric delta. Once the sequences are reset
 * the next readiness probe will return
 * {@link org.springframework.boot.actuate.health.Status#UP UP} automatically, without
 * an application restart.
 *
 * <p>This health() method is intentionally uncached (live query on every poll)
 * per SPEC-APP-4, so the endpoint turns UP after a sequence reset without an
 * application restart. Do not add caching.
 *
 * <p>The bean is only registered under the {@code postgres} Spring profile
 * (see {@link Profile @Profile}). Under the default {@code hsqldb} or {@code mysql}
 * profiles no bean is created and the readiness endpoint is unaffected.
 *
 * <p>The feature flag {@code petclinic.guard.sequences} (environment variable
 * {@code PETCLINIC_GUARD_SEQUENCES}) defaults to {@code true}. When set to
 * {@code false} / {@code OFF} the indicator returns
 * {@link org.springframework.boot.actuate.health.Status#UP UP} with
 * {@code guard=disabled} so the bean still participates in the health group
 * without blocking readiness.
 *
 * @author Spring PetClinic contributors
 * @see PetclinicGuardProperties
 */
@Component
@Profile("postgres")
public class StartupDataGuard implements HealthIndicator {

	/**
	 * Pairs of [tableName, sequenceName] for every identity-column table in the
	 * PostgreSQL schema. Constructed from a static, compile-time-known list so that
	 * no dynamic string concatenation from external input is used (NFR-009).
	 */
	private static final String[][] TABLE_SEQUENCE_PAIRS = {
		{ "vets", "vets_id_seq" },
		{ "specialties", "specialties_id_seq" },
		{ "types", "types_id_seq" },
		{ "owners", "owners_id_seq" },
		{ "pets", "pets_id_seq" },
		{ "visits", "visits_id_seq" }
	};

	private static final String PG_SEQUENCES_QUERY =
		"SELECT sequencename, COALESCE(last_value, 0) AS last_value "
		+ "FROM pg_sequences WHERE schemaname = 'public'";

	private final JdbcTemplate jdbcTemplate;

	private final PetclinicGuardProperties guardProperties;

	public StartupDataGuard(JdbcTemplate jdbcTemplate, PetclinicGuardProperties guardProperties) {
		this.jdbcTemplate = jdbcTemplate;
		this.guardProperties = guardProperties;
	}

	/**
	 * Evaluates the readiness of all PostgreSQL identity sequences.
	 *
	 * <p>This health() method is intentionally uncached (live query on every poll)
	 * per SPEC-APP-4, so the endpoint turns UP after a sequence reset without an
	 * application restart. Do not add caching.
	 *
	 * @return {@link Health#up()} when all sequences are safe or the guard is
	 *         disabled; {@link Health#down()} with {@code lagging_sequences} detail
	 *         when any sequence is behind its table's {@code MAX(id)}; or
	 *         {@link Health#down()} with exception detail on any JDBC error.
	 */
	@Override
	@Transactional(readOnly = true)
	public Health health() {
		if (!guardProperties.isSequences()) {
			return Health.up().withDetail("guard", "disabled").build();
		}
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList(PG_SEQUENCES_QUERY);
			Map<String, Long> sequenceValues = new HashMap<>();
			for (Map<String, Object> row : rows) {
				String seqName = (String) row.get("sequencename");
				Number lastVal = (Number) row.get("last_value");
				sequenceValues.put(seqName, lastVal != null ? lastVal.longValue() : 0L);
			}

			List<String> laggingSequences = new ArrayList<>();
			for (String[] pair : TABLE_SEQUENCE_PAIRS) {
				String table = pair[0];
				String seqName = pair[1];
				Long seqLastValue = sequenceValues.get(seqName);
				if (seqLastValue == null) {
					// Sequence not present in pg_sequences — skip
					continue;
				}
				Long maxId = jdbcTemplate.queryForObject(
					"SELECT COALESCE(MAX(id), 0) FROM " + table, Long.class);
				if (maxId == null) {
					maxId = 0L;
				}
				if (seqLastValue < maxId) {
					long delta = seqLastValue - maxId;
					laggingSequences.add(
						seqName + " (last_value=" + seqLastValue
						+ ", max_id=" + maxId
						+ ", delta=" + delta + ")");
				}
			}

			if (!laggingSequences.isEmpty()) {
				return Health.down().withDetail("lagging_sequences", laggingSequences).build();
			}
			return Health.up().build();
		}
		catch (DataAccessException ex) {
			return Health.down().withException(ex).build();
		}
	}

}
