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
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.job.FireSyncEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IQueueController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+BaseReScheduleTestController.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + BaseReScheduleTestController.SCHEDULE_EVENT,
		EventConstants.EVENT_TOPIC+"=" + BaseReScheduleTestController.RESCHEDULE_EVENT1,
		EventConstants.EVENT_TOPIC+"=" + BaseReScheduleTestController.RESCHEDULE_EVENT2
	}
)
public class BaseReScheduleTestController extends AbstractBaseTestController implements EventHandler,IQueueController,IOnQueuedEvent,IOnRemovedEvent,IOnTaskDone,IOnTaskError,IOnTaskTimeout,IOnFiredEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final String	JOB_ID				= BaseReScheduleTestController.class.getCanonicalName() + "Job";
	public static final int		DELAY				= 5000;
	public static final int		RESCHEDULE_DELAY	= 3000;
	public static final String 	QUEUE_ID 			= "baserescheduletestqueue";
	public static final String 	JOB_EVENT			= "org/sodeac/eventdispatcher/itest/baserescheduletest/jobevent";
	public static final String 	SCHEDULE_EVENT 		= "org/sodeac/eventdispatcher/itest/baserescheduletest/scheduleevent";
	public static final String 	RESCHEDULE_EVENT1 	= "org/sodeac/eventdispatcher/itest/baserescheduletest/rescheduleevent1";
	public static final String 	RESCHEDULE_EVENT2 	= "org/sodeac/eventdispatcher/itest/baserescheduletest/rescheduleevent2";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.queueEvent(BaseReScheduleTestController.QUEUE_ID,event);
	}
	
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(SCHEDULE_EVENT))
		{
			super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
			IQueueTask job = new FireSyncEvent(event,JOB_EVENT,event.getNativeEventProperties());
			
			super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
			event.getQueue().scheduleJob(JOB_ID,job,null,System.currentTimeMillis() + DELAY, -1,-1);
		}
		
		if(event.getEvent().getTopic().equals(RESCHEDULE_EVENT1))
		{
			event.getQueue().rescheduleJob(JOB_ID,System.currentTimeMillis() + DELAY, -1,-1);
		}
		
		if(event.getEvent().getTopic().equals(RESCHEDULE_EVENT2))
		{
			event.getQueue().rescheduleJob(JOB_ID,System.currentTimeMillis(), -1,-1);
		}
	}
}
