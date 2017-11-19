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
package org.sodeac.eventdispatcher.itest.components.base;

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
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IOnEventScheduled;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IEventController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+BaseHeartbeatTimeoutTestController.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + BaseHeartbeatTimeoutTestController.SCHEDULE_EVENT
	}
)
public class BaseHeartbeatTimeoutTestController extends AbstractBaseTestController implements EventHandler,IEventController,IOnEventScheduled,IOnRemoveEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFireEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final String 	QUEUE_ID 			= "basehbtimeouttestqueue";
	public static final String 	JOB_EVENT 			= "org/sodeac/eventdispatcher/itest/basehbtimeouttest/jobevent";
	public static final String 	SCHEDULE_EVENT 		= "org/sodeac/eventdispatcher/itest/basehbtimeouttest/scheduleevent";
	public static final long 	HEARTBEAT_TIMEOUT	= 3500;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.schedule(event, BaseHeartbeatTimeoutTestController.QUEUE_ID);
	}
	
	@Override
	public void onEventScheduled(IQueuedEvent event)
	{
		super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
		IQueueJob job = new BaseHeartBeatTimeOutJob();
		super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
		event.getQueue().scheduleJob(null,job,null,-1, -1, HEARTBEAT_TIMEOUT);
	}
}
