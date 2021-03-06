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
package info.archinnov.achilles.proxy.wrapper;

import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Mockito.*;
import info.archinnov.achilles.context.PersistenceContext;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.entity.operations.EntityProxifier;
import info.archinnov.achilles.test.mapping.entity.CompleteBean;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MapEntryWrapperTest {
	@Mock
	private Map<Method, PropertyMeta> dirtyMap;

	@Mock
	private EntityProxifier<PersistenceContext> proxifier;

	private Method setter;

	@Mock
	private PropertyMeta propertyMeta;

	@Mock
	private PersistenceContext context;

	@Before
	public void setUp() throws Exception {
		setter = CompleteBean.class.getDeclaredMethod("setFriends", List.class);
		when(propertyMeta.type()).thenReturn(PropertyType.MAP);
	}

	@Test
	public void should_mark_dirty_on_value_set() throws Exception {
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(1, "FR");
		map.put(2, "Paris");
		map.put(3, "75014");
		Entry<Integer, String> mapEntry = map.entrySet().iterator().next();

		MapEntryWrapper mapEntryWrapper = new MapEntryWrapper((Entry) mapEntry);
		mapEntryWrapper.setProxifier(proxifier);
		mapEntryWrapper.setDirtyMap(dirtyMap);
		mapEntryWrapper.setSetter(setter);
		mapEntryWrapper.setPropertyMeta(propertyMeta);
		when(proxifier.unwrap("TEST")).thenReturn("TEST");
		mapEntryWrapper.setValue("TEST");

		verify(dirtyMap).put(setter, propertyMeta);

	}

	@Test
	public void should_equal() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(4, "csdf");
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, "csdf");

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);
		wrapper1.setProxifier(proxifier);
		wrapper1.setPropertyMeta(propertyMeta);
		wrapper2.setProxifier(proxifier);
		wrapper2.setPropertyMeta(propertyMeta);

		when(proxifier.unwrap("csdf")).thenReturn("csdf");

		assertThat(wrapper1.equals(wrapper2)).isTrue();
	}

	@Test
	public void should_not_equal_when_values_differ() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(4, "csdf");
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, "df");

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);

		wrapper1.setProxifier(proxifier);
		wrapper1.setPropertyMeta(propertyMeta);
		wrapper2.setProxifier(proxifier);
		wrapper2.setPropertyMeta(propertyMeta);

		when(proxifier.unwrap("csdf")).thenReturn("csdf");
		when(proxifier.unwrap("df")).thenReturn("df");

		assertThat(wrapper1.equals(wrapper2)).isFalse();
	}

	@Test
	public void should_not_equal_when_one_value_null() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(4, "csdf");
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, null);

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);

		wrapper1.setProxifier(proxifier);
		wrapper1.setPropertyMeta(propertyMeta);
		wrapper2.setProxifier(proxifier);
		wrapper2.setPropertyMeta(propertyMeta);

		when(proxifier.unwrap((Object) null)).thenReturn(null);
		assertThat(wrapper1.equals(wrapper2)).isFalse();
	}

	@Test
	public void should_equal_compare_key_when_both_values_null() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(4, null);
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, null);

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);

		wrapper1.setProxifier(proxifier);
		wrapper1.setPropertyMeta(propertyMeta);
		wrapper2.setProxifier(proxifier);
		wrapper2.setPropertyMeta(propertyMeta);

		when(proxifier.unwrap((Object) null)).thenReturn(null);
		assertThat(wrapper1.equals(wrapper2)).isTrue();
	}

	@Test
	public void should_not_equal_when_keys_differ() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(1, null);
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, null);

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);

		wrapper1.setProxifier(proxifier);
		wrapper1.setPropertyMeta(propertyMeta);
		wrapper2.setProxifier(proxifier);
		wrapper2.setPropertyMeta(propertyMeta);

		when(proxifier.unwrap((Object) null)).thenReturn(null);
		assertThat(wrapper1.equals(wrapper2)).isFalse();
	}

	@Test
	public void should_same_hashcode_when_same_keys_and_values() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(4, "abc");
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, "abc");

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);

		assertThat(wrapper1.hashCode()).isEqualTo(wrapper2.hashCode());
	}

	@Test
	public void should_different_hashcode_when_values_differ() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(4, "abc");
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, null);

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);

		assertThat(wrapper1.hashCode()).isNotEqualTo(wrapper2.hashCode());
	}

	@Test
	public void should_different_hashcode_when_keys_differ() throws Exception {
		Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<Integer, String>(1, "abc");
		Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<Integer, String>(4, "abc");

		MapEntryWrapper wrapper1 = new MapEntryWrapper((Entry) entry1);
		MapEntryWrapper wrapper2 = new MapEntryWrapper((Entry) entry2);

		assertThat(wrapper1.hashCode()).isNotEqualTo(wrapper2.hashCode());
	}
}
