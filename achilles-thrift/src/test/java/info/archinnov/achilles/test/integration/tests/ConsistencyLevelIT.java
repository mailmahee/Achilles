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
package info.archinnov.achilles.test.integration.tests;

import static info.archinnov.achilles.type.ConsistencyLevel.*;
import static org.fest.assertions.api.Assertions.*;
import info.archinnov.achilles.consistency.ThriftConsistencyLevelPolicy;
import info.archinnov.achilles.embedded.ThriftEmbeddedServer;
import info.archinnov.achilles.entity.manager.ThriftBatchingPersistenceManager;
import info.archinnov.achilles.entity.manager.ThriftPersistenceManager;
import info.archinnov.achilles.entity.manager.ThriftPersistenceManagerFactory;
import info.archinnov.achilles.exception.AchillesException;
import info.archinnov.achilles.junit.AchillesTestResource.Steps;
import info.archinnov.achilles.test.builders.TweetTestBuilder;
import info.archinnov.achilles.test.integration.AchillesInternalThriftResource;
import info.archinnov.achilles.test.integration.entity.CompleteBean;
import info.archinnov.achilles.test.integration.entity.CompleteBeanTestBuilder;
import info.archinnov.achilles.test.integration.entity.EntityWithLocalQuorumConsistency;
import info.archinnov.achilles.test.integration.entity.EntityWithWriteOneAndReadLocalQuorumConsistency;
import info.archinnov.achilles.test.integration.entity.Tweet;
import info.archinnov.achilles.test.integration.utils.CassandraLogAsserter;
import info.archinnov.achilles.type.ConsistencyLevel;
import info.archinnov.achilles.type.OptionsBuilder;
import me.prettyprint.hector.api.exceptions.HInvalidRequestException;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConsistencyLevelIT {

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Rule
	public AchillesInternalThriftResource resource = new AchillesInternalThriftResource(Steps.AFTER_TEST,
			"CompleteBean", "Tweet", EntityWithLocalQuorumConsistency.TABLE_NAME,
			EntityWithWriteOneAndReadLocalQuorumConsistency.TABLE_NAME);

	private ThriftPersistenceManagerFactory pmf = resource.getPersistenceManagerFactory();

	private ThriftPersistenceManager manager = resource.getPersistenceManager();

	private ThriftConsistencyLevelPolicy policy = resource.getConsistencyPolicy();

	private CassandraLogAsserter logAsserter = new CassandraLogAsserter();

	private Long id = RandomUtils.nextLong();

	@Test
	public void should_throw_exception_when_persisting_with_local_quorum_consistency() throws Exception {
		EntityWithLocalQuorumConsistency bean = new EntityWithLocalQuorumConsistency();
		bean.setId(id);
		bean.setName("name");

		expectedEx.expect(HInvalidRequestException.class);
		expectedEx
				.expectMessage("InvalidRequestException(why:consistency level LOCAL_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");

		manager.persist(bean);
		assertThatConsistencyLevelsAreReinitialized();
	}

	@Test
	public void should_throw_exception_when_loading_entity_with_local_quorum_consistency() throws Exception {
		EntityWithWriteOneAndReadLocalQuorumConsistency bean = new EntityWithWriteOneAndReadLocalQuorumConsistency(id,
				"FN", "LN");

		manager.persist(bean);

		expectedEx.expect(AchillesException.class);
		expectedEx
				.expectMessage("Error when loading entity type '"
						+ EntityWithWriteOneAndReadLocalQuorumConsistency.class.getCanonicalName()
						+ "' with key '"
						+ id
						+ "'. Cause : InvalidRequestException(why:consistency level LOCAL_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy)");

		manager.find(EntityWithWriteOneAndReadLocalQuorumConsistency.class, id);
		assertThatConsistencyLevelsAreReinitialized();
	}

	@Test
	public void should_recover_from_exception_and_reinit_consistency_level() throws Exception {
		EntityWithWriteOneAndReadLocalQuorumConsistency bean = new EntityWithWriteOneAndReadLocalQuorumConsistency(id,
				"FN", "LN");

		try {
			manager.persist(bean);
			manager.find(EntityWithWriteOneAndReadLocalQuorumConsistency.class, id);
		} catch (AchillesException e) {
			// Should reinit consistency level to default
		}
		CompleteBean newBean = new CompleteBean();
		newBean.setId(id);
		newBean.setName("name");

		manager.persist(newBean);

		newBean = manager.find(CompleteBean.class, newBean.getId());

		assertThat(newBean).isNotNull();
		assertThat(newBean.getName()).isEqualTo("name");
		assertThatConsistencyLevelsAreReinitialized();
	}

	@Test
	public void should_persist_with_runtime_consistency_level_overriding_predefined_one() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name zerferg").buid();

		try {
			manager.persist(entity, OptionsBuilder.withConsistency(EACH_QUORUM));
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}

		assertThatConsistencyLevelsAreReinitialized();

		logAsserter.prepareLogLevel();
		manager.persist(entity, OptionsBuilder.withConsistency(ALL));
		CompleteBean found = manager.find(CompleteBean.class, entity.getId());
		assertThat(found.getName()).isEqualTo("name zerferg");
		logAsserter.assertConsistencyLevels(ConsistencyLevel.ONE, ConsistencyLevel.ALL);
	}

	@Test
	public void should_merge_with_runtime_consistency_level_overriding_predefined_one() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name zeruioze").buid();

		try {
			manager.merge(entity, OptionsBuilder.withConsistency(EACH_QUORUM));
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}
		assertThatConsistencyLevelsAreReinitialized();

		logAsserter.prepareLogLevel();
		manager.merge(entity, OptionsBuilder.withConsistency(ALL));
		CompleteBean found = manager.find(CompleteBean.class, entity.getId());
		assertThat(found.getName()).isEqualTo("name zeruioze");
		logAsserter.assertConsistencyLevels(ConsistencyLevel.ONE, ConsistencyLevel.ALL);
	}

	@Test
	public void should_find_with_runtime_consistency_level_overriding_predefined_one() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name rtprt").buid();
		manager.persist(entity);

		try {
			manager.find(CompleteBean.class, entity.getId(), ConsistencyLevel.EACH_QUORUM);
		} catch (AchillesException e) {
			assertThat(e)
					.hasMessage(
							"Error when loading entity type '"
									+ CompleteBean.class.getCanonicalName()
									+ "' with key '"
									+ entity.getId()
									+ "'. Cause : InvalidRequestException(why:EACH_QUORUM ConsistencyLevel is only supported for writes)");
		}
		assertThatConsistencyLevelsAreReinitialized();
		logAsserter.prepareLogLevel();
		CompleteBean found = manager.find(CompleteBean.class, entity.getId(), ConsistencyLevel.ALL);
		assertThat(found.getName()).isEqualTo("name rtprt");
		logAsserter.assertConsistencyLevels(ConsistencyLevel.ALL, ConsistencyLevel.QUORUM);
	}

	@Test
	public void should_refresh_with_runtime_consistency_level_overriding_predefined_one() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		entity = manager.merge(entity);

		try {
			manager.refresh(entity, ConsistencyLevel.EACH_QUORUM);
		} catch (AchillesException e) {
			assertThat(e)
					.hasMessage(
							"Error when loading entity type '"
									+ CompleteBean.class.getCanonicalName()
									+ "' with key '"
									+ entity.getId()
									+ "'. Cause : InvalidRequestException(why:EACH_QUORUM ConsistencyLevel is only supported for writes)");
		}
		assertThatConsistencyLevelsAreReinitialized();
		logAsserter.prepareLogLevel();
		manager.refresh(entity, ConsistencyLevel.ALL);
		logAsserter.assertConsistencyLevels(ConsistencyLevel.ALL, ConsistencyLevel.QUORUM);
	}

	@Test
	public void should_remove_with_runtime_consistency_level_overriding_predefined_one() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		entity = manager.merge(entity);

		try {
			manager.remove(entity, ConsistencyLevel.EACH_QUORUM);
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}
		assertThatConsistencyLevelsAreReinitialized();
		logAsserter.prepareLogLevel();
		manager.remove(entity, ConsistencyLevel.ALL);
		assertThat(manager.find(CompleteBean.class, entity.getId())).isNull();
		logAsserter.assertConsistencyLevels(ConsistencyLevel.ONE, ConsistencyLevel.ALL);
	}

	@Test
	public void should_reinit_consistency_level_after_exception() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name qzerferf").buid();
		try {
			manager.merge(entity, OptionsBuilder.withConsistency(EACH_QUORUM));
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}
		assertThatConsistencyLevelsAreReinitialized();
		logAsserter.prepareLogLevel();
		manager.merge(entity, OptionsBuilder.withConsistency(ALL));
		CompleteBean found = manager.find(CompleteBean.class, entity.getId());
		assertThat(found.getName()).isEqualTo("name qzerferf");
		logAsserter.assertConsistencyLevels(ConsistencyLevel.ONE, ConsistencyLevel.ALL);
	}

	@Test
	public void should_batch_with_runtime_consistency_level() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		Tweet tweet = TweetTestBuilder.tweet().randomId().content("test_tweet").buid();

		logAsserter.prepareLogLevel();
		ThriftBatchingPersistenceManager batchingManager = pmf.createBatchingPersistenceManager();
		batchingManager.startBatch(ONE);
		batchingManager.persist(entity);
		batchingManager.persist(tweet);

		batchingManager.endBatch();
		logAsserter.assertConsistencyLevels(ONE, ONE);
	}

	@Test
	public void should_get_counter_with_consistency_level() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		entity = manager.merge(entity);
		try {
			entity.getVersion().get(ConsistencyLevel.EACH_QUORUM);
		} catch (HInvalidRequestException e) {
			assertThat(e).hasMessage(
					"InvalidRequestException(why:EACH_QUORUM ConsistencyLevel is only supported for writes)");
		}
		assertThatConsistencyLevelsAreReinitialized();
	}

	@Test
	public void should_increment_counter_with_consistency_level() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		entity = manager.merge(entity);
		try {
			entity.getVersion().incr(ConsistencyLevel.EACH_QUORUM);
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}
		assertThatConsistencyLevelsAreReinitialized();
	}

	@Test
	public void should_increment_n_counter_with_consistency_level() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		entity = manager.merge(entity);
		try {
			entity.getVersion().incr(10L, ConsistencyLevel.EACH_QUORUM);
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}
		assertThatConsistencyLevelsAreReinitialized();
	}

	@Test
	public void should_decrement_counter_with_consistency_level() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		entity = manager.merge(entity);
		try {
			entity.getVersion().decr(ConsistencyLevel.EACH_QUORUM);
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}
		assertThatConsistencyLevelsAreReinitialized();
	}

	@Test
	public void should_decrement_counter_n_with_consistency_level() throws Exception {
		CompleteBean entity = CompleteBeanTestBuilder.builder().randomId().name("name").buid();
		entity = manager.merge(entity);
		try {
			entity.getVersion().decr(10L, ConsistencyLevel.EACH_QUORUM);
		} catch (HInvalidRequestException e) {
			assertThat(e)
					.hasMessage(
							"InvalidRequestException(why:consistency level EACH_QUORUM not compatible with replication strategy (org.apache.cassandra.locator.SimpleStrategy))");
		}
		assertThatConsistencyLevelsAreReinitialized();
	}

	private void assertThatConsistencyLevelsAreReinitialized() {
		assertThat(policy.getCurrentReadLevel()).isNull();
		assertThat(policy.getCurrentWriteLevel()).isNull();
	}

	@After
	public void cleanThreadLocals() {
		policy.reinitCurrentConsistencyLevels();
		policy.reinitDefaultConsistencyLevels();
	}

	@AfterClass
	public static void cleanUp() {
		ThriftEmbeddedServer.policy().reinitCurrentConsistencyLevels();
		ThriftEmbeddedServer.policy().reinitDefaultConsistencyLevels();
	}
}
