/*******************************************************************************
 * Copyright (c) 2017 Sebastian Palarus
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
	 * getter for registered property with associated {@code key}
	 * 
	 * @param key the key whose associated property is to be returned
	 *  
	 * @return the property with specified key, or null if property does not exists
	 */
	public Object getProperty(String key);
	
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
	 * remove all property entries
	 */
	public void clear();
}
