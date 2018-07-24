/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.components.scheduleresult;

import java.util.Map;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IScheduleResult;

@Component
(
	immediate=true,
	service={IEventController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+ScheduleResultTestController1.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + ScheduleResultTestController1.SCHEDULE_EVENT
	}
)
public class ScheduleResultTestController1 implements EventHandler, IEventController, IOnScheduleEvent
{
	public static final String QUEUE_ID 						= "scheduleresulttest1"	;
	public static final String SCHEDULE_EVENT 					= "org/sodeac/eventdispatcher/itest/metrics/scheduleresulttes1/run";
	public static final String PROPERTY_SCHEDULE_TIME			= "SCHEDULE_TIME";
	public static final String PROPERTY_WAIT_TIME				= "WAIT_TIME";
	public static final String PROPERTY_BRIDGE 					= "BRIDGE";
	public static final String PROPERTY_FUTURE					= "FUTURE";
	public static final String PROPERTY_SCHEDULE_DONE			= "SCHEDULE_DONE";
	public static final String PROPERTY_MANUAL_ADD_EXCEPTION	= "MANUAL_ADD_EXCEPTION";
	public static final String PROPERTY_THROWS_EXCEPTION		= "THROWS_EXCEPTION";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;

	@Override
	public void handleEvent(Event event)
	{
		try
		{
			Map<String,Object> bridge = (Map<String,Object>)event.getProperty(PROPERTY_BRIDGE);
			Future<IScheduleResult> resultFuture = dispatcher.schedule(QUEUE_ID, event);
			bridge.put(PROPERTY_FUTURE, resultFuture);
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		Long scheduleTime = (Long)event.getNativeEventProperties().get(PROPERTY_SCHEDULE_TIME);
		Boolean scheduleDone = (Boolean)event.getNativeEventProperties().get(PROPERTY_SCHEDULE_DONE);
		Exception manualAddException = (Exception)event.getNativeEventProperties().get(PROPERTY_MANUAL_ADD_EXCEPTION);
		Exception throwsException = (Exception)event.getNativeEventProperties().get(PROPERTY_THROWS_EXCEPTION);
		try
		{
			Thread.sleep(scheduleTime);
			if(manualAddException != null)
			{
				event.getScheduleResultObject().addError(manualAddException);
			}
			if((scheduleDone != null) && scheduleDone.booleanValue())
			{
				event.getScheduleResultObject().setScheduled();
			}
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		if(throwsException != null)
		{
			if(throwsException instanceof RuntimeException)
			{
				throw (RuntimeException)throwsException;
			}
			throw new RuntimeException(throwsException);
		}
	}

}
