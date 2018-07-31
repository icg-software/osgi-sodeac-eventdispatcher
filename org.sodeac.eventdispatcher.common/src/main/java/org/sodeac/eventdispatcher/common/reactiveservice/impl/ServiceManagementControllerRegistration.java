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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveService;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceRegistrationAdapter;
import org.sodeac.eventdispatcher.common.reactiveservice.impl.servicediscovery.ReactiveServiceRegistrationAdapter;

@Component(immediate=true,service=ServiceManagementControllerRegistration.class)
public class ServiceManagementControllerRegistration
{
	public ServiceManagementControllerRegistration()
	{
		super();
		
		this.writeLock = new ReentrantLock();
	}
	
	private Lock writeLock;
	
	protected volatile ComponentContext context = null;
	
	private List<EventDrivenServiceRegistration> services = new ArrayList<EventDrivenServiceRegistration>();
	private List<EventDrivenServiceRegistration> earlyBirds = new ArrayList<EventDrivenServiceRegistration>();
	
	@Activate
	private void activate(ComponentContext context, Map<String, ?> properties)
	{
		
		writeLock.lock();
		try
		{
			this.context = context;
		}
		finally 
		{
			writeLock.unlock();
		}
		
		for(EventDrivenServiceRegistration serviceReg : earlyBirds)
		{
			bindService(serviceReg.service, serviceReg.serviceServiceReference);
		}
		
		earlyBirds.clear();
	}
	
	@Deactivate
	private void deactivate(ComponentContext context)
	{
		this.context = null;
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindService(IReactiveService service, ServiceReference<IReactiveService> serviceReference)
	{
		if(serviceReference.getProperty(IReactiveService.PROPERTY_SERVICE_QUEUE_ID) == null)
		{
			return;
		}
		
		String serviceQueueId = serviceReference.getProperty(IReactiveService.PROPERTY_SERVICE_QUEUE_ID).toString();
		
		if(serviceQueueId.isEmpty())
		{
			return;
		}
		
		if(this.context == null)
		{
			writeLock.lock();
			try
			{
				if(this.context == null)
				{
					EventDrivenServiceRegistration registration = new EventDrivenServiceRegistration();
					registration.service = service;
					registration.serviceServiceReference = serviceReference;
					
					earlyBirds.add(registration);
					
					return;
				}
			}
			finally 
			{
				writeLock.unlock();
			}
			
		}
		
		writeLock.lock();
		try
		{
			for(EventDrivenServiceRegistration serviceRegistration : this.services)
			{
				if(serviceRegistration.service == service)
				{
					return;
				}
			}
			
			EventDrivenServiceRegistration registration = new EventDrivenServiceRegistration();
			registration.service = service;
			registration.serviceQueueId = serviceQueueId;
			registration.serviceServiceReference = serviceReference;
			registration.registrationController = new RegistrationController(registration);
			
			Dictionary<String, Object> properties = new Hashtable<String, Object>();
			properties.put(IEventDispatcher.PROPERTY_QUEUE_ID, registration.serviceQueueId);
			registration.controllerServiceRegistration = this.context.getBundleContext().registerService(IEventController.class, registration.registrationController, properties);
			
			this.services.add(registration);
			
		}
		finally 
		{
			writeLock.unlock();
		}
		
	}
	public void unbindService(IReactiveService service, ServiceReference<IReactiveService> serviceReference)
	{
		writeLock.lock();
		List<EventDrivenServiceRegistration> toUnregisterList = new ArrayList<EventDrivenServiceRegistration>();
		try
		{
			for(EventDrivenServiceRegistration serviceRegistration : this.services)
			{
				if(serviceRegistration.service == service)
				{
					toUnregisterList.add(serviceRegistration);
				}
			}
			
			for(EventDrivenServiceRegistration toUnregister : toUnregisterList)
			{
				this.services.remove(toUnregister);
			}
			
			for(EventDrivenServiceRegistration toUnregister : toUnregisterList)
			{
				try
				{
					toUnregister.controllerServiceRegistration.unregister();
				}
				catch (Exception e) {}
			}		
		}
		finally 
		{
			writeLock.unlock();
		}
	}
	
	private class EventDrivenServiceRegistration
	{
		private ServiceRegistration<IEventController> controllerServiceRegistration;
		private RegistrationController registrationController ;
		private ServiceReference<IReactiveService> serviceServiceReference;
		private IReactiveService service;
		private String serviceQueueId;
		private Dictionary<String, Object> serviceProperties;
	}
	
	private class RegistrationController implements IEventController, IOnQueueObserve, IOnQueueReverse
	{
		private EventDrivenServiceRegistration eventDrivenServiceRegistration;
		private RegistrationController (EventDrivenServiceRegistration eventDrivenServiceRegistration)
		{
			super();
			this.eventDrivenServiceRegistration = eventDrivenServiceRegistration;
			this.eventDrivenServiceRegistration.registrationController = this;
		}

		@Override
		public void onQueueReverse(IQueue queue)
		{
			// TODO Reverse not synchronized, Problem?
			IReactiveServiceRegistrationAdapter serviceRegistrationAdapter = queue.getConfigurationPropertyBlock().getAdapter(IReactiveServiceRegistrationAdapter.class);
			if(serviceRegistrationAdapter == null)
			{
				return;
			}
			serviceRegistrationAdapter.unregister(eventDrivenServiceRegistration.service, this.eventDrivenServiceRegistration.serviceProperties, this.eventDrivenServiceRegistration.serviceServiceReference.getBundle());
			// Signal is synchronized
			queue.signal(IReactiveServiceRegistrationAdapter.SIGNAL_REGISTRATION_UPDATE);
		}

		@Override
		public void onQueueObserve(IQueue queue)
		{
			IReactiveServiceRegistrationAdapter serviceRegistrationAdapter = queue.getConfigurationPropertyBlock().getAdapter(IReactiveServiceRegistrationAdapter.class);
			if(serviceRegistrationAdapter == null)
			{
				serviceRegistrationAdapter = new ReactiveServiceRegistrationAdapter(queue);
				queue.getConfigurationPropertyBlock().setAdapter(IReactiveServiceRegistrationAdapter.class,serviceRegistrationAdapter);
			}
			Dictionary<String, Object> properties = new Hashtable<String, Object>();
			for(String propertyKey : this.eventDrivenServiceRegistration.serviceServiceReference.getPropertyKeys())
			{
				properties.put(propertyKey, this.eventDrivenServiceRegistration.serviceServiceReference.getProperty(propertyKey));
			}
			this.eventDrivenServiceRegistration.serviceProperties = properties;
			serviceRegistrationAdapter.register(eventDrivenServiceRegistration.service, this.eventDrivenServiceRegistration.serviceProperties, this.eventDrivenServiceRegistration.serviceServiceReference.getBundle());
			queue.signal(IReactiveServiceRegistrationAdapter.SIGNAL_REGISTRATION_UPDATE);
		}
		
	}
}
