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
package org.sodeac.eventdispatcher.itest.components.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.base.AbstractBaseTestController;

@Component
(
	immediate=true,
	service={IEventController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+ScopeTestSimpleManagementController.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_CREATE,
		EventConstants.EVENT_TOPIC+"=" + ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST1,
		EventConstants.EVENT_TOPIC+"=" + ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST2,
		EventConstants.EVENT_TOPIC+"=" + ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_SIZE
	}
)
public class ScopeTestSimpleManagementController extends AbstractBaseTestController implements EventHandler,IEventController,IOnEventScheduled,IOnRemoveEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFireEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	public static final String QUEUE_ID 					= "scopetestsimplequeue";
	public static final String REQUEST_EVENT_SCOPE_CREATE	= "org/sodeac/eventdispatcher/itest/scopetestsimple/request/scopecreate";
	public static final String REQUEST_EVENT_SCOPE_REQUEST1	= "org/sodeac/eventdispatcher/itest/scopetestsimple/request/scoperequest1";
	public static final String REQUEST_EVENT_SCOPE_REQUEST2	= "org/sodeac/eventdispatcher/itest/scopetestsimple/request/scoperequest2";
	public static final String REQUEST_EVENT_SCOPE_SIZE		= "org/sodeac/eventdispatcher/itest/scopetestsimple/request/scopesize";
	
	public static final String EVENT_PROPERTY_SCOPEID		= "SCOPEID";
	public static final String EVENT_PROPERTY_WORKLATCH		= "WORKLATCH";
	public static final String SCOPE_TYPE					= "scopetestsimplequeuetype";
	public static final String SCOPE_SIGNAL_SCOPESIZE		= "SCOPE_SIZE_";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		dispatcher.schedule(event, ScopeTestSimpleManagementController.QUEUE_ID);
	}
	
	@Override
	public void onEventScheduled(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(REQUEST_EVENT_SCOPE_CREATE))
		{
			super.latch = (CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_LATCH);
			UUID scopeId = (UUID)event.getNativeEventProperties().get(EVENT_PROPERTY_SCOPEID);
			
			event.getQueue().signal(SCOPE_SIGNAL_SCOPESIZE + event.getQueue().getScopes().size());
			
			Map<String,Object> scopeConfiguration = new HashMap<String,Object>();
			scopeConfiguration.put(IEventDispatcher.PROPERTY_QUEUE_TYPE, SCOPE_TYPE);
			event.getQueue().createScope(scopeId, "TestScope", scopeConfiguration, null, false,false);
			
			event.getQueue().signal(SCOPE_SIGNAL_SCOPESIZE + event.getQueue().getScopes().size());
			
			((CountDownLatch)event.getNativeEventProperties().get(EVENT_PROPERTY_WORKLATCH)).countDown();
		}
		
		if(event.getEvent().getTopic().equals(REQUEST_EVENT_SCOPE_REQUEST1))
		{
			event.getQueue().getScope((UUID)event.getNativeEventProperties().get(EVENT_PROPERTY_SCOPEID)).scheduleEvent(event.getEvent());
		}
		
		if(event.getEvent().getTopic().equals(REQUEST_EVENT_SCOPE_REQUEST2))
		{
			event.getQueue().getScope((UUID)event.getNativeEventProperties().get(EVENT_PROPERTY_SCOPEID)).scheduleEvent(event.getEvent());
		}
		
		if(event.getEvent().getTopic().equals(REQUEST_EVENT_SCOPE_SIZE))
		{
			event.getQueue().signal(SCOPE_SIGNAL_SCOPESIZE + event.getQueue().getScopes().size());
		}
	}
}
