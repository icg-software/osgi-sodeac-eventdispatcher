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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueueService;

public class ServiceContainer
{
	private Map<String, ?> properties = null;
	private IQueueService queueService = null;
	private boolean registered = false;

	private volatile List<Filter> cachedServiceFilter = null;
	
	public Map<String, ?> getProperties()
	{
		return properties;
	}
	public void setProperties(Map<String, ?> properties)
	{
		this.properties = properties;
	}
	public IQueueService getQueueService()
	{
		return queueService;
	}
	public void setQueueService(IQueueService queueService)
	{
		this.queueService = queueService;
	}
	public boolean isRegistered()
	{
		return registered;
	}
	public void setRegistered(boolean registered)
	{
		this.registered = registered;
	}
	
	public String getNonEmptyStringProperty(String key, String defaultValue)
	{
		String stringValue = defaultValue;
		Object current = this.properties.get(key);
		if(current != null)
		{
			if(! (current instanceof String))
			{
				current = current.toString();
			}
		}
		if((current != null) && (! ((String)current).isEmpty()))
		{
			stringValue = (String)current;
		}
		else
		{
			stringValue = defaultValue;
		}
		return stringValue;
	}
	
	public void clearCache()
	{
		this.cachedServiceFilter = null;
	}
	
	@SuppressWarnings("unchecked")
	public synchronized List<Filter> getGetQueueMatchFilter() throws InvalidSyntaxException
	{
		List<Filter> serviceFilter = this.cachedServiceFilter;
		if(serviceFilter != null)
		{
			return serviceFilter;
		}
		serviceFilter = new ArrayList<Filter>();
		List<String> queueConfigurationFilterList = null;
		if(this.properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER) instanceof String)
		{
			queueConfigurationFilterList = new ArrayList<String>();
			queueConfigurationFilterList.add((String)this.properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER));
		}
		else if(this.properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER) instanceof String[])
		{
			queueConfigurationFilterList = new ArrayList<String>(Arrays.asList((String[])this.properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER)));
		}
		else if(this.properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER) instanceof Collection<?>)
		{
			queueConfigurationFilterList = new ArrayList<String>((Collection<String>)this.properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER));
		}
		else
		{
			queueConfigurationFilterList = new ArrayList<String>();
		}
		
		if(queueConfigurationFilterList.isEmpty() )
		{
			this.cachedServiceFilter = serviceFilter;
			return null;
		}
		
		for(String queueConfigurationFilter : queueConfigurationFilterList)
		{
			serviceFilter.add(FrameworkUtil.createFilter(queueConfigurationFilter));
		}
		this.cachedServiceFilter = serviceFilter;
		return serviceFilter;
	}
}
