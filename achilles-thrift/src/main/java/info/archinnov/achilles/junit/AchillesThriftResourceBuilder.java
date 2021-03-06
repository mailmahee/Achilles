/**
 *
 * Copyright (C) 2012-2013 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.junit;

import info.archinnov.achilles.junit.AchillesTestResource.Steps;

public class AchillesThriftResourceBuilder {

	private Steps cleanupSteps = Steps.BOTH;
	private String[] tablesToCleanUp;
	private String entityPackages;

	private AchillesThriftResourceBuilder() {
	}

	private AchillesThriftResourceBuilder(String entityPackages) {
		this.entityPackages = entityPackages;
	}

	/**
	 * Start building an AchillesThriftResource with entity packages
	 * 
	 * @param entityPackages
	 *            packages to scan for entity discovery, comma separated
	 */
	public static AchillesThriftResourceBuilder withEntityPackages(String entityPackages) {
		return new AchillesThriftResourceBuilder(entityPackages);
	}

	/**
	 * Tables to be truncated during unit tests
	 * 
	 * @param tables
	 *            list of tables to truncate before and/or after tests
	 */
	public AchillesThriftResourceBuilder tablesToTruncate(String... tablesToCleanUp) {
		this.tablesToCleanUp = tablesToCleanUp;
		return this;
	}

	/**
	 * Truncate tables BEFORE each test
	 */
	public AchillesThriftResourceBuilder truncateBeforeTest() {
		this.cleanupSteps = Steps.BEFORE_TEST;
		return this;
	}

	/**
	 * Truncate tables AFTER each test
	 */
	public AchillesThriftResourceBuilder truncateAfterTest() {
		this.cleanupSteps = Steps.AFTER_TEST;
		return this;
	}

	/**
	 * Truncate tables BEFORE and AFTER each test
	 */
	public AchillesThriftResourceBuilder truncateBeforeAndAfterTest() {
		this.cleanupSteps = Steps.BOTH;
		return this;
	}

	public AchillesThriftResource build() {
		return new AchillesThriftResource(entityPackages, cleanupSteps, tablesToCleanUp);
	}
}
