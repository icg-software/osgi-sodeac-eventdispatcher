package org.sodeac.eventdispatcher.itest.components;

import java.util.ArrayList;
import java.util.List;

public class TracingObject
{
	public static final String PROPERTY_KEY_TRACING_OBJECT = "TRACING_OBJECT";
	
	public TracingObject()
	{
		super();
		this.tracingEventList = new ArrayList<TracingEvent>();
	}
	
	private List<TracingEvent> tracingEventList = null;

	public List<TracingEvent> getTracingEventList()
	{
		return tracingEventList;
	}
}
