/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;

@Component(immediate=true,service=DefaultEventDispatcherFactory.class,scope=ServiceScope.SINGLETON)
public class DefaultEventDispatcherFactory
{
	@Reference(target = EventDispatcherConstants.EVENTDISPATCHER_COMPONENT_FACTORY_FILTER )
	private ComponentFactory factory;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile LogService logService = null;
	
	private ComponentInstance instance = null;
	
	@Activate
	public void activate(ComponentContext context)
	{
		Dictionary<String, Object> props = new Hashtable<String,Object>();
		props.put(EventDispatcherConstants.PROPERTY_ID, EventDispatcherConstants.DEFAULT_DISPATCHER_ID);
		props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);		// set default as default by highest ranking
		
		try
		{
			instance = this.factory.newInstance(props);
		}
		catch (Exception e) 
		{
			LogService logService = this.logService;
			ServiceReference<?> serviceReference = context == null ? null : context.getServiceReference();
			if(logService != null)
			{
				logService.log(serviceReference,LogService.LOG_ERROR, "error create new event dispatcher instance",e);
			}
			else
			{
				e.printStackTrace();
			}
		}
	}
	
	@Deactivate
	public void deactivate(ComponentContext context)
	{
		if(this.instance != null)
		{
			try
			{
				this.instance.dispose();
			}
			catch (Exception e) 
			{
				LogService logService = this.logService;
				ServiceReference<?> serviceReference = context == null ? null : context.getServiceReference();
				
				if(logService != null)
				{
					logService.log(serviceReference,LogService.LOG_ERROR, "error dispose event dispatcher instance",e);
				}
				else
				{
					e.printStackTrace();
				}
			}
		}
	}
}
