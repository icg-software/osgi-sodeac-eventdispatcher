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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
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
import org.sodeac.eventdispatcher.api.IOnJobStop;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IScheduleResult;
import org.sodeac.eventdispatcher.api.ITimer;
import org.sodeac.eventdispatcher.api.QueueNotFoundException;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;
import org.sodeac.eventdispatcher.extension.api.IExtensibleEventDispatcher;
import org.sodeac.eventdispatcher.api.ICounter;
import org.sodeac.eventdispatcher.api.IEventController;

import com.codahale.metrics.MetricRegistry;

@Component(name="EventDispatcherProvider" ,service=IEventDispatcher.class,immediate=true,property={IEventDispatcher.PROPERTY_ID + "=" + IEventDispatcher.DEFAULT_DISPATCHER_ID})
public class EventDispatcherImpl implements IEventDispatcher,IExtensibleEventDispatcher
{
	private Map<String,QueueImpl> queueIndex;
	private ReentrantReadWriteLock queueIndexLock;
	private ReadLock queueIndexReadLock;
	private WriteLock queueIndexWriteLock;
	
	private ReentrantReadWriteLock osgiLifecycleLock;
	private ReadLock osgiLifecycleReadLock;
	private WriteLock osgiLifecycleWriteLock;
	
	private List<QueueWorker> workerPool;
	private ReentrantReadWriteLock workerPoolLock;
	private ReadLock workerPoolReadLock;
	private WriteLock workerPoolWriteLock;
	
	private DispatcherGuardian dispatcherGuardian;
	private SpooledQueueWorkerScheduler spooledQueueWorkerScheduler;
	
	private List<ControllerContainer> controllerList = null;
	private Map<IEventController,ControllerContainer> controllerReverseIndex = null; 
	private List<ControllerContainer> controllerListScheduled = null;
	private ReentrantReadWriteLock controllerListLock;
	private ReadLock controllerListReadLock;
	private WriteLock controllerListWriteLock;
	
	private List<ServiceContainer> serviceList = null;
	private Map<IQueueService ,ServiceContainer> serviceReverseIndex = null; 
	private List<ServiceContainer> serviceListScheduled = null;
	private ReentrantReadWriteLock serviceListLock;
	private ReadLock serviceListReadLock;
	private WriteLock serviceListWriteLock;
	
	private String id = IEventDispatcher.DEFAULT_DISPATCHER_ID;
	
	private volatile ComponentContext context = null;
	private volatile boolean activated = false;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile LogService logService = null;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile EventAdmin eventAdmin;
	
	protected volatile MetricRegistry metricRegistry = new  MetricRegistry();
	private List<ServiceRegistration<?>> registrationList = new ArrayList<ServiceRegistration<?>>();
	
	private volatile MetricImpl metrics = null; 
	
	private List<IEventDispatcherExtension>  extensionList = null;
	private List<IEventDispatcherExtension> extensionListScheduled = null;
	private ReentrantReadWriteLock extensionListLock = null;
	private ReadLock extensionListReadLock = null;
	private WriteLock extensionListWriteLock = null;
	private volatile List<IEventDispatcherExtension>  extensionListCopy = null;
	
	private Map<String,Long> queueIsMissingLogIndex = null;
	private ReentrantLock queueIsMissingLogIndexLock = null;
	
	private ExecutorService executorService = null;
	
	@Override
	public String getId()
	{
		return this.id;
	}
	
	@Override
	public String getBundleId()
	{
		if(this.context == null)
		{
			return new String();
		}
		return this.context.getBundleContext().getBundle().getSymbolicName();
	}
	
	@Override
	public String getBundleVersion()
	{
		if(this.context == null)
		{
			return new String();
		}
		return this.context.getBundleContext().getBundle().getVersion().toString();
	}

