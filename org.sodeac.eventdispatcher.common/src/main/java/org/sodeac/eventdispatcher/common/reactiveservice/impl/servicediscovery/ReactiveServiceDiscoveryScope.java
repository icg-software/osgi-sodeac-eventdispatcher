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
package org.sodeac.eventdispatcher.common.reactiveservice.impl.servicediscovery;

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
import org.sodeac.eventdispatcher.common.queueservice.ScopeSingleSyncCallServiceAdapter;
import org.sodeac.eventdispatcher.common.queueservice.ScopeTimeoutAdapter;
import org.sodeac.eventdispatcher.common.queueservice.TimeOutServiceAdapter;
import org.sodeac.eventdispatcher.common.reactiveservice.api.DiscoverReactiveServiceRequest;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceDiscovery;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceReference;
import org.sodeac.eventdispatcher.common.reactiveservice.api.TimeOut;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Component
(
	service=IEventController.class,
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"="+"(org.sodeac.eventdispatcher.common.reactiveservice.impl.servicediscovery.ReactiveServiceDiscoveryScope=true)"
	}
)
@EventQueueConfigurationFilter("(org.sodeac.eventdispatcher.common.reactiveservice.impl.servicediscovery.ReactiveServiceDiscoveryScope=true)")
public class ReactiveServiceDiscoveryScope implements IEventController,IOnQueueObserve, IOnScheduleEvent
{

	@Override
	public void onQueueObserve(IQueue queue)
	{
		// timeout for this scope
		TimeOut timeOut = queue.getConfigurationPropertyBlock().getAdapter(DiscoverReactiveServiceRequest.class).getTimeout();
		if((timeOut ==  null) || (timeOut.getTime() < 1)) {timeOut = new TimeOut(7, TimeUnit.SECONDS);}
		queue.getConfigurationPropertyBlock().setAdapter(TimeOutServiceAdapter.class, new ScopeTimeoutAdapter(timeOut.getTime(),timeOut.getTimeUnit()));
				
		// broadcast
		queue.postEvent
		(
			IReactiveServiceDiscovery.EVENT_TOPIC_REQUEST, ImmutableMap.<String, Object>builder()
			
				.put(IReactiveServiceDiscovery.EVENT_PROPERTY_REQUEST_ID,((IQueueSessionScope)queue).getScopeId())
				.put(IReactiveServiceDiscovery.EVENT_PROPERTY_REQUEST_OBJ,queue.getConfigurationPropertyBlock().getAdapter(DiscoverReactiveServiceRequest.class))
			
			.build()
		);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		event.removeFromQueue();
		
		if(IReactiveServiceDiscovery.EVENT_TOPIC_RESPONSE.equals(event.getEvent().getTopic()))
		{
			// process response
			
			ReactiveServiceReference serviceReference = (ReactiveServiceReference)event.getNativeEventProperties().get(IReactiveServiceDiscovery.EVENT_PROPERTY_RESPONSE_OBJ);
			if(serviceReference == null)
			{
				return;
			}
			UUID  scopeId = (UUID)event.getNativeEventProperties().get(IReactiveServiceDiscovery.EVENT_PROPERTY_REQUEST_ID);
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
				ImmutableList.<IReactiveServiceReference>builder().add(serviceReference).build()
			);
		}
	}

}
