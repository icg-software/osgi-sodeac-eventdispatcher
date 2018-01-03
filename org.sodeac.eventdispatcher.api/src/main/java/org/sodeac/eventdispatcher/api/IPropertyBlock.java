/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	 */
	public Object setProperty(String key,Object value);
	
	/**
	 * register a set of properties
	 * 
	 * @param propertySet with new values
	 * @param ignoreIfEquals switch to skip updates, if old value equals new value 
	 * 
	 * @return a map of previously property registered
	 */
	public Map<String,Object> setPropertySet(Map<String,Object> propertySet, boolean ignoreIfEquals);
	
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
	 * @param defaultValue return value if property with specified key not exist or is null
	 * 
	 * @return the property with specified key, or {@code defaultValue} if property does not exists
	 */
	public <T> T getTypedProperty(String key, T defaultValue);
	
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
	 */
	public Object removeProperty(String key);
	
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
	 */
	public Map<String,Object> clear();
}
