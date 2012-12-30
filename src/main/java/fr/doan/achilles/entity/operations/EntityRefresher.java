package fr.doan.achilles.entity.operations;

import java.io.Serializable;
import java.util.Map;

import net.sf.cglib.proxy.Factory;
import fr.doan.achilles.entity.EntityHelper;
import fr.doan.achilles.entity.metadata.EntityMeta;
import fr.doan.achilles.proxy.interceptor.JpaEntityInterceptor;

/**
 * EntityRefresher
 * 
 * @author DuyHai DOAN
 * 
 */
public class EntityRefresher
{

	private EntityHelper helper = new EntityHelper();
	private EntityValidator entityValidator = new EntityValidator();
	private EntityLoader loader = new EntityLoader();

	@SuppressWarnings(
	{
			"rawtypes",
			"unchecked"
	})
	public void refresh(Object entity, Map<Class<?>, EntityMeta<?>> entityMetaMap)
	{
		entityValidator.validateEntity(entity, entityMetaMap);

		Factory proxy = (Factory) entity;
		JpaEntityInterceptor interceptor = (JpaEntityInterceptor) proxy.getCallback(0);

		Class<?> entityClass = interceptor.getTarget().getClass();
		EntityMeta<?> entityMeta = entityMetaMap.get(entityClass);
		Object primaryKey = helper.determinePrimaryKey(entity, entityMeta);

		Object freshEntity = this.loader.load(entityClass, (Serializable) primaryKey,
				(EntityMeta) entityMeta);

		interceptor.getDirtyMap().clear();
		interceptor.getLazyLoaded().clear();
		interceptor.setTarget(freshEntity);
	}
}
