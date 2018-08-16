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
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;

public class AbstractBaseTestController
{
	public static final String EVENT_PROPERTY_LATCH = "LATCH";
	public static final String SIGNAL_REMOVE_EVENT = "REMOVE_EVENT";
	
	protected TracingObject tracingObject = new TracingObject();
	protected CountDownLatch latch = null;
		
	public void onQueueReverse(IQueue queue)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_REVERSE,queue));
	}

	public void onQueueObserve(IQueue queue)
	{
		queue.getStatePropertyBlock().setProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT, this.tracingObject);
		
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_OBSERVE,queue));
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

	public void onJobDone(IQueueJob job)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_JOB_DONE,job));
		if(this.latch != null)
		{
			latch.countDown();
		}
	}

	public void onJobTimeout(IQueueJob job)
	{
		this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_JOB_TIMEOUT,job));
		if(this.latch != null)
		{
			latch.countDown();
		}
	}

	public void onJobError(IQueueJob job, Throwable exception)
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
