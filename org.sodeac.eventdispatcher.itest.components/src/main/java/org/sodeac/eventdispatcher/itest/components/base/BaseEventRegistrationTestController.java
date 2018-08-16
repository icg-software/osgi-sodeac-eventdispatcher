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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Component;
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
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.job.FireSyncEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IQueueController.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+BaseEventRegistrationTestController.QUEUE_ID,
		IQueueController.PROPERTY_CONSUME_EVENT_TOPIC +"=" + BaseEventRegistrationTestController.EVENT
	}
)
public class BaseEventRegistrationTestController extends AbstractBaseTestController implements IQueueController,IOnQueuedEvent,IOnRemovedEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFiredEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final String 	QUEUE_ID 		= "baseeventregistrationtestqueue";
	public static final String 	EVENT 			= "org/sodeac/eventdispatcher/itest/baseeventregistrationtest/jobevent";
	
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
		super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
		event.getQueue().scheduleJob(new FireSyncEvent(event,UUID.randomUUID().toString(),event.getNativeEventProperties()));
	}
}
