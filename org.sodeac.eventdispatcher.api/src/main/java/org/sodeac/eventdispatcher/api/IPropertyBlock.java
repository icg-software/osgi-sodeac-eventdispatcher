/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A thread-safe property-container
 * 
 * @author Sebastian Palarus
 *
 */
public interface IPropertyBlock
{
	/**
	 * register a property {@code value} with associated {@code key}
	 * 
	 * @param key key with which the specified property is to be associated
	 * @param value property to be associated with the specified key 
	 * 
	 * @return previews property registered with {@code key}, or null
	 * @throws PropertyIsLockedException
	 */
	public Object setProperty(String key,Object value) throws PropertyIsLockedException;
	
	/**
	 * register a set of properties
	 * 
	 * @param propertyEntrySet with new values
	 * @param ignoreIfEquals switch to skip updates, if old value equals new value 
	 * 
	 * @return a map of previously property registered
	 * @throws PropertyIsLockedException
	 */
	public Map<String,Object> setPropertyEntrySet(Set<Entry<String,Object>> propertyEntrySet, boolean ignoreIfEquals) throws PropertyIsLockedException;
	
	/**
	 * getter for registered property with associated {@code key}
	 * 
	 * @param key the key whose associated property is to be returned
	 *  
	 * @return the property with specified key, or null if property does not exists
	 */
	public Object getProperty(String key);
	
	/**
	 * typed getter for registered property with associated {@code key}
	 * 
	 * @param key the key whose associated property is to be returned
	 * @param resultClass the type of property
	 * 
	 * @return the property with specified key
	 */
	public <T> T getProperty(String key, Class<T> resultClass);
	
	/**
	 * typed getter for registered property with associated {@code key}
	 * 
	 * @param key the key whose associated property is to be returned
	 * @param resultClass the type of property
	 * @param defaultValue return value if property with specified key not exist or is null
	 * 
	 * @return the property with specified key, or {@code defaultValue} if property does not exists
	 */
	public <T> T getPropertyOrDefault(String key, Class<T> resultClass, T defaultValue);
	
	/**
	 * typed getter for registered property with associated {@code key}
	 * 
	 * @param key the key whose associated property is to be returned
	 * @param resultClass the type of property
	 * @param propertySupplierIfNotExists factory to create property value if not exists , and store with specified key 
	 * 
	 * @return the property with specified key
	 */
	public default <T> T getPropertyOrSupply(String key, Class<T> resultClass, Supplier<T> propertySupplierIfNotExists)
	{
		final Conplier<T> valueContainer = new Conplier<T>()
		{
			private T value = null;
					
			@Override
			public void accept(T t)
			{
				this.value = t;
			}

			@Override
			public T get()
			{
				return this.value;
			}
			
		};
		
		computeProcedure(new IPropertyBlockAtomicProcedure()
		{
			@SuppressWarnings("unchecked")
			@Override
			public void accept(IPropertyBlock propertyBlock)
			{
				if(propertyBlock.containsKey(key))
				{
					valueContainer.accept((T)propertyBlock.getProperty(key));
				}
				else
				{
					valueContainer.accept(propertySupplierIfNotExists.get());
					propertyBlock.setProperty(key, valueContainer.get());
				}
			}
		});
		
		return valueContainer.get();
	}
	
	/**
	 * String-typed getter for registered property with associated {@code key}. For Non-string value {@link java.lang.Object#toString()} is used as formatter. 
	 * 
	 * @param key the key whose associated property is to be returned
	 * @param defaultValue return value if property with specified key not exist or is null/empty
	 * 
	 * @return the property with specified key, or {@code defaultValue} if property does not exists
	 */
	public String getPropertyOrDefaultAsString(String key, String defaultValue);
	
	/**
	 * remove registered property with associated {@code key}
	 * 
	 * @param key key the key whose associated property is to be removed
	 * 
	 * @return the removed property with specified key, or null if property does not exists
	 * @throws PropertyIsLockedException
	 */
	public Object removeProperty(String key) throws PropertyIsLockedException;
	
	/**
	 * returns an immutable deep copy of property-container as {@link java.util.Map}
	 *  
	 * @return {@link java.util.Map} with properties
	 */
	public Map<String, Object> getProperties();
	
	/**
	 * returns an immutable  {@link java.util.Set} of all registered property keys
	 * 
	 * @return
	 */
	public Set<String> getPropertyKeySet();
	
