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
package org.sodeac.eventdispatcher.common.service.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Bundle;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.common.service.api.IEventDrivenService;
import org.sodeac.eventdispatcher.common.service.api.IServiceRegistrationAdapter;

public class ServiceRegistrationAdapter implements IServiceRegistrationAdapter
{
	public ServiceRegistrationAdapter(IQueue queue)
	{
		super();
		this.serviceContainerLock = new ReentrantReadWriteLock();
		this.serviceContainerReadLock = this.serviceContainerLock.readLock();
		this.serviceContainerWriteLock = this.serviceContainerLock.writeLock();
		this.queue = queue;
	}
	
	private ReentrantReadWriteLock serviceContainerLock = null;
	private ReadLock serviceContainerReadLock = null;
	private WriteLock serviceContainerWriteLock = null;
	
	private IQueue queue = null;
	private List<ServiceContainer> serviceContainerList = new ArrayList<ServiceContainer>();
	
	@Override
	public void register(IEventDrivenService service, Dictionary<String, ?> properties, Bundle bundle)
	{
		serviceContainerWriteLock.lock();
		try
		{
			for(ServiceContainer serviceContainer : serviceContainerList)
			{
				if(serviceContainer.service == service)
				{
					return;
				}
			}

			ServiceContainer serviceContainer = new ServiceContainer();
			serviceContainer.service = service;
			serviceContainer.properties = properties;
			serviceContainer.bundle = bundle;
			
			this.serviceContainerList.add(serviceContainer);
			
		}
		finally 
		{
			serviceContainerWriteLock.unlock();
		}
	}

	@Override
	public void unregister(IEventDrivenService service, Dictionary<String, ?> properties, Bundle bundle)
	{
		serviceContainerWriteLock.lock();
		try
		{
			List<ServiceContainer> toRemoveList = new ArrayList<ServiceContainer>();
			for(ServiceContainer serviceContainer : serviceContainerList)
			{
				if(serviceContainer.service == service)
				{
					toRemoveList.add(serviceContainer);
				}
			}
			
			for(ServiceContainer toRemove : toRemoveList)
			{
				this.serviceContainerList.remove(toRemove);
			}
			
		}
		finally 
		{
			serviceContainerWriteLock.unlock();
		}
	}

	@Override
	public boolean isEmpty()
	{
		serviceContainerReadLock.lock();
		try
		{
			return this.serviceContainerList.isEmpty();
		}
		finally 
		{
			serviceContainerReadLock.unlock();
		}
	}
	
	@Override
	public void updateRegistrations()
	{
		IServiceRegistrationAdapter registration = queue.getConfigurationPropertyBlock().getAdapter(IServiceRegistrationAdapter.class);
		if(registration == null)
		{
			return;
		}
		if(registration.isEmpty())
		{
			queue.getConfigurationPropertyBlock().removeAdapter(IServiceRegistrationAdapter.class);
		}
	}
	
	private class ServiceContainer
	{
		private IEventDrivenService service;
		private Dictionary<String, ?> properties;
		private Bundle bundle;
	}
	
}
