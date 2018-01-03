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

import java.util.Map;

import org.sodeac.eventdispatcher.api.IQueueService;

public class ServiceContainer
{
	private Map<String, ?> properties = null;
	private IQueueService queueService = null;
	private boolean registered = false;
	
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
}