	public EventDispatcherImpl()
	{
		super();
		this.queueIndex = new HashMap<String,QueueImpl>();
		this.queueIndexLock = new ReentrantReadWriteLock(true);
		this.queueIndexReadLock = this.queueIndexLock.readLock();
		this.queueIndexWriteLock = this.queueIndexLock.writeLock();
		
		this.osgiLifecycleLock = new ReentrantReadWriteLock(true);
		this.osgiLifecycleReadLock = this.osgiLifecycleLock.readLock();
		this.osgiLifecycleWriteLock = this.osgiLifecycleLock.writeLock();
		
		this.controllerList = new ArrayList<ControllerContainer>();
		this.controllerListScheduled = new ArrayList<ControllerContainer>();
		this.controllerReverseIndex = new HashMap<IEventController,ControllerContainer>();
		this.controllerListLock = new ReentrantReadWriteLock(true);
		this.controllerListReadLock = this.controllerListLock.readLock();
		this.controllerListWriteLock = this.controllerListLock.writeLock();
		
		this.extensionList = new ArrayList<IEventDispatcherExtension>();
		this.extensionListCopy = Collections.unmodifiableList(new ArrayList<IEventDispatcherExtension>());
		this.extensionListScheduled  = new ArrayList<IEventDispatcherExtension>();
		this.extensionListLock = new ReentrantReadWriteLock(true);
		this.extensionListReadLock = extensionListLock.readLock();
		this.extensionListWriteLock = extensionListLock.writeLock();
		
		this.workerPool = new ArrayList<QueueWorker>();
		this.workerPoolLock = new ReentrantReadWriteLock(true);
		this.workerPoolReadLock = this.workerPoolLock.readLock();
		this.workerPoolWriteLock = this.workerPoolLock.writeLock();
		
		this.queueIsMissingLogIndex = new HashMap<String,Long>();
		this.queueIsMissingLogIndexLock = new ReentrantLock();
		
		this.serviceList = new ArrayList<ServiceContainer>();
		this.serviceReverseIndex = new HashMap<IQueueService ,ServiceContainer>();
		this.serviceListScheduled = new ArrayList<ServiceContainer>();
		this.serviceListLock = new ReentrantReadWriteLock(true);
		this.serviceListReadLock = this.serviceListLock.readLock();
		this.serviceListWriteLock = this.serviceListLock.writeLock();
		
		this.metrics = new MetricImpl(this, new PropertyBlockImpl(this),true);
	}
	
