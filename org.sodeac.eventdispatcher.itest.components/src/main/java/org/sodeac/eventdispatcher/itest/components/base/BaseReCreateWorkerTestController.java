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
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;

@Component
(
	immediate=true,
	service={IQueueController.class,EventHandler.class},
	property=
	{
		EventDispatcherConstants.PROPERTY_QUEUE_ID+"="+BaseReCreateWorkerTestController.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + BaseReCreateWorkerTestController.SCHEDULE_EVENT
	}
)
public class BaseReCreateWorkerTestController extends AbstractBaseTestController implements EventHandler,IQueueController,IOnQueuedEvent,IOnRemovedEvent,IOnTaskDone,IOnTaskError,IOnTaskTimeout,IOnFiredEvent,IOnQueueAttach,IOnQueueDetach,IOnQueueSignal
{
	public static final long 	SLEEP_VALUE				= 10800;
	public static final long 	TIMEOUT_VALUE			= SLEEP_VALUE / 2;
	public static final String 	EVENT_PROPERTY_SIGNAL	= "SIGNAL";	
	public static final String 	QUEUE_ID 				= "baserecreateworkertestqueue";
	public static final String 	JOB_EVENT 				= "org/sodeac/eventdispatcher/itest/baserecreateworkertest/jobevent";
	public static final String 	SCHEDULE_EVENT 			= "org/sodeac/eventdispatcher/itest/baserecreateworkertest/scheduleevent";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.queueEvent(BaseReCreateWorkerTestController.QUEUE_ID,event);
	}
	
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
		String signal = (String)event.getNativeEventProperties().get(EVENT_PROPERTY_SIGNAL);
		IQueueTask job = new BaseRecreateTestJob(SLEEP_VALUE,signal);
		IPropertyBlock jobProperties = event.getQueue().getDispatcher().createPropertyBlock();
		jobProperties.setProperty(TracingObject.class.getName(), super.tracingObject);
		super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
		event.getQueue().scheduleTask(null,job,jobProperties,-1,TIMEOUT_VALUE, -1);
	}
}
