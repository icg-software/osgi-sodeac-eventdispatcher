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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.sling.commons.metrics.Counter;
import org.apache.sling.commons.metrics.MetricsService;
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
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
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
	
	private volatile ComponentContext context = null;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile LogService logService = null;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile EventAdmin eventAdmin;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC,target = "(name=sling)")
	protected volatile MetricRegistry metricRegistry;
	
	protected volatile MetricsService metricsService;
	
	public EventDispatcherImpl()
	{
		super();
		this.queueIndexLock = new ReentrantReadWriteLock(true);
		this.queueIndexReadLock = this.queueIndexLock.readLock();
		this.queueIndexWriteLock = this.queueIndexLock.writeLock();
		this.controllerList = new ArrayList<ControllerContainer>();
		
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
			// TODO oneTimeLog
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
	
	/*@Override
	public boolean ensureQueueExists(String queueId)
	{
		QueueImpl queue = null;
		this.queueIndexReadLock.lock();
		try
		{
			queue = this.queueIndex.get(queueId);
			
			if(queue != null)
			{
				return false;
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}

		this.queueIndexWriteLock.lock();
		try
		{
			queue = this.queueIndex.get(queueId);
			
			if(queue != null)
			{
				return false;
			}
			
			queue = new QueueImpl(queueId, this);
			
			this.queueIndex.put(queueId,queue );
			if(this.counterQueueSize != null)
			{
				this.counterQueueSize.increment();
			}
		}
		finally 
		{
			this.queueIndexWriteLock.unlock();
		}
		
		return true;
	}*/
	
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
	
	private Counter counterQueueSize;
	private Counter counterConfigurationSize;
	
	@Activate
	private void activate(ComponentContext context, Map<String, ?> properties)
	{
		this.context = context;
		
		this.queueIndexReadLock.lock();
		try
		{
			counterQueueSize = metricsService.counter(MetricRegistry.name(IEventDispatcher.class, "queues"));
			if(! this.queueIndex.isEmpty())
			{
				counterQueueSize.increment(this.queueIndex.size());
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		synchronized (this.controllerList)
		{
			counterConfigurationSize = metricsService.counter(MetricRegistry.name(IEventDispatcher.class, "controllerregistrations"));
			
			if(! this.controllerList.isEmpty())
			{
				counterConfigurationSize.increment(this.controllerList.size());
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
		
	}
	
	@Deactivate
	private void deactivate(ComponentContext context)
	{
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
				entry.getValue().stopQueueMonitor();
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
				counterQueueSize.decrement(this.queueIndex.size());
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
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindMetricsService(MetricsService metricsService,Map<String, ?> properties)
	{
		this.metricsService = metricsService;
		
		synchronized (this.controllerList)
		{
			if(this.counterConfigurationSize != null)
			{
				if(! this.controllerList.isEmpty())
				{
					this.counterConfigurationSize.increment(this.controllerList.size());
				}
			}
		}
	}
	
	public void unbindMetricsService(MetricsService metricsService,Map<String, ?> properties)
	{
		if(this.metricsService != null)
		{
			if(this.counterConfigurationSize != null)
			{
				if(! this.controllerList.isEmpty())
				{
					this.counterConfigurationSize.decrement(this.controllerList.size());
				}
			}
		}
		this.counterQueueSize = null;
		this.counterConfigurationSize = null;
		this.metricsService = null;
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
				this.counterConfigurationSize.increment();
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
					this.counterQueueSize.increment();
				}
			}
			finally 
			{
				this.queueIndexWriteLock.unlock();
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
						this.counterConfigurationSize.decrement();
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
					// TODO remove/stop Queue
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

	public MetricsService getMetricsService()
	{
		return metricsService;
	}

	public MetricRegistry getMetricRegistry()
	{
		return metricRegistry;
	}

	@Override
	public IPropertyBlock createPropertyBlock()
	{
		return new PropertyBlockImpl();
	}
	
}