	@Override
	public Future<IScheduleResult> schedule(String queueId, Event event)
	{
		osgiLifecycleReadLock.lock();
		try
		{
			if(this.context == null)
			{
				return null;
			}
			
			if(! activated)
			{
				return null;
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
				
				boolean log = false;
				this.queueIsMissingLogIndexLock.lock();
				try
				{
					if(( this.queueIsMissingLogIndex.get(queueId) == null) || (this.queueIsMissingLogIndex.get(queueId).longValue() < (System.currentTimeMillis() - (1000L * 60L * 60L))))
					{
						log = true;
						this.queueIsMissingLogIndex.put(queueId,System.currentTimeMillis());
					}
				}
				finally 
				{
					this.queueIsMissingLogIndexLock.unlock();
				}
				if(log)
				{
					log(LogService.LOG_ERROR, "Queue is missing " + queueId,null);
				}
				throw new QueueNotFoundException(queueId);
			}
			
			return queue.scheduleEvent(event);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
	}
	
	@Override
	public Future<IScheduleResult> schedule(String queueId, List<Event> eventList)
	{
		osgiLifecycleReadLock.lock();
		try
		{
			if(this.context == null)
			{
				return null;
			}
			
			if(! activated)
			{
				return null;
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
				
				boolean log = false;
				this.queueIsMissingLogIndexLock.lock();
				try
				{
					if(( this.queueIsMissingLogIndex.get(queueId) == null) || (this.queueIsMissingLogIndex.get(queueId).longValue() < (System.currentTimeMillis() - (1000L * 60L * 60L))))
					{
						log = true;
						this.queueIsMissingLogIndex.put(queueId,System.currentTimeMillis());
					}
				}
				finally 
				{
					this.queueIsMissingLogIndexLock.unlock();
				}
				if(log)
				{
					log(LogService.LOG_ERROR, "Queue is missing " + queueId,null);
				}
				throw new QueueNotFoundException(queueId);
			}
			
			return queue.scheduleEventList(eventList);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
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
		osgiLifecycleWriteLock.lock();
		try
		{
			this.context = context;
			
			if((properties.get(IEventDispatcher.PROPERTY_ID) != null) && (properties.get(IEventDispatcher.PROPERTY_ID).toString().length() > 0))
			{
				this.id = properties.get(IEventDispatcher.PROPERTY_ID).toString();
			}
			
			this.queueIndexReadLock.lock();
			try
			{
				counterQueueSize = metrics.counter(IMetrics.METRICS_QUEUE);
				if(! this.queueIndex.isEmpty())
				{
					counterQueueSize.inc(this.queueIndex.size());
				}
			}
			finally 
			{
				this.queueIndexReadLock.unlock();
			}
			
			counterConfigurationSize = metrics.counter( IMetrics.METRICS_EVENT_CONTROLLER);
			
			this.executorService = Executors.newCachedThreadPool();
			
			this.dispatcherGuardian = new DispatcherGuardian(this);
			this.dispatcherGuardian.start();
			
			this.spooledQueueWorkerScheduler = new SpooledQueueWorkerScheduler(this);
			this.spooledQueueWorkerScheduler.start();
			
			Dictionary<String, Object> metricRegistryProperties = new Hashtable<>();
			metricRegistryProperties.put(Constants.SERVICE_DESCRIPTION, "Sodeac Metrics Registry for EventDispatcher");
			metricRegistryProperties.put(Constants.SERVICE_VENDOR, "Sodeac Framework");
			metricRegistryProperties.put("name", "sodeac-eventdispatcher-" + this.id); 
			registrationList.add(context.getBundleContext().registerService(MetricRegistry.class.getName(), metricRegistry, metricRegistryProperties));
		}
		finally 
		{
			osgiLifecycleWriteLock.unlock();
		}
		
		for(IEventDispatcherExtension eventDispatcherExtension : this.extensionListScheduled)
		{
			this.bindEventDispatcherExtension(eventDispatcherExtension);
		}
		
		this.extensionListScheduled.clear();
		this.extensionListScheduled = null;
		
		for(ServiceContainer container : this.serviceListScheduled)
		{
			this.bindQueueService(container.getQueueService(), container.getProperties());
		}
		
		this.serviceListScheduled.clear();
		this.serviceListScheduled = null;
		
		for(ControllerContainer container : this.controllerListScheduled)
		{
			this.bindEventController(container.getEventController(), container.getProperties());
		}
		
		this.controllerListScheduled.clear();
		this.controllerListScheduled = null;
		
		this.activated = true;
		
	}
	
	@Deactivate
	private void deactivate(ComponentContext context)
	{
		osgiLifecycleWriteLock.lock();
	
		try
		{
			for (ServiceRegistration<?> reg : registrationList) 
			{
	            reg.unregister();
			}
			controllerListReadLock.lock();
			try
			{
				for(ControllerContainer controllerContainer : controllerList)
				{
					try
					{
						this.unregisterEventController(controllerContainer);
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
			}
			finally 
			{
				controllerListReadLock.unlock();
			}
			
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
						log(LogService.LOG_ERROR,"dispose queue on dispatcher shutdown",e);
					}
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
				this.dispatcherGuardian.stopGuardian();
			}
			catch (Exception e) {}
			
			try
			{
				this.spooledQueueWorkerScheduler.stopScheduler();
			}
			catch (Exception e) {}
			
			counterQueueSize = null;
			counterConfigurationSize = null;
			
			for(IEventDispatcherExtension eventDispatcherExtension : this.extensionListCopy)
			{
				try
				{
					eventDispatcherExtension.unregisterEventDispatcher(this);
				}
				catch (Exception e) 
				{
					if(logService != null)
					{
						logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Exception on unregister dispatcher to extension", e);
					}
					else
					{
						System.err.println("Exception on unregister dispatcher to extension " + e.getMessage());
					}
				}
			}
			
			this.workerPoolWriteLock.lock();
			try
			{
				for(QueueWorker worker : this.workerPool)
				{
					try
					{
						worker.stopWorker();
					}
					catch (Exception e) {}
				}
				this.workerPool.clear();
				this.workerPool = null;
			}
			finally 
			{
				this.workerPoolWriteLock.unlock();
			}
			
			this.context = null;
			try
			{
				this.metrics.dispose();
			}
			catch (Exception e) {e.printStackTrace();}
		}
		finally 
		{
			osgiLifecycleWriteLock.unlock();
		}
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindEventDispatcherExtension(IEventDispatcherExtension eventDispatcherExtension)
	{
		osgiLifecycleReadLock.lock();
		try
		{
			if(this.context ==  null)
			{
				// wait for activate, before than collect in controllerListScheduled
				
				extensionListWriteLock.lock();
				try
				{
					if(this.extensionListScheduled == null)
					{
						return;
					}
					
					this.extensionListScheduled.add(eventDispatcherExtension);
				}
				finally 
				{
					extensionListWriteLock.unlock();
				}
				return;
			}
			
			this.extensionListWriteLock.lock();
			try
			{
				boolean alreadyConnected = false;
				for(IEventDispatcherExtension extension : this.extensionList)
				{
					if(extension == eventDispatcherExtension)
					{
						alreadyConnected = true;
						break;
					}
				}
				if(! alreadyConnected)
				{
					this.extensionList.add(eventDispatcherExtension);
					this.extensionListCopy = Collections.unmodifiableList(new ArrayList<IEventDispatcherExtension>(this.extensionList));
				}
				else
				{
					return;
				}
			}
			finally 
			{
				this.extensionListWriteLock.unlock();
			}
		}
		finally
		{
			osgiLifecycleReadLock.unlock();
		}
		
		this.registerDispatcherExtension(eventDispatcherExtension);
	}
	
	private void registerDispatcherExtension(IEventDispatcherExtension eventDispatcherExtension)
	{
		try
		{
			eventDispatcherExtension.registerEventDispatcher(this);
			this.metrics.registerOnExtension(eventDispatcherExtension);
			
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR, "Exception on register dispatcher to extension", e);
		}
		
		
		controllerListReadLock.lock();
		try
		{
			for(ControllerContainer controllerContainer : controllerList)
			{
				eventDispatcherExtension.registerEventController(this,controllerContainer.getEventController(), controllerContainer.getProperties());
			}
		}
		finally 
		{
			controllerListReadLock.unlock();
		}
		
		this.queueIndexReadLock.lock();
		try
		{
			for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet())
			{
				eventDispatcherExtension.registerEventQueue(this,entry.getValue());
				entry.getValue().registerOnExtension(eventDispatcherExtension);
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
	}

	public void unbindEventDispatcherExtension(IEventDispatcherExtension eventDispatcherExtension)
	{
		osgiLifecycleReadLock.lock();
		try
		{
			this.extensionListWriteLock.lock();
			try
			{
				while(this.extensionList.remove(eventDispatcherExtension)) {}
				this.extensionListCopy = Collections.unmodifiableList(new ArrayList<IEventDispatcherExtension>(this.extensionList));
			}
			finally 
			{
				this.extensionListWriteLock.unlock();
			}
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
		
		try
		{
			eventDispatcherExtension.unregisterEventDispatcher(this);
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR, "Exception on unregister dispatcher to extension", e);
		}
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindEventController(IEventController eventController,Map<String, ?> properties)
	{
		osgiLifecycleReadLock.lock();
		try
		{
			if(this.context ==  null)
			{
				// wait for activate, before than collect in controllerListScheduled
				
				controllerListWriteLock.lock();
				try
				{
					if(this.controllerListScheduled == null)
					{
						return;
					}
					
					ControllerContainer controllerContainer = new ControllerContainer();
					controllerContainer.setEventController(eventController);
					controllerContainer.setProperties(properties);
					
					this.controllerListScheduled.add(controllerContainer);
				}
				finally 
				{
					controllerListWriteLock.unlock();
				}
				return;
			}
			
			String dispatcherId = IEventDispatcher.DEFAULT_DISPATCHER_ID;
			if((properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) != null) && (properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString().length() > 0))
			{
				dispatcherId = properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString();
			}
			if(! dispatcherId.equals(this.id))
			{
				return;
			}
			
			ControllerContainer controllerContainer = null;
			controllerListWriteLock.lock();
			try
			{
				controllerContainer = this.controllerReverseIndex.get(eventController);
				
				if(controllerContainer == null)
				{
					controllerContainer = new ControllerContainer();
					controllerContainer.setEventController(eventController);
					controllerContainer.setProperties(properties);
					
					this.controllerReverseIndex.put(eventController,controllerContainer);
					this.controllerList.add(controllerContainer);
					
					if(this.counterConfigurationSize != null)
					{
						this.counterConfigurationSize.inc();
					}
				}
			}
			finally 
			{
				controllerListWriteLock.unlock();
			}
			
			registerEventController(controllerContainer);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
	}
	
	private boolean registerEventController(ControllerContainer controllerContainer)
	{
		this.extensionListReadLock.lock();
		try
		{
			if(controllerContainer.isRegistered())
			{
				return false;
			}
			
			controllerContainer.setRegistered(true);
			
			String queueConfigurationFilter = controllerContainer.getNonEmptyStringProperty(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER,"");
			String queueId = controllerContainer.getNonEmptyStringProperty(IEventDispatcher.PROPERTY_QUEUE_ID,"");
			if( queueId.isEmpty() && queueConfigurationFilter.isEmpty() )
			{
				if(this.logService != null)
				{
					log(LogService.LOG_WARNING, "Missing QueueId or ConfigurationFilter",null);
				}
				return false;
			}
			
			try
			{
				getMetrics().meter(IMetrics.METRICS_EVENT_CONTROLLER,IMetrics.METRICS_CREATE).mark();
			}
			catch(Exception e)
			{
				log(LogService.LOG_ERROR, "mark metric register controller", e);
			}
			
			ITimer.Context timerContext = null;
			try
			{
				timerContext = getMetrics().timer(IMetrics.METRICS_EVENT_CONTROLLER,IMetrics.METRICS_CREATE).time();
			}
			catch(Exception e)
			{
				log(LogService.LOG_ERROR, "metric timer register controller", e);
			}
			
			QueueImpl queue = null;
			
			boolean controllerInUse = false;
			
			if((queueId != null) && (! queueId.isEmpty()))
			{	
			
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
					this.queueIndexWriteLock.lock();
					try
					{
						queue = this.queueIndex.get(queueId);
					
						if(queue == null)
						{
							try
							{
								getMetrics().meter(IMetrics.METRICS_QUEUE,IMetrics.METRICS_CREATE).mark();
							}
							catch(Exception e)
							{
								log(LogService.LOG_ERROR, "mark metric queue create", e);
							}
							
							ITimer.Context createQueueTimerContext = null;
							try
							{
								createQueueTimerContext = getMetrics().timer(IMetrics.METRICS_QUEUE,IMetrics.METRICS_CREATE).time();
							}
							catch(Exception e)
							{
								log(LogService.LOG_ERROR, "metric timer queue create", e);
							}
							
							String name  = controllerContainer.getNonEmptyStringProperty(IEventController.PROPERTY_JMX_NAME, controllerContainer.getEventController().getClass().getSimpleName());
							String category = controllerContainer.getNonEmptyStringProperty(IEventController.PROPERTY_JMX_CATEGORY,null);
							
							queue = new QueueImpl(queueId,this, ! controllerContainer.isDisableMetrics(), name,category,null,null);
							this.queueIndex.put(queueId,queue);
							if(this.counterQueueSize != null)
							{
								this.counterQueueSize.inc();
							}
							
							for(IEventDispatcherExtension extension : this.extensionListCopy)
							{
								try
								{
									extension.registerEventQueue(this, queue);
									queue.registerOnExtension(extension);
								}
								catch (Exception e) 
								{
									log(LogService.LOG_ERROR, "register new queue to extension",e);
								}
							}
							
							serviceListReadLock.lock();
							try
							{
								QueueBindingModifyFlags modifyFlags = new QueueBindingModifyFlags();
								
								for(ServiceContainer serviceContainer :  this.serviceList )
								{
									modifyFlags.reset();
									
									queue.checkForService(serviceContainer, modifyFlags);
								}
								
								try
								{
									getMetrics().histogram(IMetrics.METRICS_QUEUE,IMetrics.METRICS_CREATE,IMetrics.METRICS_SERVICE,IMetrics.METRICS_MATCH_CHECK).update(serviceList.size());
								}
								catch(Exception e)
								{
									log(LogService.LOG_ERROR, "metric histogram queue create", e);
								}
							}
							finally 
							{
								serviceListReadLock.unlock();
							}
							
							if(createQueueTimerContext != null)
							{
								try {createQueueTimerContext.stop();}catch (Exception e) {}
							}
						}
					}
					finally 
					{
						this.queueIndexWriteLock.unlock();
					}
					
					controllerInUse = true;
					queue.setController(controllerContainer);
				}
			}
		
			if(! queueConfigurationFilter.isEmpty())
			{
				try
				{
					QueueBindingModifyFlags modifyFlags = new QueueBindingModifyFlags();
					
					this.queueIndexReadLock.lock();
					try
					{
						for(Entry<String,QueueImpl> entry : queueIndex.entrySet())
						{
							modifyFlags.reset();
							entry.getValue().checkForController(controllerContainer,modifyFlags);
							if(modifyFlags.isGlobalSet() || modifyFlags.isScopeSet())
							{
								controllerInUse = true;
							}
						}
						
						try
						{
							getMetrics().histogram(IMetrics.METRICS_EVENT_CONTROLLER,IMetrics.METRICS_CREATE,IMetrics.METRICS_QUEUE,IMetrics.METRICS_MATCH_CHECK).update(queueIndex.size());
						}
						catch(Exception e)
						{
							log(LogService.LOG_ERROR, "metric histogram register controller", e);
						}
					}
					finally 
					{
						this.queueIndexReadLock.unlock();
					}
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"check queue binding for controller by configuration filter",e);
				}
			}
			
			if(timerContext != null)
			{
				try {timerContext.stop();}catch (Exception e) {}
			}
			
			return controllerInUse;
		}
		finally 
		{
			extensionListReadLock.unlock();
		}
	}
	
	public void unbindEventController(IEventController eventController,Map<String, ?> properties)
	{
		ControllerContainer controllerContainer = null;
		osgiLifecycleReadLock.lock();
		try
		{
			
			this.controllerListWriteLock.lock();
			try
			{
				controllerContainer = this.controllerReverseIndex.get(eventController);
				
				if(controllerContainer == null)
				{
					return;
				}
				
				while(this.controllerList.remove(controllerContainer)) 
				{
					if(this.counterConfigurationSize != null)
					{
						this.counterConfigurationSize.dec();
					}
				}
				this.controllerReverseIndex.remove(eventController);
			}
			finally 
			{
				controllerListWriteLock.unlock();
			}
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
		
		this.unregisterEventController(controllerContainer);
		
		for(IEventDispatcherExtension extension : this.extensionListCopy)
		{
			try
			{
				extension.unregisterEventController(this,eventController);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR, "unregister eventcontroller from extension", e);
			}
		}
	}
	
	private boolean unregisterEventController(ControllerContainer controllerContainer)
	{
		boolean registered = false;
		List<QueueImpl> registeredOnQueueList = null;
		List<QueueImpl> queueRemoveList = null;
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_EVENT_CONTROLLER,IMetrics.METRICS_DISPOSE).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric register controller", e);
		}
		
		ITimer.Context timerContext = null;
		try
		{
			timerContext = getMetrics().timer(IMetrics.METRICS_EVENT_CONTROLLER,IMetrics.METRICS_DISPOSE).time();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "metric timer register controller", e);
		}
		
