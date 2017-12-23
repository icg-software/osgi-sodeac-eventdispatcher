package org.sodeac.eventdispatcher.extension.jmx;

import java.util.Map;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.api.IDescriptionProvider;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IOnEventScheduled;
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
	public IEventController eventController;
	public Map<String,?> properties;
	public ObjectName controllerObjectName;
	
	public EventController(IEventController eventController,Map<String,?> properties,ObjectName name)
	{
		super();
		this.eventController = eventController;
		this.properties = properties;
		this.controllerObjectName = name;
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
		return this.eventController instanceof IOnEventScheduled;
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
