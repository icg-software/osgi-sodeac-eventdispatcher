/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.components.base;

import java.util.concurrent.CountDownLatch;

import org.osgi.service.event.Event;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;

public class AbstractBaseTestController
{
	public static final String EVENT_PROPERTY_LATCH = "LATCH";
	public static final String SIGNAL_REMOVE_EVENT = "REMOVE_EVENT";
	
	protected TracingObject tracingObject = new TracingObject();
	protected CountDownLatch latch = null;
		
	public void onQueueDetach(IQueue queue)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_DETACH,queue));
	}

	public void onQueueAttach(IQueue queue)
	{
		queue.getStatePropertyBlock().setProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT, this.tracingObject);
		
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_ATTACH,queue));
	}

	public void onRemovedEvent(IQueuedEvent event)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_REMOVE_EVENT,event));
		event.getQueue().signal(SIGNAL_REMOVE_EVENT);
	}

	public void onFiredEvent(Event event, IQueue queue)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_FIRE_EVENT,queue));
	}

	public void onTaskDone(IQueue queue, IQueueTask job)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_JOB_DONE,job));
		if(this.latch != null)
		{
			latch.countDown();
		}
	}

	public void onTaskTimeout(IQueue queue, IQueueTask job)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_JOB_TIMEOUT,job));
		if(this.latch != null)
		{
			latch.countDown();
		}
	}

	public void onTaskError(IQueue queue, IQueueTask job, Throwable exception)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_JOB_ERROR,job,(Exception)exception));
		if(this.latch != null)
		{
			latch.countDown();
		}
	}

	public void onQueueSignal(IQueue queue, String signal)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_SIGNAL,queue, signal));
	}
}