		this.queueIndexReadLock.lock();
		try
		{
			for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet() )
			{
				if(entry.getValue().unsetController(controllerContainer))
				{
					if(registeredOnQueueList == null)
					{
						registeredOnQueueList = new ArrayList<QueueImpl>();
					}
					registered = true;
					registeredOnQueueList.add(entry.getValue());
				}
				
				if(entry.getValue().getControllerSize() == 0)
				{
					if(queueRemoveList == null)
					{
						queueRemoveList = new ArrayList<QueueImpl>();
					}
					queueRemoveList.add(entry.getValue());
				}
			}
			try
			{
				getMetrics().histogram(IMetrics.METRICS_EVENT_CONTROLLER,IMetrics.METRICS_DISPOSE,IMetrics.METRICS_QUEUE,IMetrics.METRICS_MATCH_CHECK).update(this.queueIndex.size());
			}
			catch(Exception e)
			{
				log(LogService.LOG_ERROR, "metric histogram unregister controller", e);
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		if(controllerContainer.getEventController() instanceof IOnQueueReverse)
		{
			try
			{
				if(registeredOnQueueList != null)
				{
					for(QueueImpl queue : registeredOnQueueList)
					{	
						this.executeOnQueueReverse((IOnQueueReverse)controllerContainer.getEventController(), queue);
					}
				}
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR, "Exception on onQueueReverse() event controller", e);
			}
		}
		
