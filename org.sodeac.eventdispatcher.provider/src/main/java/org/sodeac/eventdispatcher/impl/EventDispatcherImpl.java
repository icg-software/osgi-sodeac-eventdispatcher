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
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.ICounter;
import org.sodeac.eventdispatcher.api.IEventController;

import com.codahale.metrics.MetricRegistry;

@Component(name="EventDispatcherProvider" ,service=IEventDispatcher.class,immediate=true)
public class EventDispatcherImpl implements IEventDispatcher
{
	private Map<String,QueueImpl> queueIndex = new HashMap<String,QueueImpl>();
	private ReentrantReadWriteLock queueIndexLock;
	private ReadLock queueIndexReadLock;
	private WriteLock queueIndexWriteLock;
	private DispatcherGuardian dispatcherGuardian;
	private List<ControllerContainer> controllerList = null;
	private List<ServiceContainer> serviceList = null;
	
	private volatile ComponentContext context = null;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile LogService logService = null;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile EventAdmin eventAdmin;
	
	protected volatile MetricRegistry metricRegistry = new  MetricRegistry();
	private List<ServiceRegistration<?>> registrationList = new ArrayList<ServiceRegistration<?>>();
	
	private volatile IMetrics metrics = null; 
	
	// TODO replace synchronized (controllerList/serviceList) by locks ?
	
	public EventDispatcherImpl()
	{
		super();
		this.queueIndexLock = new ReentrantReadWriteLock(true);
		this.queueIndexReadLock = this.queueIndexLock.readLock();
		this.queueIndexWriteLock = this.queueIndexLock.writeLock();
		this.controllerList = new ArrayList<ControllerContainer>();
		this.serviceList = new ArrayList<ServiceContainer>();
		this.metrics = new MetricImpl(this, new PropertyBlockImpl());
	}
	
