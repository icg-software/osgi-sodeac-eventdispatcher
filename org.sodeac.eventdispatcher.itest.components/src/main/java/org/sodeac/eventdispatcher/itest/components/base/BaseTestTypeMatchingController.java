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
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
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
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"=("+IEventDispatcher.PROPERTY_QUEUE_TYPE + "=" + BaseTestTypeMatchingController.QUEUE_TYPE + ")",
		EventConstants.EVENT_TOPIC+"=" + BaseTestTypeMatchingController.SCHEDULE_EVENT
	}
)
public class BaseTestTypeMatchingController extends AbstractBaseTestController implements EventHandler,IQueueController,IOnScheduleEvent,IOnRemoveEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFireEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final String QUEUE_ID 		= "basetypedcontrollerbinding";
	public static final String QUEUE_TYPE 		= "test17type";
	public static final String JOB_EVENT 		= "org/sodeac/eventdispatcher/itest/basetesttypematching/jobevent";
	public static final String SCHEDULE_EVENT 	= "org/sodeac/eventdispatcher/itest/basetesttypematching/scheduleevent";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.schedule(BaseTestTypeMatchingController.QUEUE_ID, event);
	}
	
	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
		IQueueJob job = new FireSyncEvent(event,JOB_EVENT,event.getNativeEventProperties());
		super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
		event.getQueue().scheduleJob(job);
	}
}
