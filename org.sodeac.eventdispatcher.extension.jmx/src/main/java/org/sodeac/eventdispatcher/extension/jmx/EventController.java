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
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
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
		return this.eventController instanceof IOnQueuedEvent;
	}

	@Override
	public boolean isImplementsOnFireEvent()
	{
		return this.eventController instanceof IOnFiredEvent;
	}

	@Override
	public boolean isImplementsOnJobDone()
	{
		return this.eventController instanceof IOnTaskDone;
	}

	@Override
	public boolean isImplementsOnJobError()
	{
		return this.eventController instanceof IOnTaskError;
	}

	@Override
	public boolean isImplementsOnJobTimeout()
	{
		return this.eventController instanceof IOnTaskTimeout;
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
		return this.eventController instanceof IOnRemovedEvent;
	}

	@Override
	public String getControllerClassName()
	{
		return this.eventController.getClass().getCanonicalName();
	}

}
