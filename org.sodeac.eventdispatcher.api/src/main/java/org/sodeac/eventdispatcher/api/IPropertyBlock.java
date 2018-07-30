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

import java.util.List;
import java.util.Map;

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
	 * @param propertySet with new values
	 * @param ignoreIfEquals switch to skip updates, if old value equals new value 
	 * 
	 * @return a map of previously property registered
	 * @throws PropertyIsLockedException
	 */
	public Map<String,Object> setPropertySet(Map<String,Object> propertySet, boolean ignoreIfEquals) throws PropertyIsLockedException;
	
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
	public <T> T getProperty(String key, Class<T> resultClass, T defaultValue);
	
	/**
	 * String-typed getter for registered property with associated {@code key}. For Non-string value {@link java.lang.Object#toString()} is used as formatter. 
	 * 
	 * @param key the key whose associated property is to be returned
	 * @param defaultValue return value if property with specified key not exist or is null/empty
	 * 
	 * @return the property with specified key, or {@code defaultValue} if property does not exists
	 */
	public String getNonEmptyStringProperty(String key, String defaultValue);
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
	 * returns an immutable  {@link java.util.List} of all registered property keys
	 * 
	 * @return
	 */
	public List<String> getPropertyKeys();
	
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
	 * Enables complex editing in locked mode with {@link IPropertyBlockOperationHandler}.
	 * 
	 * @param operationHandler handle to edit property
	 * @return audit trail
	 */
	public IPropertyBlockOperationResult operate(IPropertyBlockOperationHandler operationHandler);
}
