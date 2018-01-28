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
package org.sodeac.eventdispatcher.itest.components.rescheduleservice;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
import org.sodeac.eventdispatcher.api.IOnEventScheduled;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

@Component
(
	immediate=true,
	service={IEventController.class,EventHandler.class,IQueueService.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+ RescheduleTestService.QUEUE_ID,
		IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL+"=5000",
		IQueueService.PROPERTY_SERVICE_ID+"=" + RescheduleTestService.SERVICE_ID,
		EventConstants.EVENT_TOPIC+"=" + RescheduleTestService.SHARED_OBJECT_EVENT,
		EventConstants.EVENT_TOPIC+"=" + RescheduleTestService.DATA_VALUE_EVENT
	}
)
public class RescheduleTestService implements EventHandler,IEventController,IOnEventScheduled,IQueueService
{
	public static final String QUEUE_ID = "rescheduletestservicequeue";
	public static final String SERVICE_ID = "rescheduletestservice";
	public static final String SHARED_OBJECT_EVENT = "org/sodeac/eventdispatcher/itest/rescheduleservice/sharedobject";
	public static final String DATA_VALUE_EVENT = "org/sodeac/eventdispatcher/itest/rescheduleservice/datavalue";
	public static final String PROPERTY_DATA_OBJECT = "DATA_OBJECT"; 
	public static final String PROPERTY_DATA_VALUE = "DATA_VALUE"; 
	
	public static final long TOLERANCE = 100;
	
	private AtomicLong dataObject = null;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}
	
	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl, List<IQueueJob> currentProcessedJobList)
	{
		if( dataObject == null)
		{
			return;
		}
		
		List<IQueuedEvent> eventList  = queue.getEventList(null, null, null);
		if(eventList.isEmpty())
		{
			return;
		}
		
		IQueuedEvent lastQueuedEvent = eventList.get(eventList.size() -1);
		dataObject.set((Long)lastQueuedEvent.getNativeEventProperties().get(PROPERTY_DATA_VALUE));
		
		for(IQueuedEvent evt : eventList)
		{
			queue.removeEvent(evt.getUUID());
		}
	}

	@Override
	public void onEventScheduled(IQueuedEvent event)
	{
		List<IQueuedEvent> eventList  = event.getQueue().getEventList(null, null, null);
		if(eventList.isEmpty())
		{
			return;
		}
		
		IQueuedEvent lastQueuedEvent = eventList.get(eventList.size() -1);
		for(IQueuedEvent evt : eventList)
		{
			if(lastQueuedEvent != evt)
			{
				event.getQueue().removeEvent(evt.getUUID());
			}
		}
		if(lastQueuedEvent != event)
		{
			return;
		}
		
		event.getQueue().rescheduleJob(SERVICE_ID, System.currentTimeMillis(), -1, -1);
	}

	@Override
	public void handleEvent(Event event)
	{
		if(event.getTopic().equals(SHARED_OBJECT_EVENT))
		{
			this.dataObject = (AtomicLong) event.getProperty("DATA_OBJECT");
		}
		if(event.getTopic().equals(DATA_VALUE_EVENT))
		{
			dispatcher.schedule(event, QUEUE_ID);
		}
	}

}
