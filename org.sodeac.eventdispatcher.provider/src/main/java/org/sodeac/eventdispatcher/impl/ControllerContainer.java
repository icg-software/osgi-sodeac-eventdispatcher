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
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.sodeac.eventdispatcher.api.IDisableMetricsOnQueueObserve;
import org.sodeac.eventdispatcher.api.IEnableMetricsOnQueueObserve;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;

public class ControllerContainer
{
	private Map<String, ?> properties = null;
	private IEventController eventController = null;
	private List<ConsumeEventHandler> consumeEventHandlerList = null;
	private boolean registered = false;
	
	private String cachedControllerQueueConfigurationFilter = null;
	private Filter cachedControllerFilter = null;
	
	public Map<String, ?> getProperties()
	{
		return properties;
	}
	public void setProperties(Map<String, ?> properties)
	{
		this.properties = properties;
	}
	public IEventController getEventController()
	{
		return eventController;
	}
	public void setEventController(IEventController eventController)
	{
		this.eventController = eventController;
	}
	public boolean isRegistered()
	{
		return registered;
	}
	public void setRegistered(boolean registered)
	{
		this.registered = registered;
	}
	public ConsumeEventHandler addConsumeEventHandler(ConsumeEventHandler consumeEventHandler)
	{
		if(this.consumeEventHandlerList == null)
		{
			this.consumeEventHandlerList = new ArrayList<ConsumeEventHandler>();
		}
		this.consumeEventHandlerList.add(consumeEventHandler);
		return consumeEventHandler;
	}
	public List<ConsumeEventHandler> getConsumeEventHandlerList()
	{
		return consumeEventHandlerList;
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
	
	public boolean isEnableMetrics()
	{
		boolean enableMetrics = eventController instanceof IEnableMetricsOnQueueObserve;
		boolean disableMetrics = isDisableMetrics();
		if(disableMetrics)
		{
			enableMetrics = false;
		}
		return enableMetrics;
	}
	
	public boolean isDisableMetrics()
	{
		boolean disableMetrics = eventController instanceof IDisableMetricsOnQueueObserve;
		if(! disableMetrics)
		{
			Object disableMetricsProperty = properties.get(IEventController.PROPERTY_DISABLE_METRICS);
			if(disableMetricsProperty != null)
			{
				if(disableMetricsProperty instanceof Boolean)
				{
					disableMetrics = (Boolean)disableMetricsProperty;
				}
				else if (disableMetricsProperty instanceof String)
				{
					disableMetrics = ((String)disableMetricsProperty).equalsIgnoreCase("true");
				}
				else
				{
					disableMetrics = disableMetricsProperty.toString().equalsIgnoreCase("true");
				}
			}
		}
		return disableMetrics;
	}
	
	public synchronized Filter getGetQueueMatchFilter() throws InvalidSyntaxException
	{
		Filter filter = null;
		String queueConfigurationFilter = getNonEmptyStringProperty(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER,"");
		if(queueConfigurationFilter.isEmpty())
		{
			return filter;
		}
		
		if((cachedControllerQueueConfigurationFilter == null) || (!cachedControllerQueueConfigurationFilter.equals(queueConfigurationFilter)))
		{
			cachedControllerFilter = FrameworkUtil.createFilter(queueConfigurationFilter);
			cachedControllerQueueConfigurationFilter = queueConfigurationFilter;
		}
			
		filter = cachedControllerFilter;
		return filter;
	}
}
