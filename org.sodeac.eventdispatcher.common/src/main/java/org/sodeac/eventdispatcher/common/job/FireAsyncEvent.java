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
package org.sodeac.eventdispatcher.common.job;

import java.util.List;
import java.util.Map;

import org.sodeac.eventdispatcher.api.IConcernEvent;
import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

public class FireAsyncEvent implements IQueueJob,IConcernEvent
{
	private IQueuedEvent event = null;
	private String topic =  null;
	private Map<String,Object> properties = null;
	private boolean removeEvent = true;
	
	public FireAsyncEvent(IQueuedEvent event, String topic, Map<String,Object> properties,boolean removeEvent)
	{
		super();
		this.event = event;
		this.topic = topic;
		this.properties = properties;
		
		if(this.topic == null)
		{
			this.topic = event.getEvent().getTopic();
		}
		if(this.properties == null)
		{
			this.properties = event.getNativeEventProperties();
		}
		
		this.removeEvent = removeEvent;
	}
	
	public FireAsyncEvent(IQueuedEvent event, boolean removeEvent)
	{
		this(event, null, null, removeEvent);
	}
	
	public FireAsyncEvent(IQueuedEvent event)
	{
		this(event, null, null, true);
	}
	
	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}
	
	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock properties, IJobControl jobControl, List<IQueueJob> currentProcessedJobList)
	{
		if((removeEvent && (this.event != null)))
		{
			try
			{
				queue.removeEvent(this.event.getUUID());
			}
			catch (Exception e) {}
		}
		queue.postEvent(this.topic, this.properties);	
	}

	@Override
	public boolean concernEvent(IQueuedEvent event)
	{
		return this.event == event;
	}
}
