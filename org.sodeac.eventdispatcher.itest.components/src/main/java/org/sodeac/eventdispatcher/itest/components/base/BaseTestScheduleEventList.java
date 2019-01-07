/*******************************************************************************
 * Copyright (c) 2018, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.components.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnQueuedEventList;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.multichainlist.Snapshot;

@Component
(
	immediate=true,
	service={IQueueController.class,EventHandler.class},
	property=
	{
		EventDispatcherConstants.PROPERTY_QUEUE_ID+"="+BaseTestScheduleEventList.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + BaseTestScheduleEventList.BUSY_EVENT,
		EventConstants.EVENT_TOPIC+"=" + BaseTestScheduleEventList.SCHEDULE_EVENT
	}
)
public class BaseTestScheduleEventList  extends AbstractBaseTestController implements EventHandler,IQueueController,IOnQueuedEvent,IOnQueuedEventList,IOnRemovedEvent,IOnTaskDone,IOnTaskError,IOnTaskTimeout,IOnFiredEvent,IOnQueueAttach,IOnQueueDetach,IOnQueueSignal
{
	public static final String 	QUEUE_ID 			= "basetestscheduleeventlistqueue";
	public static final String 	JOB_EVENT	 		= "org/sodeac/eventdispatcher/itest/basetestscheduleeventlist/jobevent";
	public static final String 	BUSY_EVENT 			= "org/sodeac/eventdispatcher/itest/basetestscheduleeventlist/startbusyevents";
	public static final String 	SCHEDULE_EVENT 		= "org/sodeac/eventdispatcher/itest/basetestscheduleeventlist/scheduleevents";
	public static final Long 	BUSY_TIME 			= 7000L;
	public static final String 	SIGNAL_EVENT_SIZE	= "EVENT_SIZE_";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.queueEvent(BaseTestScheduleEventList.QUEUE_ID,event);
	}
	
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(BUSY_EVENT))
		{
			super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
			IQueueTask job = new IQueueTask()
			{
				
				@Override
				public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, ITaskControl taskControl,List<IQueueTask> currentProcessedJobList)
				{
					try {Thread.sleep(BUSY_TIME);}catch (Exception e) {e.printStackTrace();}	
				}
				
			};
			
			event.getQueue().scheduleTask(job);
		}
	}

	@Override
	public void onQueuedEventList(IQueue queue, Snapshot<IQueuedEvent> eventList)
	{
		List<IQueuedEvent> list = new ArrayList<IQueuedEvent>();
		for(IQueuedEvent event : eventList)
		{
			if(event.getEvent().getTopic().equals(SCHEDULE_EVENT))
			{
				list.add(event);
				super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
			}
		}
		if(list.size() > 0)
		{
			queue.signal(SIGNAL_EVENT_SIZE + list.size());
		}
		
	}
	
}
