package org.sodeac.eventdispatcher.extension.jmx;

import java.util.Map;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.api.IEventController;

public class EventController implements EventControllerMBean
{
	public IEventController eventController;
	public Map<String,?> properties;
	public ObjectName name;
	
	public EventController(IEventController eventController,Map<String,?> properties,ObjectName name)
	{
		super();
		this.eventController = eventController;
		this.properties = properties;
		this.name = name;
	}

	@Override
	public void printInfo()
	{
		System.out.println("PrintINfo: " + this.eventController);

	}

}