		if(queueRemoveList != null)
		{
			this.queueIndexWriteLock.lock();
			try
			{
				for(QueueImpl queue : queueRemoveList)
				{

					try
					{
						getMetrics().meter(IMetrics.METRICS_QUEUE,IMetrics.METRICS_DISPOSE).mark();
					}
					catch(Exception e)
					{
						log(LogService.LOG_ERROR, "mark metric queue disponse", e);
					}
					
					ITimer.Context removeQueueTimerContext = null;
					try
					{
						removeQueueTimerContext = getMetrics().timer(IMetrics.METRICS_QUEUE,IMetrics.METRICS_DISPOSE).time();
					}
					catch(Exception e)
					{
						log(LogService.LOG_ERROR, "metric timer queue dispose", e);
					}
					
					try
					{
						queue.dispose();
					}
					catch(Exception e)
					{
						log(LogService.LOG_ERROR,"dispose queue after remove all controller",e);
					}
					
					this.queueIndex.remove(queue.getQueueId());
					counterQueueSize.dec();
					
					if(removeQueueTimerContext != null)
					{
						try {removeQueueTimerContext.stop();}catch (Exception e) {}
					}
					
				}
			}
			finally 
			{
				this.queueIndexWriteLock.unlock();
			}
		}
		
		if(timerContext != null)
		{
			try {timerContext.stop();}catch (Exception e) {}
		}
		
