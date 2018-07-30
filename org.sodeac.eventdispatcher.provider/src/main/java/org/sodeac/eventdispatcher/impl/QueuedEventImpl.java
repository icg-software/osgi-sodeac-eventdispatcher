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
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.event.Event;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IScheduleResult;

public class QueuedEventImpl implements IQueuedEvent
{
	private ScheduleResultImpl scheduleResult = null;
	private QueueImpl queue = null;
	private Event event = null;
	private String uuid = null;
	
	private ReentrantLock lock = null;
	private volatile PropertyBlockImpl propertyBlock = null;
	private volatile Map<String,Object> nativeProperties;
	private List<String> emptyKeyList = null;
	private Map<String, Object>  emptyProperties = null;
	private long createTimeStamp;
	
	public QueuedEventImpl(Event event,QueueImpl queue)
	{
		super();
		this.event = event;
		this.queue = queue;
		this.lock = new ReentrantLock();
		this.uuid = UUID.randomUUID().toString();
		this.createTimeStamp = System.currentTimeMillis();
	}

	@Override
	public Event getEvent()
	{
		return this.event;
	}
	
	@Override
	public IScheduleResult getScheduleResultObject()
	{
		return this.scheduleResult;
	}

	public void setScheduleResultObject(ScheduleResultImpl scheduleResult)
	{
		this.scheduleResult = scheduleResult;
	}

	@Override
	public Object setProperty(String key, Object value)
	{
		if(this.propertyBlock == null)
		{
			lock.lock();
			try
			{
				if(this.propertyBlock == null)
				{
					this.propertyBlock =  (PropertyBlockImpl)queue.getDispatcher().createPropertyBlock();
					this.emptyKeyList = null;
					this.emptyProperties = null;
				}
			}
			finally 
			{
				lock.unlock();
			}
		}
		
		return this.propertyBlock.setProperty(key, value);
	}

	@Override
	public Object getProperty(String key)
	{
		if(this.propertyBlock == null)
		{
			return null;
		}
		
		return this.propertyBlock.getProperty(key);
	}

	@Override
	public String getUUID()
	{
		return uuid;
	}

	@Override
	public List<String> getPropertyKeys()
	{
		if(this.propertyBlock == null)
		{
			List<String> returnList = this.emptyKeyList;
			if(returnList ==  null)
			{
				this.lock.lock();
				try
				{
					if(this.propertyBlock == null)
					{
						if(this.emptyKeyList == null)
						{
							this.emptyKeyList = Collections.unmodifiableList(new ArrayList<String>());
						}
						returnList = this.emptyKeyList;
					}
				}
				finally 
				{
					lock.unlock();
				}
			}
			if(returnList != null)
			{
				return returnList;
			}
		}
		return this.propertyBlock.getPropertyKeys();
	}

	@Override
	public Map<String, Object> getProperties()
	{
		if(this.propertyBlock == null)
		{
			Map<String,Object> returnIndex = this.emptyProperties;
			if(returnIndex ==  null)
			{
				lock.lock();
				try
				{
					if(this.propertyBlock == null)
					{
						if(this.emptyProperties == null)
						{
							this.emptyProperties = Collections.unmodifiableMap(new HashMap<String,Object>());
						}
						returnIndex = this.emptyProperties;
					}
				}
				finally 
				{
					lock.unlock();
				}
			}
			if(returnIndex != null)
			{
				return returnIndex;
			}
		}
		return this.propertyBlock.getProperties();
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

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapterClass)
	{
		if(adapterClass == IPropertyBlock.class)
		{
			if(this.propertyBlock == null)
			{
				lock.lock();
				try
				{
					if(this.propertyBlock == null)
					{
						this.propertyBlock =  (PropertyBlockImpl)queue.getDispatcher().createPropertyBlock();
						this.emptyKeyList = null;
						this.emptyProperties = null;
					}
				}
				finally 
				{
					lock.unlock();
				}
			}
			return (T)this.propertyBlock;
		}
		return IQueuedEvent.super.getAdapter(adapterClass);
	}

	@Override
	public IQueue getQueue()
	{
		return this.queue;
	}

	@Override
	public long getCreateTimeStamp()
	{
		return this.createTimeStamp;
	}

}
