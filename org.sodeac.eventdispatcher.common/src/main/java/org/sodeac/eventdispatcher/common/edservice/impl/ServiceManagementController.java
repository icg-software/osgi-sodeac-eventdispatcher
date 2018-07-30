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
package org.sodeac.eventdispatcher.common.edservice.impl;

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
import org.sodeac.eventdispatcher.common.edservice.api.DiscoverEServiceRequest;
import org.sodeac.eventdispatcher.common.edservice.api.IEServiceDiscovery;
import org.sodeac.eventdispatcher.common.edservice.api.IEServiceReference;
import org.sodeac.eventdispatcher.common.edservice.api.IServiceRegistrationAdapter;

import com.google.common.collect.ImmutableMap;

@Component
(
	service= {IEventController.class},
	property=
	{
		IServiceRegistrationAdapter.SERVICE_PROPERTY__MATCH_QUEUE,
		IEServiceDiscovery.SERVICE_PROPERTY__CONSUME_EVENTS_DISCOVER_SERVICE,
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
		if(IServiceRegistrationAdapter.SIGNAL_REGISTRATION_UPDATE.equals(signal))
		{
			IServiceRegistrationAdapter registration = queue.getConfigurationPropertyBlock().getAdapter(IServiceRegistrationAdapter.class);
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
		if(IEServiceDiscovery.EVENT_TOPIC_REQUEST.equals(event.getEvent().getTopic()))
		{
			DiscoverEServiceRequest discoverServerRequest = (DiscoverEServiceRequest)event.getNativeEventProperties().get("service.discovery.request.object");
			if(discoverServerRequest == null)
			{
				return;
			}
			UUID requestId = (UUID)event.getNativeEventProperties().get("service.discovery.request.id");
			if(requestId == null)
			{
				return;
			}
			
			IServiceRegistrationAdapter serviceRegistration = event.getQueue().getConfigurationPropertyBlock().getAdapter(IServiceRegistrationAdapter.class);
			if(serviceRegistration == null)
			{
				return;
			}
	
			List<IEServiceReference> referenceList = serviceRegistration.discoverServices(discoverServerRequest);
			if(referenceList != null)
			{
				for(IEServiceReference serviceReference : referenceList)
				{
					event.getQueue().sendEvent
					(
						IEServiceDiscovery.EVENT_TOPIC_RESPONSE, ImmutableMap.<String, Object>builder()
						
							.put(IEServiceDiscovery.EVENT_PROPERTY_REQUEST_ID,requestId)
							.put(IEServiceDiscovery.EVENT_PROPERTY_RESPONSE_OBJ,serviceReference)
						
						.build()
					);
				}
			}
		}
		
		event.removeFromQueue();
	}

}
