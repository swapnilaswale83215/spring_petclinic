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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the PetClinic guard subsystem.
 *
 * <p>Bound from the {@code petclinic.guard.*} namespace. The environment variable
 * {@code PETCLINIC_GUARD_SEQUENCES} maps to {@code petclinic.guard.sequences} via
 * Spring Boot relaxed binding.
 *
 * @author Spring PetClinic contributors
 */
@Component
@ConfigurationProperties(prefix = "petclinic.guard")
@Validated
public class PetclinicGuardProperties {

	/**
	 * Whether the StartupDataGuard sequence-health check is enabled.
	 * Defaults to {@code true} (guard is active by default).
	 * Set {@code PETCLINIC_GUARD_SEQUENCES=false} (or {@code OFF}) to disable.
	 */
	private boolean sequences = true;

	public boolean isSequences() {
		return this.sequences;
	}

	public void setSequences(boolean sequences) {
		this.sequences = sequences;
	}

}
