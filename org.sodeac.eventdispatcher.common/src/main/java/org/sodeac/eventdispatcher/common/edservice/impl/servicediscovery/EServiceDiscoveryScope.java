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
package org.sodeac.eventdispatcher.common.edservice.impl.servicediscovery;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.annotation.EventQueueConfigurationFilter;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.edservice.api.DiscoverEServiceRequest;
import org.sodeac.eventdispatcher.common.edservice.api.IEServiceDiscovery;
import org.sodeac.eventdispatcher.common.edservice.api.IEServiceReference;
import org.sodeac.eventdispatcher.common.edservice.api.TimeOut;
import org.sodeac.eventdispatcher.common.queueservice.ScopeSingleSyncCallServiceAdapter;
import org.sodeac.eventdispatcher.common.queueservice.ScopeTimeoutAdapter;
import org.sodeac.eventdispatcher.common.queueservice.TimeOutServiceAdapter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Component
(
	service=IEventController.class,
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"="+"(org.sodeac.eventdispatcher.common.edservice.impl.servicediscovery.EServiceDiscoveryScope=true)"
	}
)
@EventQueueConfigurationFilter("org.sodeac.eventdispatcher.common.service.impl.servicediscovery.EServiceDiscoveryScope=true")
public class EServiceDiscoveryScope implements IEventController,IOnQueueObserve, IOnScheduleEvent
{

	@Override
	public void onQueueObserve(IQueue queue)
	{
		// timeout for this scope
		TimeOut timeOut = queue.getConfigurationPropertyBlock().getAdapter(DiscoverEServiceRequest.class).getTimeout();
		if((timeOut ==  null) || (timeOut.getTime() < 1)) {timeOut = new TimeOut(7, TimeUnit.SECONDS);}
		queue.getConfigurationPropertyBlock().setAdapter(TimeOutServiceAdapter.class, new ScopeTimeoutAdapter(timeOut.getTime(),timeOut.getTimeUnit()));
				
		// broadcast
		queue.postEvent
		(
			IEServiceDiscovery.EVENT_TOPIC_REQUEST, ImmutableMap.<String, Object>builder()
			
				.put(IEServiceDiscovery.EVENT_PROPERTY_REQUEST_ID,((IQueueSessionScope)queue).getScopeId())
				.put(IEServiceDiscovery.EVENT_PROPERTY_REQUEST_OBJ,queue.getConfigurationPropertyBlock().getAdapter(DiscoverEServiceRequest.class))
			
			.build()
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		event.removeFromQueue();
		
		if(IEServiceDiscovery.EVENT_TOPIC_RESPONSE.equals(event.getEvent().getTopic()))
		{
			// process response
			
			EServiceReference serviceReference = (EServiceReference)event.getNativeEventProperties().get(IEServiceDiscovery.EVENT_PROPERTY_RESPONSE_OBJ);
			if(serviceReference == null)
			{
				return;
			}
			UUID  scopeId = (UUID)event.getNativeEventProperties().get(IEServiceDiscovery.EVENT_PROPERTY_REQUEST_ID);
			if(scopeId == null)
			{
				return;
			}
			if(! scopeId.equals(((IQueueSessionScope)event.getQueue()).getScopeId()))
			{
				return;
			}
			
			event.getQueue().getConfigurationPropertyBlock().getAdapter(TimeOutServiceAdapter.class).heartBeat();
			event.getQueue().getConfigurationPropertyBlock().getAdapter(ScopeSingleSyncCallServiceAdapter.class).publishResult
			(
				ImmutableList.<IEServiceReference>builder().add(serviceReference).build()
			);
		}
	}

}
