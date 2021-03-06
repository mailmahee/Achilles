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
package info.archinnov.achilles.entity.manager;

import static info.archinnov.achilles.configuration.ConfigurationParameters.*;
import info.archinnov.achilles.configuration.ArgumentExtractor;
import info.archinnov.achilles.configuration.ThriftArgumentExtractor;
import info.archinnov.achilles.consistency.AchillesConsistencyLevelPolicy;
import info.archinnov.achilles.consistency.ThriftConsistencyLevelPolicy;
import info.archinnov.achilles.context.ConfigurationContext.Impl;
import info.archinnov.achilles.context.ThriftDaoContext;
import info.archinnov.achilles.context.ThriftDaoContextBuilder;
import info.archinnov.achilles.context.ThriftPersistenceContextFactory;
import info.archinnov.achilles.table.ThriftColumnFamilyCreator;
import info.archinnov.achilles.type.ConsistencyLevel;
import info.archinnov.achilles.validation.Validator;

import java.util.Collections;
import java.util.Map;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThriftPersistenceManagerFactory extends PersistenceManagerFactory {

	private static final Logger log = LoggerFactory.getLogger(ThriftPersistenceManagerFactory.class);

	private Cluster cluster;
	private Keyspace keyspace;

	private ThriftDaoContext daoContext;
	private ThriftPersistenceContextFactory contextFactory;
	private ThriftConsistencyLevelPolicy policy;

	/**
	 * Create a new ThriftPersistenceManagerFactoryImpl with a configuration map
	 * 
	 * @param configurationMap
	 *            Check documentation for more details on configuration
	 *            parameters
	 */
	public ThriftPersistenceManagerFactory(Map<String, Object> configurationMap) {
		super(configurationMap, new ThriftArgumentExtractor());
		configContext.setImpl(Impl.THRIFT);

		ThriftArgumentExtractor thriftArgumentExtractor = new ThriftArgumentExtractor();
		cluster = thriftArgumentExtractor.initCluster(configurationMap);
		keyspace = thriftArgumentExtractor.initKeyspace(cluster,
				(ThriftConsistencyLevelPolicy) configContext.getConsistencyPolicy(), configurationMap);

		Validator.validateNotEmpty(entityPackages,
				"'%s' property should be set for Achilles ThrifPersistenceManagerFactory bootstraping",
				ENTITY_PACKAGES_PARAM);

		log.info("Initializing Achilles ThriftPersistenceManagerFactory for cluster '{}' and keyspace '{}' ",
				cluster.getName(), keyspace.getKeyspaceName());

		boolean hasSimpleCounter = bootstrap();
		new ThriftColumnFamilyCreator(cluster, keyspace).validateOrCreateTables(entityMetaMap, configContext,
				hasSimpleCounter);
		daoContext = new ThriftDaoContextBuilder().buildDao(cluster, keyspace, entityMetaMap, configContext,
				hasSimpleCounter);
		contextFactory = new ThriftPersistenceContextFactory(daoContext, configContext, entityMetaMap);

	}

	/**
	 * Create a new ThriftPersistenceManager. This instance of
	 * ThriftPersistenceManager is <strong>thread-safe</strong>
	 * 
	 * @return ThriftPersistenceManager
	 */
	public ThriftPersistenceManager createPersistenceManager() {
		log.info("Create new Thrift-based Persistence Manager ");

		return new ThriftPersistenceManager(Collections.unmodifiableMap(entityMetaMap), //
				contextFactory, daoContext, configContext);
	}

	/**
	 * Create a new state-full PersistenceManager for batch handling <br/>
	 * <br/>
	 * 
	 * <strong>WARNING : This PersistenceManager is state-full and not
	 * thread-safe. In case of exception, you MUST not re-use it but create
	 * another one</strong>
	 * 
	 * @return a new state-full PersistenceManager
	 */
	public ThriftBatchingPersistenceManager createBatchingPersistenceManager() {
		return new ThriftBatchingPersistenceManager(entityMetaMap, contextFactory, daoContext, configContext);
	}

	@Override
	protected AchillesConsistencyLevelPolicy initConsistencyLevelPolicy(Map<String, Object> configurationMap,
			ArgumentExtractor argumentExtractor) {
		log.info("Initializing new Achilles Configurable Consistency Level Policy from arguments ");

		ConsistencyLevel defaultReadConsistencyLevel = argumentExtractor
				.initDefaultReadConsistencyLevel(configurationMap);
		ConsistencyLevel defaultWriteConsistencyLevel = argumentExtractor
				.initDefaultWriteConsistencyLevel(configurationMap);
		Map<String, ConsistencyLevel> readConsistencyMap = argumentExtractor.initReadConsistencyMap(configurationMap);
		Map<String, ConsistencyLevel> writeConsistencyMap = argumentExtractor.initWriteConsistencyMap(configurationMap);

		policy = new ThriftConsistencyLevelPolicy(defaultReadConsistencyLevel, defaultWriteConsistencyLevel,
				readConsistencyMap, writeConsistencyMap);
		return policy;
	}

	protected void setThriftDaoContext(ThriftDaoContext thriftDaoContext) {
		this.daoContext = thriftDaoContext;
	}

	public ThriftConsistencyLevelPolicy getConsistencyPolicy() {
		return policy;
	}
}