		return registered;
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindQueueService(IQueueService queueService,Map<String, ?> properties)
	{
		osgiLifecycleReadLock.lock();
		try
		{
			if(this.context ==  null)
			{
				// wait for activate, before than collect in serviceListScheduled
				
				serviceListWriteLock.lock();
				try
				{
					if(this.serviceListScheduled == null)
					{
						return;
					}
					
					ServiceContainer serviceContainer = new ServiceContainer();
					serviceContainer.setQueueService(queueService);
					serviceContainer.setProperties(properties);
					
					this.serviceListScheduled.add(serviceContainer);
				}
				finally 
				{
					serviceListWriteLock.unlock();
				}
				return;
			}
			
			String dispatcherId = IEventDispatcher.DEFAULT_DISPATCHER_ID;
			if((properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) != null) && (properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString().length() > 0))
			{
				dispatcherId = properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString();
			}
			if(! dispatcherId.equals(this.id))
			{
				return;
			}
			
			ServiceContainer serviceContainer = null;
			serviceListWriteLock.lock();
			try
			{
				serviceContainer = serviceReverseIndex.get(queueService);
				
				if(serviceContainer == null)
				{
					serviceContainer = new ServiceContainer();
					serviceContainer.setQueueService(queueService);
					serviceContainer.setProperties(properties);
					
					this.serviceList.add(serviceContainer);
					this.serviceReverseIndex.put(queueService,serviceContainer);
				}				
			}
			finally 
			{
				serviceListWriteLock.unlock();
			}
			
			registerQueueService(serviceContainer);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
		
	}
	