	/**
	 * Returns true if this propertyblock contains no entries
	 * 
	 * @return true if this propertyblock contains no entries
	 */
	public boolean isEmpty();
	
	/**
	 * returns true if this propertyblock contains entry with {@code key}  ( key==null ? k==null : key.equals(k)). 
	 * 
	 * @param key  key whose presence in this propertyblock is to be tested
	 * @return true if this propertyblock contains a entrymapped with specified key
	 */
	public boolean containsKey(Object key);
	
	/**
	 * remove all property entries
	 * 
	 * @return a map of previously property registered
	 * @throws PropertyIsLockedException
	 */
	public Map<String,Object> clear() throws PropertyIsLockedException;
	
	/**
	 * register an adapter 
	 * 
	 * @param adapterClass type of adapter
	 * @param adapter implementation of adapter
	 * @throws PropertyIsLockedException
	 */
	public default <T> void setAdapter(Class<T> adapterClass, T adapter) throws PropertyIsLockedException
	{
		setProperty(adapterClass.getCanonicalName(), adapter);
	}
	
	/**
	 * get registered adapter
	 * 
	 * @param adapterClass type of adapter
	 * 
	 * @return registered adapter with specified adapterClass
	 */
	public default <T> T getAdapter(Class<T> adapterClass)
	{
		return getProperty(adapterClass.getCanonicalName(), adapterClass);
	}
	
	/**
	 * get registered adapter
	 * 
	 * @param adapterClass type of adapter
	 * @param adapterFactoryIfNotExists factory to create adapter if not exists , and store with specified key 
	 * 
	 * @return registered adapter with specified adapterClass
	 */
	public default <T> T getAdapter(Class<T> adapterClass, Supplier<T> adapterFactoryIfNotExists)
	{
		return getPropertyOrSupply(adapterClass.getCanonicalName(), adapterClass, adapterFactoryIfNotExists);
	}
	
	/**
	 * remove registered adapter
	 * 
	 * @param adapterClass type of adapter
	 * @throws PropertyIsLockedException
	 */
	public default <T> void removeAdapter(Class<T> adapterClass) throws PropertyIsLockedException
	{
		removeProperty(adapterClass.getCanonicalName());
	}
	
	/**
	 * locks a property to prevent writable access
	 * 
	 * @param key the key whose associated property is to be locked
	 * @return a {@link IPropertyBlock} or null, if property is already locked
	 */
	public IPropertyLock lockProperty(String key);
	
	/**
	 * Enables complex editing in locked mode with {@link IPropertyBlockAtomicProcedure}.
	 * 
	 * @param operationHandler handle to edit property
	 * @return audit trail of modified property items
	 */
	public Supplier<List<PropertyBlockModifyItem>> computeProcedure(IPropertyBlockAtomicProcedure operationHandler);
	
	public interface Conplier<T> extends Supplier<T>,Consumer<T>{}
	
	/**
	 * helper to build a map in static way
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public static interface IMapBuilder<K,V>
	{
		/**
		 * put key-value pair to map
		 * 
		 * @param key key with which the specified value is to be associated
		 * @param value value to be associated with the specified key
		 * 
		 * @return builder
		 */
		public IMapBuilder<K,V> put(K key, V value);
		
		/**
		 * put all mapping to map
		 * 
		 * @param m mappings to be stored in this map
		 * @return builder
		 */
		public IMapBuilder<K,V> putAll(Map<? extends K, ? extends V> m);
		
		/**
		 * create immutable map
		 * 
		 * @return immutableMap
		 */
		public Map<K,V> buildImmutableMap();
		
		/**
		 * creates builder to build a map
		 * 
		 * @return builder to build a map
		 */
		public static <K, V> IMapBuilder<K,V> newBuilderInstance()
		{
			IMapBuilder<K, V> builder = new IMapBuilder<K, V>()
			{
				Map<K,V> map = new HashMap<K,V>();
				
				@Override
				public IMapBuilder<K, V> put(K key, V value)
				{
					map.put(key, value);
					return this;
				}
				
				@Override
				public IMapBuilder<K,V> putAll(Map<? extends K, ? extends V> m)
				{
					this.map.putAll(m);
					return this;
				}
				
				@Override
				public Map<K, V> buildImmutableMap()
				{
					return Collections.unmodifiableMap(map);
				}
			};
			
			return builder;
		}
	}
}
