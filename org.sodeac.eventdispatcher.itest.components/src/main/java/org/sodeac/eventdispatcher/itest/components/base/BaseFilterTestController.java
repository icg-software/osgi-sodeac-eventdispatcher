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

import java.util.Map;
import java.util.Map.Entry;
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
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;

@Component
(
	immediate=true,
	service={IQueueController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID + "=" + BaseFilterTestController.QUEUE_ID,
		EventConstants.EVENT_TOPIC + "=" + BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.ALL
	}
)
public class BaseFilterTestController extends AbstractBaseTestController implements EventHandler,IQueueController,IOnQueuedEvent,IOnRemovedEvent,IOnTaskDone,IOnTaskError,IOnTaskTimeout,IOnFiredEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final long 	SLEEP_VALUE			= 10800;
	public static final String 	QUEUE_ID 			= "basefiltertestqueue";
	public static final String 	PROPERTY_JOB_ID 	= "jobid";
	public static final String 	PROPERTY_QE_PROPS 	= "queueeventprops";
	public static final String 	PROPERTY_JOB_PROPS 	= "jobprops";
	public static final String 	PROPERTY_JOB	 	= "job";
	public static final String 	JOB_PROPERTY_EVENT 	= "origevent";
	public static final String 	SCHEDULE_EVENT 		= "org/sodeac/eventdispatcher/itest/basefiltertest/";
	
	public static final String	ALL					= "*";
	public static final String	EVENT1				= "e1";
	public static final String	EVENT2				= "e2";
	public static final String	EVENT3				= "e3";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.queueEvent(BaseFilterTestController.QUEUE_ID,event);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{	
		event.getQueue().getStatePropertyBlock().setProperty(event.getEvent().getTopic(),  event);
		
		Map<String,Object> eProps = (Map<String,Object>)event.getEvent().getProperty(PROPERTY_QE_PROPS);
		if(eProps != null)
		{
			for(Entry<String, Object> eEntry : eProps.entrySet())
			{
				event.setProperty(eEntry.getKey(), eEntry.getValue());
			}
		}
		
		IPropertyBlock jobProperties = dispatcher.createPropertyBlock();
		Map<String,Object> jProps = (Map<String,Object>)event.getEvent().getProperty(PROPERTY_JOB_PROPS);
		if(jProps != null)
		{
			for(Entry<String, Object> eEntry : jProps.entrySet())
			{
				jobProperties.setProperty(eEntry.getKey(), eEntry.getValue());
			}
		}
		jobProperties.setProperty(JOB_PROPERTY_EVENT, event.getEvent());
		
		super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
		IQueueTask job = new BaseTimeOutJob(SLEEP_VALUE);
		super.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_EVENT_SCHEDULED,event));
		event.getQueue().scheduleTask((String)event.getNativeEventProperties().get(PROPERTY_JOB_ID),job,jobProperties,System.currentTimeMillis() + 2000,-1, -1);
		
		event.setProperty(PROPERTY_JOB, job);
	}
}
