/*******************************************************************************
 * Copyright (c) 2017, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.common.task;

import java.util.Map;

import org.sodeac.eventdispatcher.api.IConcernEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueTaskContext;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

public class FireSyncEvent implements IQueueTask,IConcernEvent
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
	public void run(IQueueTaskContext taskContext)
	{
		IQueue queue = taskContext.getQueue();
		
		if((removeEvent && (this.event != null)))
		{
			try
			{
				queue.removeEvent(this.event.getUUID());
			}
			catch (Exception e) {e.printStackTrace();} // TODO log
		}
		queue.sendEvent(this.topic, this.properties);	
	}

	@Override
	public boolean concernEvent(IQueuedEvent event)
	{
		return this.event == event;
	}
}
