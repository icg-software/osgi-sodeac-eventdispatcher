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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.common.queueservice.ScopeSingleSyncCallServiceAdapter;
import org.sodeac.eventdispatcher.common.reactiveservice.api.DiscoverReactiveServiceRequest;
import org.sodeac.eventdispatcher.common.reactiveservice.api.DiscoverReactiveServiceResponse;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceDiscovery;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceReference;
import org.sodeac.eventdispatcher.common.reactiveservice.api.wiring.Requirement;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;

@Component
(
	service= {IReactiveServiceDiscovery.class,IQueueController.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+IReactiveServiceDiscovery.QUEUE_ID,
		IEventDispatcher.PROPERTY_DISPATCHER_ID+"=" +IEventDispatcher.DEFAULT_DISPATCHER_ID,
		IQueueController.PROPERTY_CONSUME_EVENT_TOPIC+"=" + IReactiveServiceDiscovery.EVENT_TOPIC_RESPONSE,
	}
)
public class ReactiveServiceDiscovery implements IReactiveServiceDiscovery,IQueueController,IOnQueuedEvent,IOnQueueAttach, IOnQueueDetach
{
	
	private IQueue managementQueue = null;

	@Override
	public void onQueueAttach(IQueue queue)
	{
		if(queue.getDispatcher().getId().equals(IEventDispatcher.DEFAULT_DISPATCHER_ID) && queue.getId().equals(QUEUE_ID))
		{
			// cache and cacheloader
			LoadingCache<CacheableRequest, List<IReactiveServiceReference>> serviceCache = CacheBuilder.newBuilder()
			.maximumSize(1080)
			.expireAfterWrite(7, TimeUnit.MINUTES)
			.build
			(
				new CacheLoader<CacheableRequest, List<IReactiveServiceReference>> () 
				{
					@SuppressWarnings("unchecked")
					public List<IReactiveServiceReference> load(CacheableRequest key) 
					{
						List<IReactiveServiceReference> broadcastResult = discoverServiceBroadcast(key);
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
	public DiscoverReactiveServiceResponse discoverService(DiscoverReactiveServiceRequest discoverServerRequest)
	{
		if(managementQueue == null)
		{
			return null;
		}
		
		LoadingCache<CacheableRequest, List<IReactiveServiceReference>> serviceCache = managementQueue.getStatePropertyBlock().getAdapter(LoadingCache.class);
		try
		{
			return new DiscoverReactiveServiceResponse(serviceCache.get(new CacheableRequest(discoverServerRequest)));
		} 
		catch (ExecutionException e){}
		return null;
	}
	
	private List<IReactiveServiceReference> discoverServiceBroadcast(CacheableRequest cacheableRequest)
	{
		UUID scopeId = UUID.randomUUID();
		ScopeSingleSyncCallServiceAdapter<List<IReactiveServiceReference>> scopeCallAdapter = new ScopeSingleSyncCallServiceAdapter<>(7, TimeUnit.SECONDS);
		DiscoverReactiveServiceRequest discoverServerRequest = cacheableRequest.getServiceRequest();
		
		managementQueue.createSessionScope
		(
			scopeId, 
			"discover service " + scopeId, 
			null, 
			ImmutableMap.<String, Object>builder()
				.put(DiscoverReactiveServiceRequest.class.getCanonicalName(),discoverServerRequest)
				.put(ReactiveServiceDiscoveryScope.class.getCanonicalName(),true)
				.put(ScopeSingleSyncCallServiceAdapter.class.getCanonicalName(),scopeCallAdapter)
			.build(),
			null,
			false, 
			false
		);
		
		try
		{
			List<IReactiveServiceReference> references = scopeCallAdapter.waitForResult
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
	public void onQueuedEvent(IQueuedEvent event)
	{
		if(event.getQueue() != this.managementQueue)
		{
			return;
		}
		
		event.removeFromQueue();
		
		if(IReactiveServiceDiscovery.EVENT_TOPIC_RESPONSE.equals(event.getEvent().getTopic()))
		{
			UUID scopeId = (UUID)event.getNativeEventProperties().get(IReactiveServiceDiscovery.EVENT_PROPERTY_REQUEST_ID);
			if(scopeId == null)
			{
				return;
			}
			IQueueSessionScope scope = this.managementQueue.getSessionScope(scopeId);
			if(scope == null)
			{
				return;
			}
			scope.queueEvent(event.getEvent());
		}
		
	}
	
	@Override
	public void onQueueDetach(IQueue queue)
	{
		if(queue == this.managementQueue)
		{
			this.managementQueue = null;
		}
	}

	// key wrapper for DiscoverEServiceRequest // TODO move to RequestObject
	private class CacheableRequest
	{
		private DiscoverReactiveServiceRequest serviceRequest = null;
		private String hash = null;
		private byte[] digest;
		private CacheableRequest(DiscoverReactiveServiceRequest serviceRequest)
		{
			super();
			this.serviceRequest = serviceRequest;
			this.createHash();
		}
		
		public DiscoverReactiveServiceRequest getServiceRequest()
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
