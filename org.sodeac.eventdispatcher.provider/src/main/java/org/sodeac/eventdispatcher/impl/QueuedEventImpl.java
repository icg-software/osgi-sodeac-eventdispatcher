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
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.service.event.Event;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

public class QueuedEventImpl implements IQueuedEvent
{
	private QueueImpl queue = null;
	private Event event = null;
	private String uuid = null;
	
	private Map<String,Object> properties;
	private ReentrantReadWriteLock propertiesLock;
	private ReadLock propertiesReadLock;
	private WriteLock propertiesWriteLock;
	private Map<String,Object> propertiesCopy;
	private volatile Map<String,Object> nativeProperties;
	
	public QueuedEventImpl(Event event,QueueImpl queue)
	{
		super();
		this.event = event;
		this.queue = queue;
		this.uuid = UUID.randomUUID().toString();
		this.propertiesLock = new ReentrantReadWriteLock(true);
		this.propertiesReadLock = this.propertiesLock.readLock();
		this.propertiesWriteLock = this.propertiesLock.writeLock();
	}

	@Override
	public Event getEvent()
	{
		return this.event;
	}

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
	public String getUUID()
	{
		return uuid;
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

	@Override
	public Map<String, Object> getNativeEventProperties()
	{
		Map<String,Object> props = nativeProperties;
		if(props == null)
		{
			props = new HashMap<String,Object>();
			for(String nm : event.getPropertyNames())
			{
				props.put(nm, event.getProperty(nm));
			}
			nativeProperties = props;
		}
		return props;
	}

	@Override
	public IQueue getQueue()
	{
		return this.queue;
	}

}