	private boolean registerQueueService(ServiceContainer serviceContainer)
	{
		extensionListReadLock.lock();
		try
		{
			if(serviceContainer.isRegistered())
			{
				return false;
			}
			
			serviceContainer.setRegistered(true);
			
			String queueConfigurationFilter = serviceContainer.getNonEmptyStringProperty(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER,"");
			String queueId = serviceContainer.getNonEmptyStringProperty(IQueueService.PROPERTY_QUEUE_ID,"");
			
			if(queueId.isEmpty() && queueConfigurationFilter.isEmpty())
			{
				log(LogService.LOG_WARNING, "Missing queueId or queueConfigurationFilter for service", null);
				
				return false;
			}
			
			QueueBindingModifyFlags modifyFlags = new QueueBindingModifyFlags();
			
			if(queueConfigurationFilter.isEmpty())
			{
				this.queueIndexReadLock.lock();
				try
				{
					QueueImpl queue = this.queueIndex.get(queueId);
					if(queue != null)
					{
						queue.checkForService(serviceContainer, modifyFlags);
					}
				}
				finally 
				{
					this.queueIndexReadLock.unlock();
				}
			}
			else
			{
				this.queueIndexReadLock.lock();
				try
				{
					for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet())
					{
						modifyFlags.reset();
						entry.getValue().checkForService(serviceContainer, modifyFlags);
					}
				}
				finally 
				{
					this.queueIndexReadLock.unlock();
				}
			}
			
			return true;
		}
		finally 
		{
			extensionListReadLock.unlock();
		}
	}
	
	public void unbindQueueService(IQueueService queueService,Map<String, ?> properties)
	{
		ServiceContainer serviceContainer = null;
		osgiLifecycleReadLock.lock();
		try
		{
			this.serviceListWriteLock.lock();
			try
			{
				serviceContainer = this.serviceReverseIndex.get(queueService);
				if(serviceContainer == null)
				{
					return;
				}
				
				this.serviceReverseIndex.remove(queueService);
				while(this.serviceList.remove(serviceContainer)) {}
			}
			finally 
			{
				this.serviceListWriteLock.unlock();
			}
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
		this.unregisterQueueService(serviceContainer);
		
	}
	
	private boolean unregisterQueueService(ServiceContainer serviceContainer)
	{
		boolean registered = false;
		this.queueIndexReadLock.lock();
		try
		{
			for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet() )
			{
				entry.getValue().unsetService(serviceContainer);
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		return registered;
	}
	
	protected void log(int logServiceLevel,String logMessage, Throwable e)
	{
		
		try
		{
			LogService logService = this.logService;
			ComponentContext context = this.context;
			
			if(logService != null)
			{
				logService.log(context == null ? null : context.getServiceReference(), logServiceLevel, logMessage, e);
			}
			else
			{
				if(logServiceLevel == LogService.LOG_ERROR)
				{
					System.err.println(logMessage);
				}
				if(e != null)
				{
					e.printStackTrace();
				}
			}
		}
		catch (Exception ie) 
		{
			ie.printStackTrace();
		}
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
		return new PropertyBlockImpl(this);
	}

	public List<IEventDispatcherExtension> getEventDispatcherExtensionList()
	{
		return extensionListCopy;
	}
	
	protected boolean addToWorkerPool(QueueWorker worker)
	{
		if(! worker.isGo())
		{
			return false;
		}
		if(worker.getEventQueue() != null)
		{
			return false;
		}
		worker.setSpoolTimeStamp(System.currentTimeMillis());
		this.workerPoolWriteLock.lock();
		try
		{
			this.workerPool.add(0, worker);
		}
		finally 
		{
			this.workerPoolWriteLock.unlock();
		}
		
		return true;
	}
	
	protected QueueWorker getFromWorkerPool()
	{
		this.workerPoolWriteLock.lock();
		try
		{
			QueueWorker foundWorker = null;
			while(! this.workerPool.isEmpty())
			{
				foundWorker = this.workerPool.remove(0);
				if(! foundWorker.isGo())
				{
					continue;
				}
				if(foundWorker.getEventQueue() != null)
				{
					continue;
				}
				if(! foundWorker.isAlive())
				{
					continue;
				}
				return foundWorker;
			}
			return null;
		}
		finally 
		{
			this.workerPoolWriteLock.unlock();
		}
	}
	
	protected void checkTimeoutWorker()
	{
		this.workerPoolWriteLock.lock();
		try
		{
			long shutdownTimeStamp = System.currentTimeMillis() - QueueWorker.DEFAULT_SHUTDOWN_TIME;
			List<QueueWorker> removeList = new ArrayList<QueueWorker>();
			for(QueueWorker worker : this.workerPool)
			{
				try
				{
					if(worker.getEventQueue() != null)
					{
						removeList.add(worker);
						continue;
					}
					if(! worker.isGo())
					{
						removeList.add(worker);
						continue;
					}
					if(worker.getSpoolTimeStamp() < shutdownTimeStamp)
					{
						removeList.add(worker);
						continue;
					}
				}
				catch (Exception e) {}
				catch (Error e) {}
			}
			for(QueueWorker remove : removeList)
			{
				try
				{
					this.workerPool.remove(remove);
					remove.stopWorker();
				}
				catch (Exception e) {this.log(LogService.LOG_ERROR, "remove spooled worker", e);}
				catch (Error e) {this.log(LogService.LOG_ERROR, "remove spooled worker", e);}
			}
		}
		finally 
		{
			this.workerPoolWriteLock.unlock();
		}
	}
	
	protected SpooledQueueWorker scheduleQueueWorker(QueueImpl queue, long wakeUpTime)
	{
		return this.spooledQueueWorkerScheduler.scheduleQueueWorker(queue, wakeUpTime);
	}
	
	public void executeOnJobTimeOut(IOnJobTimeout controller, IQueueJob job)
	{
		try
		{
			this.executorService.submit(new Callable<IQueueJob>()
			{
				@Override
				public IQueueJob call()
				{
					try
					{
						controller.onJobTimeout(job);
					}
					catch (Exception e) {}
					return job;
				}
			}).get(7, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
	}
	
	public void executeOnJobStopExecuter(QueueWorker worker, IQueueJob job)
	{
		this.executorService.execute(new Runnable()
		{
			@Override
			@SuppressWarnings("deprecation")
			public void run()
			{
				if(worker.isAlive())
				{
					if(job instanceof IOnJobStop)
					{
						long number = 0L;
						long moreTimeUntilNow = 0L;
						long moreTime;
						
						while(worker.isAlive() && ((moreTime = ((IOnJobStop)job).requestForMoreLifeTime(number, moreTimeUntilNow, worker.getWorkerWrapper())) > 0) )
						{
							try
							{
								Thread.sleep(moreTime);
							}
							catch (Exception e) {}
							catch (Error e) {}
							number++;
							moreTimeUntilNow += moreTime;
						}
					}
					
				}
				
				if(worker.isAlive())
				{
					try
					{
						worker.interrupt();
					}
					catch (Exception e) {}
					catch (Error e) {}
					
					try
					{
						Thread.sleep(13);
					}
					catch (Exception e) {}
					catch (Error e) {}
					
					if(worker.isAlive())
					{
					
						try
						{
							log(LogService.LOG_WARNING,"stop worker " + worker.getName(),null);
							worker.stop();
						}
						catch (Exception e) {log(LogService.LOG_WARNING,"stop worker " + worker.getName(),e);}
						catch (Error e) {log(LogService.LOG_WARNING,"stop worker " + worker.getName(),e);}
					}
				}
			}
		});
	}
	
	public void executeOnQueueReverse(IOnQueueReverse onQueueReverse , IQueue queue)
	{
		try
		{
			this.executorService.submit(new Callable<IQueue>()
			{
				@Override
				public IQueue call()
				{
					try
					{
						onQueueReverse.onQueueReverse(queue);
					}
					catch (Exception e) {}
					return queue;
				}
			}).get(3, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
	}
	
	public Future<IScheduleResult> createFutureOfScheduleResult(ScheduleResultImpl scheduleResult)
	{
		Callable<IScheduleResult> call = new Callable<IScheduleResult>()
		{
			@Override
			public IScheduleResult call() throws Exception
			{
				scheduleResult.waitForScheduledIsFinished();
				return scheduleResult;
			}
			
		};
		return this.executorService.submit(call);
	}
	
	
	public void onConfigurationModify(QueueImpl queue)
	{
		try
		{
			getMetrics().meter(IMetrics.METRICS_ON_CONFIGURATION_MODIFY).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric on-configuration-modify", e);
		}
		
		ITimer.Context timerContext = null;
		try
		{
			timerContext = getMetrics().timer(IMetrics.METRICS_ON_CONFIGURATION_MODIFY).time();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "metric timer on-configuration-modify", e);
		}
		
		QueueBindingModifyFlags modifyFlags = new QueueBindingModifyFlags();
		
		try
		{
			controllerListReadLock.lock();
			try
			{	
				for(ControllerContainer controllerContainer : controllerList)
				{
					modifyFlags.reset();
					
					try
					{
						queue.checkForController(controllerContainer,modifyFlags);
					}
					catch (Exception e) 
					{
						log(LogService.LOG_ERROR,"check queue binding for controller by configuration filter on queue configuration modify",e);
					}
				}
				
				try
				{
					getMetrics().histogram(IMetrics.METRICS_ON_CONFIGURATION_MODIFY,IMetrics.METRICS_EVENT_CONTROLLER,IMetrics.METRICS_MATCH_CHECK).update(controllerList.size());
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "metric histogram on-configuration-modify", e);
				}
			}
			finally 
			{
				controllerListReadLock.unlock();
			}
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"check queue binding for controller by configuration filter on queue configuration modify",e);
		}
		
		try
		{
			serviceListReadLock.lock();
			try
			{
				for(ServiceContainer serviceContainer : serviceList)
				{
					modifyFlags.reset();
					try
					{
						queue.checkForService(serviceContainer,modifyFlags);
					}
					catch (Exception e) 
					{
						log(LogService.LOG_ERROR,"check queue binding for services by configuration filter on queue configuration modify",e);
					}
				}
				
				try
				{
					getMetrics().histogram(IMetrics.METRICS_ON_CONFIGURATION_MODIFY,IMetrics.METRICS_SERVICE,IMetrics.METRICS_MATCH_CHECK).update(serviceList.size());
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "metric histogram on-configuration-modify", e);
				}
			}
			finally 
			{
				serviceListReadLock.unlock();
			}
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"check queue binding for services by configuration filter on queue configuration modify",e);
		}
		
		if(timerContext != null)
		{
			try {timerContext.stop();}catch (Exception e) {}
		}
	}
}
