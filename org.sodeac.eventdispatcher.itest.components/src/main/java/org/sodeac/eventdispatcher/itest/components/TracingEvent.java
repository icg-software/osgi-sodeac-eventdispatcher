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
package org.sodeac.eventdispatcher.itest.components;

import org.osgi.service.event.Event;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

public class TracingEvent
{
	public static final int ON_QUEUE_OBSERVE = 1;
	public static final int ON_EVENT_SCHEDULED = 2;
	public static final int ON_REMOVE_EVENT = 3;
	public static final int ON_FIRE_EVENT = 4;
	public static final int ON_JOB_DONE = 5;
	public static final int ON_JOB_TIMEOUT = 6;
	public static final int ON_JOB_ERROR = 7;
	public static final int ON_QUEUE_SIGNAL = 8;
	
	public static final int SEND_EVENT = 30;
	
	public static final int ON_QUEUE_REVERSE = 99;
	
	public TracingEvent(int methode,IQueue queue)
	{
		super();
		this.timestamp = System.currentTimeMillis();
		this.queue = queue;
		this.methode = methode;
	}
	
	public TracingEvent(int methode,IQueue queue, String signal)
	{
		super();
		this.timestamp = System.currentTimeMillis();
		this.queue = queue;
		this.methode = methode;
		this.signal = signal;
	}
	
	public TracingEvent(int methode,IQueue queue, Event event)
	{
		super();
		this.timestamp = System.currentTimeMillis();
		this.queue = queue;
		this.methode = methode;
		this.rawEvent = event;
	}
	
	public TracingEvent(int methode,IQueuedEvent event)
	{
		super();
		this.timestamp = System.currentTimeMillis();
		this.queue = event.getQueue();
		this.event = event;
		this.methode = methode;
	}
	
	public TracingEvent(int methode,IQueueJob job)
	{
		super();
		this.timestamp = System.currentTimeMillis();
		this.job = job;
		this.methode = methode;
	}
	
	public TracingEvent(int methode,IQueueJob job,Exception exception)
	{
		super();
		this.timestamp = System.currentTimeMillis();
		this.job = job;
		this.methode = methode;
		this.exception = exception;
	}
	
	private long timestamp;
	private int methode;
	private IQueue queue;
	private IQueuedEvent event;
	private IQueueJob job;
	private Exception exception;
	private String signal;
	private Event rawEvent;
	
	public long getTimestamp()
	{
		return timestamp;
	}
	public int getMethode()
	{
		return methode;
	}
	public IQueue getQueue()
	{
		return queue;
	}
	public IQueuedEvent getEvent()
	{
		return event;
	}
	public Event getRawEvent()
	{
		return rawEvent;
	}
	public IQueueJob getJob()
	{
		return job;
	}
	public Exception getException()
	{
		return exception;
	}
	public String getSignal()
	{
		return signal;
	}
}
