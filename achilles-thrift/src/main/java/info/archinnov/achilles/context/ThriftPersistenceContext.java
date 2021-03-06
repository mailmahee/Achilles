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
package info.archinnov.achilles.context;

import info.archinnov.achilles.context.execution.SafeExecutionContext;
import info.archinnov.achilles.dao.ThriftCounterDao;
import info.archinnov.achilles.dao.ThriftGenericEntityDao;
import info.archinnov.achilles.dao.ThriftGenericWideRowDao;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.operations.EntityRefresher;
import info.archinnov.achilles.entity.operations.ThriftEntityLoader;
import info.archinnov.achilles.entity.operations.ThriftEntityMerger;
import info.archinnov.achilles.entity.operations.ThriftEntityPersister;
import info.archinnov.achilles.entity.operations.ThriftEntityProxifier;
import info.archinnov.achilles.exception.AchillesStaleObjectStateException;
import info.archinnov.achilles.proxy.EntityInterceptor;
import info.archinnov.achilles.type.ConsistencyLevel;
import info.archinnov.achilles.type.Options;
import me.prettyprint.hector.api.mutation.Mutator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThriftPersistenceContext extends PersistenceContext {
	private static final Logger log = LoggerFactory.getLogger(ThriftPersistenceContext.class);

	private ThriftEntityPersister persister = new ThriftEntityPersister();
	private ThriftEntityLoader loader = new ThriftEntityLoader();
	private ThriftEntityMerger merger = new ThriftEntityMerger();
	private ThriftEntityProxifier proxifier = new ThriftEntityProxifier();
	private EntityRefresher<ThriftPersistenceContext> refresher;

	private ThriftDaoContext daoContext;
	private ThriftGenericEntityDao entityDao;
	private ThriftGenericWideRowDao wideRowDao;
	private ThriftAbstractFlushContext<?> flushContext;

	public ThriftPersistenceContext(EntityMeta entityMeta, ConfigurationContext configContext,
			ThriftDaoContext daoContext, ThriftAbstractFlushContext<?> flushContext, Object entity, Options options) {
		super(entityMeta, configContext, entity, flushContext, options);
		log.trace("Create new persistence context for instance {} of class {}", entity, entityMeta.getClassName());

		initCollaborators(daoContext, flushContext);
		initDaos();
	}

	public ThriftPersistenceContext(EntityMeta entityMeta, ConfigurationContext configContext,
			ThriftDaoContext daoContext, ThriftAbstractFlushContext<?> flushContext, Class<?> entityClass,
			Object primaryKey, Options options) {
		super(entityMeta, configContext, entityClass, primaryKey, flushContext, options);
		log.trace("Create new persistence context for instance {} of class {}", entity, entityClass.getCanonicalName());

		initCollaborators(daoContext, flushContext);
		initDaos();
	}

	private void initCollaborators(ThriftDaoContext thriftDaoContext, ThriftAbstractFlushContext<?> flushContext) {
		refresher = new EntityRefresher<ThriftPersistenceContext>(loader, proxifier);
		this.daoContext = thriftDaoContext;
		this.flushContext = flushContext;
	}

	private void initDaos() {
		String tableName = entityMeta.getTableName();
		if (entityMeta.isClusteredEntity()) {
			this.wideRowDao = daoContext.findWideRowDao(tableName);
		} else {
			this.entityDao = daoContext.findEntityDao(tableName);
		}
	}

	@Override
	public ThriftPersistenceContext duplicate(Object entity) {
		return new ThriftPersistenceContext(entityMeta, configContext, daoContext, flushContext.duplicate(), entity,
				options.duplicateWithoutTtlAndTimestamp());
	}

	@Override
	public void persist() {
		flushContext.getConsistencyContext().executeWithWriteConsistencyLevel(new SafeExecutionContext<Void>() {
			@Override
			public Void execute() {
				persister.persist(ThriftPersistenceContext.this);
				flush();
				return null;
			}
		});
	}

	@Override
	public <T> T merge(final T entity) {

		return flushContext.getConsistencyContext().executeWithWriteConsistencyLevel(new SafeExecutionContext<T>() {
			@Override
			public T execute() {
				T merged = merger.<T> merge(ThriftPersistenceContext.this, entity);
				flush();
				return merged;
			}
		});

	}

	@Override
	public void remove() {
		flushContext.getConsistencyContext().executeWithWriteConsistencyLevel(new SafeExecutionContext<Void>() {
			@Override
			public Void execute() {
				persister.remove(ThriftPersistenceContext.this);
				flush();
				return null;
			}
		});
	}

	@Override
	public <T> T find(final Class<T> entityClass) {
		T entity = flushContext.getConsistencyContext().executeWithReadConsistencyLevel(new SafeExecutionContext<T>() {
			@Override
			public T execute() {
				return loader.<T> load(ThriftPersistenceContext.this, entityClass);
			}
		});

		if (entity != null) {
			entity = proxifier.buildProxy(entity, this);
		}
		return entity;
	}

	@Override
	public <T> T getReference(Class<T> entityClass) {
		setLoadEagerFields(false);
		return find(entityClass);
	}

	@Override
	public void refresh() throws AchillesStaleObjectStateException {
		flushContext.getConsistencyContext().executeWithReadConsistencyLevel(new SafeExecutionContext<Void>() {
			@Override
			public Void execute() {
				try {
					refresher.refresh(ThriftPersistenceContext.this);
				} catch (AchillesStaleObjectStateException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

	}

	@Override
	public <T> T initialize(final T entity) {
		log.debug("Force lazy fields initialization for entity {}", entity);
		final EntityInterceptor<ThriftPersistenceContext, T> interceptor = proxifier.getInterceptor(entity);

		flushContext.getConsistencyContext().executeWithReadConsistencyLevel(new SafeExecutionContext<Void>() {
			@Override
			public Void execute() {
				initializer.initializeEntity(entity, entityMeta, interceptor);
				return null;
			}
		});

		return entity;
	}

	public <T> T executeWithReadConsistencyLevel(SafeExecutionContext<T> context, ConsistencyLevel readLevel) {
		return flushContext.getConsistencyContext().executeWithReadConsistencyLevel(context, readLevel);
	}

	public <T> T executeWithWriteConsistencyLevel(SafeExecutionContext<T> context, ConsistencyLevel writeLevel) {
		return flushContext.getConsistencyContext().executeWithWriteConsistencyLevel(context, writeLevel);
	}

	public boolean isValueless() {
		return entityMeta.isValueless();
	}

	public ThriftGenericEntityDao findEntityDao(String tableName) {
		return daoContext.findEntityDao(tableName);
	}

	public ThriftGenericWideRowDao findWideRowDao(String tableName) {
		return daoContext.findWideRowDao(tableName);
	}

	public ThriftCounterDao getCounterDao() {
		return daoContext.getCounterDao();
	}

	public Mutator<Object> getEntityMutator(String tableName) {
		return flushContext.getEntityMutator(tableName);
	}

	public Mutator<Object> getWideRowMutator(String tableName) {
		return flushContext.getWideRowMutator(tableName);
	}

	public Mutator<Object> getCounterMutator() {
		return flushContext.getCounterMutator();
	}

	public ThriftGenericEntityDao getEntityDao() {
		return entityDao;
	}

	public ThriftGenericWideRowDao getWideRowDao() {
		return wideRowDao;
	}

}
