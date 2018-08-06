/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.extension.jmx;

import java.util.Map;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.api.IDescriptionProvider;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IStateInfoProvider;

public class EventController implements EventControllerMBean
{
	public IQueueController eventController;
	public Map<String,?> properties;
	public ObjectName controllerObjectName;
	public String objectNamePrefix; 
	
	public EventController(IQueueController eventController,Map<String,?> properties,ObjectName name, String objectNamePrefix)
	{
		super();
		this.eventController = eventController;
		this.properties = properties;
		this.controllerObjectName = name;
		this.objectNamePrefix = objectNamePrefix;
	}
	
	public void dispose()
	{
	}

	@Override
	public String showStateInfo()
	{
		return this.eventController instanceof IStateInfoProvider ? ((IStateInfoProvider)this.eventController).getStateInfo() : "No state information available";
	}
	
	@Override
	public String showDescription()
	{
		return this.eventController instanceof IDescriptionProvider ? ((IDescriptionProvider)this.eventController).getDescription() : "No description available";
	}

	@Override
	public boolean isImplementsOnEventScheduled()
	{
		return this.eventController instanceof IOnScheduleEvent;
	}

	@Override
	public boolean isImplementsOnFireEvent()
	{
		return this.eventController instanceof IOnFireEvent;
	}

	@Override
	public boolean isImplementsOnJobDone()
	{
		return this.eventController instanceof IOnJobDone;
	}

	@Override
	public boolean isImplementsOnJobError()
	{
		return this.eventController instanceof IOnJobError;
	}

	@Override
	public boolean isImplementsOnJobTimeout()
	{
		return this.eventController instanceof IOnJobTimeout;
	}

	@Override
	public boolean isImplementsOnQueueObserve()
	{
		return this.eventController instanceof IOnQueueObserve;
	}

	@Override
	public boolean isImplementsOnQueueReserve()
	{
		return this.eventController instanceof IOnQueueReverse;
	}

	@Override
	public boolean isImplementsOnQueueSignal()
	{
		return this.eventController instanceof IOnQueueSignal;
	}

	@Override
	public boolean isImplementsOnRemoveEvent()
	{
		return this.eventController instanceof IOnRemoveEvent;
	}

	@Override
	public String getControllerClassName()
	{
		return this.eventController.getClass().getCanonicalName();
	}

}
