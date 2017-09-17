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

import org.osgi.service.event.Event;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

public class QueuedEventImpl implements IQueuedEvent
{
	private QueueImpl queue = null;
	private Event event = null;
	private String uuid = null;
	
	private Object lockPropertyInstance = null;
	private volatile PropertyBlockImpl propertyBlock = null;
	private volatile Map<String,Object> nativeProperties;
	private List<String> emptyKeyList = null;
	private Map<String, Object>  emptyProperties = null;
	
	public QueuedEventImpl(Event event,QueueImpl queue)
	{
		super();
		this.event = event;
		this.queue = queue;
		this.lockPropertyInstance = new Object();
		this.uuid = UUID.randomUUID().toString();
	}

	@Override
	public Event getEvent()
	{
		return this.event;
	}

	@Override
	public Object setProperty(String key, Object value)
	{
		if(this.propertyBlock == null)
		{
			synchronized (lockPropertyInstance)
			{
				if(this.propertyBlock == null)
				{
					this.propertyBlock =  new PropertyBlockImpl();
					this.emptyKeyList = null;
					this.emptyProperties = null;
				}
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
				synchronized (this.lockPropertyInstance)
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
				synchronized (this.lockPropertyInstance)
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

	@Override
	public IQueue getQueue()
	{
		return this.queue;
	}

}
