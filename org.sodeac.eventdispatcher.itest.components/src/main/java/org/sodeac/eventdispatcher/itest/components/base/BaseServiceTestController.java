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
package org.sodeac.eventdispatcher.itest.components.base;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IQueueTaskContext;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IQueueController.class,EventHandler.class,IQueueService.class},
	property=
	{
		EventDispatcherConstants.PROPERTY_QUEUE_ID+"="+BaseServiceTestController.QUEUE_ID,
		EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL+"="+ BaseServiceTestController.PRI,
		EventDispatcherConstants.PROPERTY_SERVICE_ID+"=" + BaseServiceTestController.SERVICE_ID,
		EventConstants.EVENT_TOPIC+"=" + BaseServiceTestController.RESCHEDULE_EVENT1,
		EventConstants.EVENT_TOPIC+"=" + BaseServiceTestController.RESCHEDULE_EVENT2,
		EventConstants.EVENT_TOPIC+"=" + BaseServiceTestController.RESCHEDULE_EVENT3
	}
)

public class BaseServiceTestController extends AbstractBaseTestController implements EventHandler,IQueueController,IOnQueuedEvent,IOnRemovedEvent,IOnTaskDone,IOnTaskError,IOnTaskTimeout,IOnFiredEvent,IOnQueueAttach,IOnQueueDetach,IOnQueueSignal,IQueueService
{
	public static final String	SERVICE_ID			= "TestService";
	public static final String 	QUEUE_ID 			= "baseservicetestqueue";
	public static final String 	PRI	 				= "600000";
	public static final String 	JOB_EVENT			= "org/sodeac/eventdispatcher/itest/baseservicetest/jobevent";
	public static final String 	RESCHEDULE_EVENT1 	= "org/sodeac/eventdispatcher/itest/baseservicetest/rescheduleevent1";
	public static final String 	RESCHEDULE_EVENT2 	= "org/sodeac/eventdispatcher/itest/baseservicetest/rescheduleevent2";
	public static final String 	RESCHEDULE_EVENT3 	= "org/sodeac/eventdispatcher/itest/baseservicetest/rescheduleevent3";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@SuppressWarnings("unused")
	private volatile ComponentContext context = null;
	private CountDownLatch latch =  null;
	
	@Activate
	private void activate(ComponentContext context, Map<String, ?> properties)
	{
		this.latch = new CountDownLatch(1);
		this.context = context;
	}
	
	@Deactivate
	private void deactivate(ComponentContext context)
	{
		this.context = null;
	}
	
	public void onQueueAttach(IQueue queue)
	{
		super.onQueueAttach(queue);
		queue.getStatePropertyBlock().setProperty(EVENT_PROPERTY_LATCH, this.latch);
	}
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.queueEvent(BaseServiceTestController.QUEUE_ID, event);
	}
	
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
		event.getQueue().rescheduleTask(SERVICE_ID, System.currentTimeMillis(), -1, -1);
	}

	@Override
	public void run(IQueueTaskContext taskContext)
	{
		IQueue queue = taskContext.getQueue();
		
		List<IQueuedEvent> queueEventList = queue.getEventList(null, null, null);
		for(IQueuedEvent event : queueEventList)
		{
			queue.signal(event.getEvent().getTopic());
			queue.removeEvent(event.getUUID());
			if(event.getEvent().getTopic().equals(BaseServiceTestController.RESCHEDULE_EVENT3))
			{
				this.latch.countDown();
			}
		}
	}
}
