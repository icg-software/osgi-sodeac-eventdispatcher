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
package org.sodeac.eventdispatcher.common.reactiveservice.impl;

import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.reactiveservice.api.DiscoverReactiveServiceRequest;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceDiscovery;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceReference;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceRegistrationAdapter;

import com.google.common.collect.ImmutableMap;

@Component
(
	service= {IEventController.class},
	property=
	{
		IReactiveServiceRegistrationAdapter.SERVICE_PROPERTY__MATCH_QUEUE,
		IReactiveServiceDiscovery.SERVICE_PROPERTY__CONSUME_EVENTS_DISCOVER_SERVICE,
	}
)
public class ServiceManagementController implements IEventController, IOnQueueObserve, IOnQueueReverse, IOnQueueSignal, IOnScheduleEvent
{
	@Override
	public void onQueueObserve(IQueue queue)
	{
	}

	@Override
	public void onQueueReverse(IQueue queue)
	{
	}

	@Override
	public void onQueueSignal(IQueue queue, String signal)
	{
		if(IReactiveServiceRegistrationAdapter.SIGNAL_REGISTRATION_UPDATE.equals(signal))
		{
			IReactiveServiceRegistrationAdapter registration = queue.getConfigurationPropertyBlock().getAdapter(IReactiveServiceRegistrationAdapter.class);
			if(registration == null)
			{
				return;
			}
			registration.updateRegistrations();
		}
	}
	
	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		if(IReactiveServiceDiscovery.EVENT_TOPIC_REQUEST.equals(event.getEvent().getTopic()))
		{
			DiscoverReactiveServiceRequest discoverServerRequest = (DiscoverReactiveServiceRequest)event.getNativeEventProperties().get(IReactiveServiceDiscovery.EVENT_PROPERTY_REQUEST_OBJ);
			if(discoverServerRequest == null)
			{
				return;
			}
			UUID requestId = (UUID)event.getNativeEventProperties().get(IReactiveServiceDiscovery.EVENT_PROPERTY_REQUEST_ID);
			if(requestId == null)
			{
				return;
			}
			
			IReactiveServiceRegistrationAdapter serviceRegistration = event.getQueue().getConfigurationPropertyBlock().getAdapter(IReactiveServiceRegistrationAdapter.class);
			if(serviceRegistration == null)
			{
				return;
			}
	
			List<IReactiveServiceReference> referenceList = serviceRegistration.discoverServices(discoverServerRequest);
			if(referenceList != null)
			{
				for(IReactiveServiceReference serviceReference : referenceList)
				{
					event.getQueue().sendEvent
					(
						IReactiveServiceDiscovery.EVENT_TOPIC_RESPONSE, ImmutableMap.<String, Object>builder()
						
							.put(IReactiveServiceDiscovery.EVENT_PROPERTY_REQUEST_ID,requestId)
							.put(IReactiveServiceDiscovery.EVENT_PROPERTY_RESPONSE_OBJ,serviceReference)
						
						.build()
					);
				}
			}
		}
		
		event.removeFromQueue();
	}

}
