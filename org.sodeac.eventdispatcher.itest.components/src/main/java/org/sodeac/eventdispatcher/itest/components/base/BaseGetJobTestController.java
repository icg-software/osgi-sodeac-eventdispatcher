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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.job.FireSyncEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IQueueController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+BaseGetJobTestController.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + BaseGetJobTestController.SCHEDULE_EVENT,
		EventConstants.EVENT_TOPIC+"=" + BaseGetJobTestController.GETJOB_EVENT
	}
)
public class BaseGetJobTestController extends AbstractBaseTestController implements EventHandler,IQueueController,IOnQueuedEvent,IOnRemovedEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFiredEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final int		DELAY			= 5000;	
	public static final String 	QUEUE_ID 		= "basegetjobtestqueue";
	public static final String 	JOB_ID 			= BaseGetJobTestController.class.getCanonicalName() + "Job";
	public static final String 	JOB_EVENT 		= "org/sodeac/eventdispatcher/itest/basegetjobtest/jobevent";
	public static final String 	SCHEDULE_EVENT 	= "org/sodeac/eventdispatcher/itest/basegetjobtest/scheduleevent";
	public static final String 	GETJOB_EVENT 	= "org/sodeac/eventdispatcher/itest/basegetjobtest/getjobevent";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.schedule(BaseGetJobTestController.QUEUE_ID, event);
	}
	
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(SCHEDULE_EVENT))
		{
			super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
			IQueueJob job = new FireSyncEvent(event,JOB_EVENT,event.getNativeEventProperties());
			super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
			event.getQueue().scheduleJob(JOB_ID,job,null,System.currentTimeMillis() + DELAY, -1,-1);

		}
		if(event.getEvent().getTopic().equals(GETJOB_EVENT))
		{
			IQueueJob job = event.getQueue().getJob(JOB_ID);
			if(job == null)
			{
				super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_SIGNAL,event.getQueue(),"JOB_NOT_FOUND"));
			}
			else
			{
				super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_SIGNAL,event.getQueue(),"JOB_FOUND"));
			}
		}
	}
}
