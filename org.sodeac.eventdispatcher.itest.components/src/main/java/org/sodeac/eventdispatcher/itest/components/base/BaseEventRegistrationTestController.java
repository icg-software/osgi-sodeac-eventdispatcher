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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Component;
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
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.job.FireSyncEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IEventController.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+BaseEventRegistrationTestController.QUEUE_ID,
		IEventController.PROPERTY_CONSUME_EVENT_TOPIC +"=" + BaseEventRegistrationTestController.EVENT
	}
)
public class BaseEventRegistrationTestController extends AbstractBaseTestController implements IEventController,IOnEventScheduled,IOnRemoveEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFireEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final String 	QUEUE_ID 		= "baseeventregistrationtestqueue";
	public static final String 	EVENT 			= "org/sodeac/eventdispatcher/itest/baseeventregistrationtest/jobevent";
	
	@Override
	public void onEventScheduled(IQueuedEvent event)
	{
		super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
		super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
		event.getQueue().scheduleJob(new FireSyncEvent(event,UUID.randomUUID().toString(),event.getNativeEventProperties()));
	}
}
