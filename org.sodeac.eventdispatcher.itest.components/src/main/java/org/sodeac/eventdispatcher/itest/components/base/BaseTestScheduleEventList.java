/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnScheduleEventList;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IEventController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+BaseTestScheduleEventList.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + BaseTestScheduleEventList.BUSY_EVENT,
		EventConstants.EVENT_TOPIC+"=" + BaseTestScheduleEventList.SCHEDULE_EVENT
	}
)
public class BaseTestScheduleEventList  extends AbstractBaseTestController implements EventHandler,IEventController,IOnScheduleEvent,IOnScheduleEventList,IOnRemoveEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFireEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
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
		dispatcher.schedule(BaseTestScheduleEventList.QUEUE_ID,event);
	}
	
	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(BUSY_EVENT))
		{
			super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
			IQueueJob job = new IQueueJob()
			{
				
				@Override
				public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl,List<IQueueJob> currentProcessedJobList)
				{
					try {Thread.sleep(BUSY_TIME);}catch (Exception e) {e.printStackTrace();}	
				}
				
				@Override
				public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}
			};
			
			event.getQueue().scheduleJob(job);
		}
	}

	@Override
	public void onScheduleEventList(IQueue queue, List<IQueuedEvent> eventList)
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
