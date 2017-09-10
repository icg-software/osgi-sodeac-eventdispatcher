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
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.sodeac.eventdispatcher.api.IPropertyBlock;

public class PropertyBlockImpl implements IPropertyBlock
{
	public PropertyBlockImpl()
	{
		super();
		
		this.propertiesLock = new ReentrantReadWriteLock(true);
		this.propertiesReadLock = this.propertiesLock.readLock();
		this.propertiesWriteLock = this.propertiesLock.writeLock();
	}
	
	private Map<String,Object> properties;
	private Map<String,Object> propertiesCopy;
	private ReentrantReadWriteLock propertiesLock;
	private ReadLock propertiesReadLock;
	private WriteLock propertiesWriteLock;
	
	@Override
	public Object setProperty(String key, Object value)
	{
		Object old = null;
		
		propertiesWriteLock.lock();
		try
		{
			if(this.properties == null)
			{
				this.properties = new HashMap<String,Object>();
			}
			else
			{
				old = this.properties.get(key);
			}
			this.properties.put(key, value);
			this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(this.properties));
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		return old;
	}

	@Override
	public Object getProperty(String key)
	{
		if(this.properties == null)
		{
			return null;
		}
		
		try
		{
			propertiesReadLock.lock();
			return this.properties.get(key);
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
	}
	
	@Override
	public Object removeProperty(String key)
	{
		if(this.properties == null)
		{
			return null;
		}
		
		try
		{
			propertiesReadLock.lock();
			if(! properties.containsKey(key))
			{
				return null;
			}
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
		
		try
		{
			propertiesWriteLock.lock();
			Object oldPropertyValue = this.properties.get(key);
			this.properties.remove(key);
			return oldPropertyValue;
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
	}

	@Override
	public List<String> getPropertyKeys()
	{
		if(this.properties == null)
		{
			return new ArrayList<String>();
		}
		try
		{
			propertiesReadLock.lock();
			List<String> keyList = new ArrayList<String>();
			for(String key : this.properties.keySet())
			{
				keyList.add(key);
			}
			return Collections.unmodifiableList(keyList);
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
	}
	
	@Override
	public Map<String, Object> getProperties()
	{
		Map<String,Object> props = this.propertiesCopy;
		if(props == null)
		{
			propertiesWriteLock.lock();
			try
			{
				if(this.properties == null)
				{
					this.properties = new HashMap<String,Object>();
				}
				this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(this.properties));
				props = this.propertiesCopy;
			}
			finally 
			{
				propertiesWriteLock.unlock();
			} 
		}
		return props;
	}
	
}