	@Override
	public boolean schedule(Event event, String queueId)
	{
		
		QueueImpl queue = null;
		this.queueIndexReadLock.lock();
		try
		{
			queue = this.queueIndex.get(queueId);
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		if(queue == null)
		{
			// TODO oneTime - Log
			if(this.logService != null)
			{
				this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "Queue is missing " + queueId);
			}
			else
			{
				System.err.println("Queue is missing " + queueId);
			}
			return false; 
		}
		
		queue.scheduleEvent(event);
		
		
		return true;
	}
	
	
	@Override
	public List<String> getQueueIdList()
	{
		List<String> queueIdList = new ArrayList<>();
		this.queueIndexReadLock.lock();
		try
		{
			for(String id :  this.queueIndex.keySet())
			{
				queueIdList.add(id);
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		return Collections.unmodifiableList(queueIdList);
	}

	@Override
	public IQueue getQueue(String queueId)
	{
		this.queueIndexReadLock.lock();
		try
		{
			return this.queueIndex.get(queueId);
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
	}
	
	public void registerTimeOut(QueueImpl queue, JobContainer job)
	{
		this.dispatcherGuardian.registerTimeOut(queue,job);
	}
	
	public void unregisterTimeOut(QueueImpl queue, JobContainer job)
	{
		this.dispatcherGuardian.unregisterTimeOut(queue,job);
	}
	
	private ICounter counterQueueSize;
	private ICounter counterConfigurationSize;
	
	@Activate
	private void activate(ComponentContext context, Map<String, ?> properties)
	{
		this.context = context;
		
		this.queueIndexReadLock.lock();
		try
		{
			counterQueueSize = metrics.counter("Queues");
			if(! this.queueIndex.isEmpty())
			{
				counterQueueSize.inc(this.queueIndex.size());
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		synchronized (this.controllerList)
		{
			counterConfigurationSize = metrics.counter( "ControllerRegistrations");
			
			if(! this.controllerList.isEmpty())
			{
				counterConfigurationSize.inc(this.controllerList.size());
			}
		}
		this.dispatcherGuardian = new DispatcherGuardian(this);
		this.dispatcherGuardian.start();
		
		List<ControllerContainer> controllerContainerCopy = null;
		synchronized (this.controllerList)
		{
			controllerContainerCopy = new ArrayList<ControllerContainer>(this.controllerList);
		}
		for(ControllerContainer controllerContainer : controllerContainerCopy)
		{
			try
			{
				this.registerEventController(controllerContainer.getEventController(), controllerContainer.getProperties());
			}
			catch (Exception e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Exception on register event controller", e);
				}
				else
				{
					System.err.println("Exception on register event controller " + e.getMessage());
				}
			}
			catch (Error e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Error on register event controller", e);
				}
				else
				{
					System.err.println("Error on register event controller " + e.getMessage());
				}
			}
		}
		
		Dictionary<String, Object> metricRegistryProperties = new Hashtable<>();
		metricRegistryProperties.put(Constants.SERVICE_DESCRIPTION, "Sodeac Metrics Registry");
		metricRegistryProperties.put(Constants.SERVICE_VENDOR, "Sodeac Framework");
		metricRegistryProperties.put("name", "sodeac"); // analog sling metrics
		registrationList.add(context.getBundleContext().registerService(MetricRegistry.class.getName(), metricRegistry, metricRegistryProperties));
		
	}
	
	@Deactivate
	private void deactivate(ComponentContext context)
	{
		for (ServiceRegistration<?> reg : registrationList) 
		{
            reg.unregister();
		}
		
		List<ControllerContainer> controllerContainerCopy = null;
		synchronized (this.controllerList)
		{
			controllerContainerCopy = new ArrayList<ControllerContainer>(this.controllerList);
		}
		for(ControllerContainer controllerContainer : controllerContainerCopy)
		{
			try
			{
				this.unregisterEventController(controllerContainer.getEventController());
			}
			catch (Exception e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Exception on unregister event controller", e);
				}
				else
				{
					System.err.println("Exception on unregister event controller " + e.getMessage());
				}
			}
			catch (Error e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Error on Register Event Configuration", e);
				}
				else
				{
					System.err.println("Error on unregister event controller " + e.getMessage());
				}
			}
		}
		
		this.context = null;
		this.queueIndexReadLock.lock();
		try
		{
			for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet() )
			{
				try
				{
					entry.getValue().dispose();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				
				try
				{
					entry.getValue().stopQueueWorker();
				}
				catch (Exception e) {}
				
				try
				{
					if(entry.getValue().getMetrics() != null)
					{
						((MetricImpl)entry.getValue().getMetrics()).dispose();
					}
				}
				catch (Exception e) {}
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		this.queueIndexWriteLock.lock();
		try
		{
			if(this.counterQueueSize != null)
			{
				counterQueueSize.dec(this.queueIndex.size());
				this.counterQueueSize = null;
			}
			this.queueIndex.clear();
		}
		finally 
		{
			this.queueIndexWriteLock.unlock();
		}
		
		try
		{
			this.dispatcherGuardian.stopWatchDog();
		}
		catch (Exception e) {}
		
		counterQueueSize = null;
		counterConfigurationSize = null;
		try
		{
			((MetricImpl)this.metrics).dispose();
		}
		catch (Exception e) {e.printStackTrace();}
	}

	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindEventController(IEventController eventController,Map<String, ?> properties)
	{
		synchronized (this.controllerList)
		{
			ControllerContainer controllerContainer = new ControllerContainer();
			controllerContainer.setEventController(eventController);
			controllerContainer.setProperties(properties);
			
			this.controllerList.add(controllerContainer);
			
			if(this.counterConfigurationSize != null)
			{
				this.counterConfigurationSize.inc();
			}
		}
		
		if(this.context != null)
		{
			registerEventController(eventController, properties);
		}
	}
	
	private boolean registerEventController(IEventController eventController,Map<String, ?> properties)
	{
		if(this.context == null)
		{
			return false;
		}
		
		String queueId = (String)properties.get(IEventDispatcher.PROPERTY_QUEUE_ID);
		if(queueId == null)
		{
			if(this.logService != null)
			{
				this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "Missing QueueId (Null)");
			}
			else
			{
				System.err.println("Missing QueueId (Null)");
			}
			return false;
		}
		if(queueId.isEmpty())
		{
			if(this.logService != null)
			{
				this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "Missing QueueId (Empty)");
			}
			else
			{
				System.err.println("Missing QueueId (Empty)");
			}
			return false;
		}
		QueueImpl queue = null;
		this.queueIndexReadLock.lock();
		try
		{
			queue = this.queueIndex.get(queueId);
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		
		if(queue == null)
		{
			queue = new QueueImpl(queueId,this);
			this.queueIndexWriteLock.lock();
			try
			{
				this.queueIndex.put(queueId,queue );
				if(this.counterQueueSize != null)
				{
					this.counterQueueSize.inc();
				}
			}
			finally 
			{
				this.queueIndexWriteLock.unlock();
			}
			
			this.queueIndexReadLock.lock();
			try
			{
				synchronized(this.serviceList)
				{
					for(ServiceContainer serviceContainer :  this.serviceList )
					{
						if(! queueId.equals(serviceContainer.getProperties().get(IEventDispatcher.PROPERTY_QUEUE_ID)))
						{
							continue;
						}
						
						queue.addService(serviceContainer.getQueueService(), serviceContainer.getProperties());
					}
				}
			}
			finally 
			{
				this.queueIndexReadLock.unlock();
			}
		}
		
		queue.addConfiguration(eventController, properties);
		if(eventController instanceof IOnQueueObserve)
		{
			// TODO move to worker
			try
			{
				((IOnQueueObserve)eventController).onQueueObserve( queue);
			}
			catch (Exception e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Exception on on-create() event controller", e);
				}
				else
				{
					System.err.println("Exception on onQueueObserve() event controller " + e.getMessage());
				}
			}
		}
		
		return true;
	}
	
	public void unbindEventController(IEventController eventController,Map<String, ?> properties)
	{
		synchronized (this.controllerList)
		{
			List<ControllerContainer> removeList = null;
			for(ControllerContainer controllerContainer : this.controllerList)
			{
				if(controllerContainer.getEventController() == eventController)
				{
					if(removeList ==  null)
					{
						removeList = new ArrayList<ControllerContainer>();
					}
					removeList.add(controllerContainer);
				}
			}
			if(removeList != null)
			{
				for(ControllerContainer controllerContainer : removeList)
				{
					this.controllerList.remove(controllerContainer);
					
					if(this.counterConfigurationSize != null)
					{
						this.counterConfigurationSize.dec();
					}
				}
			}
		}
		if(this.context != null)
		{
			this.unregisterEventController(eventController);
		}
	}
	
	private boolean unregisterEventController(IEventController eventQueueConfiguration)
	{
		if(this.context == null)
		{
			return false;
		}
		
		boolean registered = false;
		List<QueueImpl> registeredOnQueueList = null;
		List<QueueImpl> removeList = null;
		this.queueIndexReadLock.lock();
		try
		{
			for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet() )
			{
				if(entry.getValue().removeConfiguration(eventQueueConfiguration))
				{
					if(registeredOnQueueList == null)
					{
						registeredOnQueueList = new ArrayList<QueueImpl>();
					}
					registered = true;
					registeredOnQueueList.add(entry.getValue());
				}
				
				if(entry.getValue().getConfigurationSize() == 0)
				{
					if(removeList == null)
					{
						removeList = new ArrayList<QueueImpl>();
					}
					removeList.add(entry.getValue());
				}
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		if(eventQueueConfiguration instanceof IOnQueueReverse)
		{
			try
			{
				if(registeredOnQueueList != null)
				{
					for(QueueImpl queue : registeredOnQueueList)
					{	
						((IOnQueueReverse)eventQueueConfiguration).onQueueReverse(queue);
					}
				}
			}
			catch (Exception e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Exception on onQueueReverse() event controller", e);
				}
				else
				{
					System.err.println("Exception on on-close() event controller " + e.getMessage());
				}
			}
		}
		
		if(removeList != null)
		{
			this.queueIndexWriteLock.lock();
			try
			{
				for(QueueImpl queue : removeList)
				{
					try
					{
						if(queue.getMetrics() != null)
						{
							((MetricImpl)queue.getMetrics()).dispose();
						}
					}
					catch (Exception e) {}
					
					try
					{
						queue.dispose();
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					
					try
					{
						queue.stopQueueWorker();
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					
					this.queueIndex.remove(queue.getQueueId());
					
					counterQueueSize.dec();
				}
			}
			finally 
			{
				this.queueIndexWriteLock.unlock();
			}
		}
		return registered;
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindQueueService(IQueueService queueService,Map<String, ?> properties)
	{
		synchronized (this.serviceList)// sync
		{
			ServiceContainer serviceContainer = new ServiceContainer();
			serviceContainer.setQueueService(queueService);
			serviceContainer.setProperties(properties);
			
			this.serviceList.add(serviceContainer);
			
		}
		
		if(this.context != null)
		{
			registerQueueService(queueService, properties);
		}
	}
	
	private boolean registerQueueService(IQueueService queueService,Map<String, ?> properties)
	{
		if(this.context == null)
		{
			return false;
		}
		
		String queueId = (String)properties.get(IQueueService.PROPERTY_QUEUE_ID);
		if(queueId == null)
		{
			if(this.logService != null)
			{
				this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "Missing QueueId (Null) for service");
			}
			else
			{
				System.err.println("Missing QueueId (Null) for service");
			}
			return false;
		}
		if(queueId.isEmpty())
		{
			if(this.logService != null)
			{
				this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "Missing QueueId (Empty) for service");
			}
			else
			{
				System.err.println("Missing QueueId (empty) for service");
			}
			return false;
		}
		QueueImpl queue = null;
		this.queueIndexReadLock.lock();
		try
		{
			queue = this.queueIndex.get(queueId);
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		
		if(queue == null)
		{
			return false;
		}
		
		queue.addService(queueService, properties);
		return true;
	}
	
	public void unbindQueueService(IQueueService queueService,Map<String, ?> properties)
	{
		synchronized (this.serviceList)
		{
			List<ServiceContainer> removeList = null;
			for(ServiceContainer serviceContainer : this.serviceList)
			{
				if(serviceContainer.getQueueService() == queueService)
				{
					if(removeList ==  null)
					{
						removeList = new ArrayList<ServiceContainer>();
					}
					removeList.add(serviceContainer);
				}
			}
			if(removeList != null)
			{
				for(ServiceContainer serviceContainer : removeList)
				{
					this.serviceList.remove(serviceContainer);
				}
			}
		}
		if(this.context != null)
		{
			this.unregisterQueueService(queueService);
		}
	}
	
	private boolean unregisterQueueService(IQueueService queueService)
	{
		if(this.context == null)
		{
			return false;
		}
		
		boolean registered = false;
		this.queueIndexReadLock.lock();
		try
		{
			for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet() )
			{
				if(entry.getValue().removeService(queueService))
				{
					// TODO setDone
				}
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		return registered;
	}

	public ComponentContext getContext()
	{
		return context;
	}

	public LogService getLogService()
	{
		return logService;
	}

	public MetricRegistry getMetricRegistry()
	{
		return this.metricRegistry;
	}

	public IMetrics getMetrics()
	{
		return metrics;
	}

	@Override
	public IPropertyBlock createPropertyBlock()
	{
		return new PropertyBlockImpl();
	}
	
}
