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
package org.springframework.samples.petclinic.owner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * PETC-305 proof-of-functionality integration test.
 *
 * <p>Verifies that the Spring Data JPA SpEL helpers {@code escape()} and
 * {@code escapeCharacter()} are available and work correctly in {@code @Query}
 * annotations within this project's runtime (Spring Boot 4.1.0 / Spring Data JPA
 * 4.1.x, well above the 2.5.0 introduction threshold).
 *
 * <p>Evidence for the decision recorded in
 * {@code docs/decisions/PETC-305-spring-data-jpa-escape-check.md}.
 *
 * @see OwnerRepository#findByLastNameStartingWithSpelProof
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class SpelEscapeProofTest {

	@Autowired
	private OwnerRepository owners;

	private final Pageable pageable = Pageable.unpaged();

	/**
	 * A plain prefix returns the expected owners — SpEL expression evaluates correctly.
	 */
	@Test
	void spelEscapeHelperReturnsMatchingOwners() {
		Page<Owner> results = this.owners.findByLastNameStartingWithSpelProof("Davis", pageable);
		assertThat(results).hasSize(2);
	}

	/**
	 * A prefix containing {@code %} is treated as a literal character, not a wildcard.
	 */
	@Test
	void spelEscapeHelperTreatsPercentAsLiteral() {
		Page<Owner> results = this.owners.findByLastNameStartingWithSpelProof("D%", pageable);
		assertThat(results).isEmpty();
	}

	/**
	 * A prefix containing {@code _} is treated as a literal character, not a wildcard.
	 */
	@Test
	void spelEscapeHelperTreatsUnderscoreAsLiteral() {
		Page<Owner> results = this.owners.findByLastNameStartingWithSpelProof("D_vis", pageable);
		assertThat(results).isEmpty();
	}

}
