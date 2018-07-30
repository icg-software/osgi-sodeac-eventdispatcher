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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.edservice.api.DiscoverEServiceRequest;
import org.sodeac.eventdispatcher.common.edservice.api.DiscoverEServiceResponse;
import org.sodeac.eventdispatcher.common.edservice.api.IEServiceDiscovery;
import org.sodeac.eventdispatcher.common.edservice.api.IEServiceReference;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.Requirement;
import org.sodeac.eventdispatcher.common.queueservice.ScopeSingleSyncCallServiceAdapter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

@Component
(
	service= {IEServiceDiscovery.class,IEventController.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+IEServiceDiscovery.QUEUE_ID,
		IEventDispatcher.PROPERTY_DISPATCHER_ID+"=" +IEventDispatcher.DEFAULT_DISPATCHER_ID,
		IEventController.PROPERTY_CONSUME_EVENT_TOPIC+"=" + IEServiceDiscovery.EVENT_TOPIC_RESPONSE,
	}
)
public class EServiceDiscovery implements IEServiceDiscovery,IEventController,IOnScheduleEvent,IOnQueueObserve, IOnQueueReverse
{
	
	private IQueue managementQueue = null;

	@Override
	public void onQueueObserve(IQueue queue)
	{
		if(queue.getDispatcher().getId().equals(IEventDispatcher.DEFAULT_DISPATCHER_ID) && queue.getId().equals(QUEUE_ID))
		{
			// cache and cacheloader
			LoadingCache<CacheableRequest, List<IEServiceReference>> serviceCache = CacheBuilder.newBuilder()
			.maximumSize(1080)
			.expireAfterWrite(7, TimeUnit.MINUTES)
			.build
			(
				new CacheLoader<CacheableRequest, List<IEServiceReference>> () 
				{
					@SuppressWarnings("unchecked")
					public List<IEServiceReference> load(CacheableRequest key) 
					{
						List<IEServiceReference> broadcastResult = discoverServiceBroadcast(key);
						return broadcastResult == null ? Collections.unmodifiableList(Collections.EMPTY_LIST): broadcastResult;
					}
				}
			);
			queue.getStatePropertyBlock().setAdapter(LoadingCache.class, serviceCache);
			
			// queue
			this.managementQueue = queue;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DiscoverEServiceResponse discoverService(DiscoverEServiceRequest discoverServerRequest)
	{
		if(managementQueue == null)
		{
			return null;
		}
		
		LoadingCache<CacheableRequest, List<IEServiceReference>> serviceCache = managementQueue.getStatePropertyBlock().getAdapter(LoadingCache.class);
		try
		{
			return new DiscoverEServiceResponse(serviceCache.get(new CacheableRequest(discoverServerRequest)));
		} 
		catch (ExecutionException e){}
		return null;
	}
	
	private List<IEServiceReference> discoverServiceBroadcast(CacheableRequest cacheableRequest)
	{
		UUID scopeId = UUID.randomUUID();
		ScopeSingleSyncCallServiceAdapter<List<IEServiceReference>> scopeCallAdapter = new ScopeSingleSyncCallServiceAdapter<>(7, TimeUnit.SECONDS);
		DiscoverEServiceRequest discoverServerRequest = cacheableRequest.getServiceRequest();
		
		managementQueue.createSessionScope
		(
			scopeId, 
			"discover service " + scopeId, 
			null, 
			ImmutableMap.<String, Object>builder()
				.put(DiscoverEServiceRequest.class.getCanonicalName(),discoverServerRequest)
				.put(EServiceDiscoveryScope.class.getCanonicalName(),true)
				.put(ScopeSingleSyncCallServiceAdapter.class.getCanonicalName(),scopeCallAdapter)
			.build(),
			null,
			false, 
			false
		);
		
		try
		{
			List<IEServiceReference> references = scopeCallAdapter.waitForResult
			(
				(discoverServerRequest.getTimeout() == null) ? -1 : discoverServerRequest.getTimeout().getTime(),
				(discoverServerRequest.getTimeout() == null) ? null : discoverServerRequest.getTimeout().getTimeUnit()
			);
			return references;
		}
		catch (TimeoutException e) {}
		catch (InterruptedException e) {}
		
		return null;
	}

	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		if(event.getQueue() != this.managementQueue)
		{
			return;
		}
		
		event.removeFromQueue();
		
		if(IEServiceDiscovery.EVENT_TOPIC_RESPONSE.equals(event.getEvent().getTopic()))
		{
			UUID scopeId = (UUID)event.getNativeEventProperties().get(IEServiceDiscovery.EVENT_PROPERTY_REQUEST_ID);
			if(scopeId == null)
			{
				return;
			}
			IQueueSessionScope scope = this.managementQueue.getSessionScope(scopeId);
			if(scope == null)
			{
				return;
			}
			scope.scheduleEvent(event.getEvent());
		}
		
	}
	
	@Override
	public void onQueueReverse(IQueue queue)
	{
		if(queue == this.managementQueue)
		{
			this.managementQueue = null;
		}
	}

	// key wrapper for DiscoverEServiceRequest
	private class CacheableRequest
	{
		private DiscoverEServiceRequest serviceRequest = null;
		private String hash = null;
		private byte[] digest;
		private CacheableRequest(DiscoverEServiceRequest serviceRequest)
		{
			super();
			this.serviceRequest = serviceRequest;
			this.createHash();
		}
		
		public DiscoverEServiceRequest getServiceRequest()
		{
			return serviceRequest;
		}
		
		private void createHash()
		{
			if(this.serviceRequest == null)
			{
				return;
			}
			try
			{
				MessageDigest md = MessageDigest.getInstance("MD5");
				
				List<Requirement> requirementList = this.serviceRequest.getRequirementList();
				if(requirementList != null)
				{
					md.update(("SIZE::" + requirementList.size()).getBytes());
					for(Requirement requirement : this.serviceRequest.getRequirementList())
					{
						md.update(("NAMESPACE::" + ( requirement.getNamespace() == null ? "" : requirement.getNamespace().trim()) + " _ ").getBytes());
						md.update(("EFECTIVE::" + requirement.getEffective()).getBytes());
						md.update(("OPTIONAL::" + Boolean.toString(requirement.isOptional())).getBytes());
						md.update(("FILTERTYPE::" + requirement.getFilterType()).getBytes());
						md.update(("FILTER::" + ( requirement.getFilterExpression() == null ? "" : requirement.getFilterExpression().trim()) + " _ ").getBytes());
					}
				}
				else
				{
					md.update("SIZE::NULL".getBytes());
				}
				this.digest = md.digest();
				StringBuilder sb = new StringBuilder();
				for ( byte b : this.digest )
				{
					sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
				}
				this.hash = sb.toString();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((hash == null) ? 0 : hash.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if(obj == null)
			{
				return false;
			}
			if(obj == this)
			{
				return true;
			}
			if(! (obj instanceof CacheableRequest))
			{
				return false;
			}
			if(this.hash == null)
			{
				return false;
			}
			return this.hash.equals(((CacheableRequest)obj).hash);
		}
	}

}
