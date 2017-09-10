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
package org.sodeac.eventdispatcher.common;

import java.util.List;
import java.util.Map;

import org.sodeac.eventdispatcher.api.IConcernEvent;
import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

public class FireSyncEvent implements IQueueJob,IConcernEvent
{
	private IQueuedEvent event = null;
	private String topic =  null;
	private Map<String,Object> properties = null;
	private boolean removeEvent = true;
	
	public FireSyncEvent(IQueuedEvent event, String topic, Map<String,Object> properties)
	{
		super();
		this.event = event;
		this.topic = topic;
		this.properties = properties;
	}
	
	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl)
	{
		// TODO Auto-generated method stub
		
	}
	
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
		queue.sendEvent(this.topic, this.properties);	
	}

	@Override
	public boolean concernEvent(IQueuedEvent event)
	{
		return this.event == event;
	}
}
