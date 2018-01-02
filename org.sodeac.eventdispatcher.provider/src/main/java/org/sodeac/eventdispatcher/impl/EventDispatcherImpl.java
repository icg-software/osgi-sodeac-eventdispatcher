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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
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
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;
import org.sodeac.eventdispatcher.extension.api.IExtensibleEventDispatcher;
import org.sodeac.eventdispatcher.api.ICounter;
import org.sodeac.eventdispatcher.api.IDisableMetricsOnQueueObserve;
import org.sodeac.eventdispatcher.api.IEnableMetricsOnQueueObserve;
import org.sodeac.eventdispatcher.api.IEventController;

import com.codahale.metrics.MetricRegistry;

@Component(name="EventDispatcherProvider" ,service=IEventDispatcher.class,immediate=true,property={IEventDispatcher.PROPERTY_ID + "=" + IEventDispatcher.DEFAULT_DISPATCHER_ID})
public class EventDispatcherImpl implements IEventDispatcher,IExtensibleEventDispatcher
{
	private Map<String,QueueImpl> queueIndex;
	private ReentrantReadWriteLock queueIndexLock;
	private ReadLock queueIndexReadLock;
	private WriteLock queueIndexWriteLock;
	
	private List<QueueWorker> workerPool;
	private ReentrantReadWriteLock workerPoolLock;
	private ReadLock workerPoolReadLock;
	private WriteLock workerPoolWriteLock;
	
	private DispatcherGuardian dispatcherGuardian;
	private SpooledQueueWorkerScheduler spooledQueueWorkerScheduler;
	
	private List<ControllerContainer> controllerList = null;
	private volatile List<ControllerContainer> controllerListCopy =  null;
	private ReentrantLock controllerListLock =  null;
	
	private List<ServiceContainer> serviceList = null; 
	private ReentrantLock serviceListListLock =  null;
	
	private String id = IEventDispatcher.DEFAULT_DISPATCHER_ID;
	
	private volatile ComponentContext context = null;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile LogService logService = null;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY,policy=ReferencePolicy.STATIC)
	protected volatile EventAdmin eventAdmin;
	
	protected volatile MetricRegistry metricRegistry = new  MetricRegistry();
	private List<ServiceRegistration<?>> registrationList = new ArrayList<ServiceRegistration<?>>();
	
	private volatile MetricImpl metrics = null; 
	
	private List<IEventDispatcherExtension>  eventDispatcherExtensionList = null;
	private ReentrantLock eventDispatcherExtensionLock = null;
	private volatile List<IEventDispatcherExtension>  eventDispatcherExtensionListCopy = null;
	
	private Map<String,Long> queueIsMissingLogIndex = null;
	private ReentrantLock queueIsMissingLogIndexLock = null;
	
	private ExecutorService onJobTimeOutExecuterService = null;
	private ExecutorService onJobStopExecuterService = null;
	
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
		
		this.controllerList = new ArrayList<ControllerContainer>();
		this.controllerListCopy = Collections.unmodifiableList( new ArrayList<ControllerContainer>());
		this.controllerListLock = new ReentrantLock();
		this.eventDispatcherExtensionList = new ArrayList<IEventDispatcherExtension>();
		this.eventDispatcherExtensionListCopy = Collections.unmodifiableList(new ArrayList<IEventDispatcherExtension>());
		this.eventDispatcherExtensionLock = new ReentrantLock();
		
		this.workerPool = new ArrayList<QueueWorker>();
		this.workerPoolLock = new ReentrantReadWriteLock(true);
		this.workerPoolReadLock = this.workerPoolLock.readLock();
		this.workerPoolWriteLock = this.workerPoolLock.writeLock();
		
		this.queueIsMissingLogIndex = new HashMap<String,Long>();
		this.queueIsMissingLogIndexLock = new ReentrantLock();
		
		this.serviceList = new ArrayList<ServiceContainer>();
		this.serviceListListLock = new ReentrantLock();
		this.metrics = new MetricImpl(this, new PropertyBlockImpl(this),true);
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
			
			boolean log = false;
			this.queueIsMissingLogIndexLock.lock();
			try
			{
				if(( this.queueIsMissingLogIndex.get(queueId) == null) || (this.queueIsMissingLogIndex.get(queueId).longValue() < (System.currentTimeMillis() - 60000L)))
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
				if(this.logService != null)
				{
					this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "Queue is missing " + queueId);
				}
				else
				{
					System.err.println("Queue is missing " + queueId);
				}
			}
			return false; 
		}
		
		return queue.scheduleEvent(event);
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
		
		if((properties.get(IEventDispatcher.PROPERTY_ID) != null) && (properties.get(IEventDispatcher.PROPERTY_ID).toString().length() > 0))
		{
			this.id = properties.get(IEventDispatcher.PROPERTY_ID).toString();
		}
		
		for(IEventDispatcherExtension eventDispatcherExtension : this.eventDispatcherExtensionListCopy)
		{
			try
			{
				eventDispatcherExtension.registerEventDispatcher(this);
				this.metrics.registerOnExtension(eventDispatcherExtension);
			}
			catch (Exception e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Exception on register dispatcher to extension", e);
				}
				else
				{
					System.err.println("Exception on register dispatcher to extension " + e.getMessage());
				}
			}
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
		
		
		
		controllerListLock.lock();
		try
		{
			counterConfigurationSize = metrics.counter( IMetrics.METRICS_EVENT_CONTROLLER);
			
			if(! this.controllerList.isEmpty())
			{
				counterConfigurationSize.inc(this.controllerList.size());
			}
		}
		finally 
		{
			controllerListLock.unlock();	
		}
		
		this.onJobTimeOutExecuterService = Executors.newCachedThreadPool();
		this.onJobStopExecuterService = Executors.newCachedThreadPool();
		
		this.dispatcherGuardian = new DispatcherGuardian(this);
		this.dispatcherGuardian.start();
		
		this.spooledQueueWorkerScheduler = new SpooledQueueWorkerScheduler(this);
		this.spooledQueueWorkerScheduler.start();
		
		for(ControllerContainer controllerContainer : controllerListCopy)
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
		metricRegistryProperties.put(Constants.SERVICE_DESCRIPTION, "Sodeac Metrics Registry for EventDispatcher");
		metricRegistryProperties.put(Constants.SERVICE_VENDOR, "Sodeac Framework");
		metricRegistryProperties.put("name", "sodeac-eventdispatcher-" + this.id); 
		registrationList.add(context.getBundleContext().registerService(MetricRegistry.class.getName(), metricRegistry, metricRegistryProperties));
		
	}
	
	@Deactivate
	private void deactivate(ComponentContext context)
	{
		for (ServiceRegistration<?> reg : registrationList) 
		{
            reg.unregister();
		}
		
		for(ControllerContainer controllerContainer : controllerListCopy)
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
		try
		{
			this.metrics.dispose();
		}
		catch (Exception e) {e.printStackTrace();}
		
		for(IEventDispatcherExtension eventDispatcherExtension : this.eventDispatcherExtensionListCopy)
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
		
		try
		{
			this.onJobTimeOutExecuterService.shutdown();
			this.onJobTimeOutExecuterService.awaitTermination(3, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		try
		{
			this.onJobStopExecuterService.shutdown();
			this.onJobStopExecuterService.awaitTermination(3, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		this.context = null;
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindEventDispatcherExtension(IEventDispatcherExtension eventDispatcherExtension)
	{
		this.eventDispatcherExtensionLock.lock();
		try
		{
			boolean alreadyConnected = false;
			for(IEventDispatcherExtension extension : this.eventDispatcherExtensionList)
			{
				if(extension == eventDispatcherExtension)
				{
					alreadyConnected = true;
					break;
				}
			}
			if(! alreadyConnected)
			{
				this.eventDispatcherExtensionList.add(eventDispatcherExtension);
				this.eventDispatcherExtensionListCopy = Collections.unmodifiableList(new ArrayList<IEventDispatcherExtension>(this.eventDispatcherExtensionList));
			}
			else
			{
				return;
			}
			
			this.registerDispatcherExtension(eventDispatcherExtension);
		}
		finally 
		{
			this.eventDispatcherExtensionLock.unlock();
		}
	}
	
	public void registerDispatcherExtension(IEventDispatcherExtension eventDispatcherExtension)
	{
		if(this.context == null)
		{
			return;
		}
		
		List<QueueImpl> queueListCopy = new ArrayList<QueueImpl>();
		
		this.queueIndexReadLock.lock();
		try
		{
			for(Entry<String,QueueImpl> entry :  this.queueIndex.entrySet())
			{
				queueListCopy.add(entry.getValue());
			}
		}
		finally 
		{
			this.queueIndexReadLock.unlock();
		}
		
		try
		{
			eventDispatcherExtension.registerEventDispatcher(this);
			this.metrics.registerOnExtension(eventDispatcherExtension);
			
		}
		catch (Exception e) 
		{
			if(logService != null)
			{
				logService.log(context.getServiceReference(), LogService.LOG_ERROR, "Exception on register dispatcher to extension", e);
			}
			else
			{
				System.err.println("Exception on register dispatcher to extension " + e.getMessage());
			}
		}
		
		
		controllerListLock.lock();
		try
		{
			for(ControllerContainer controllerContainer : controllerList)
			{
				eventDispatcherExtension.registerEventController(this,controllerContainer.getEventController(), controllerContainer.getProperties());
			}
		}
		finally 
		{
			controllerListLock.unlock();
		}
		
		for(QueueImpl queue : queueListCopy)
		{
			eventDispatcherExtension.registerEventQueue(this,queue);
			queue.registerOnExtension(eventDispatcherExtension);
		}
	}

	public void unbindEventDispatcherExtension(IEventDispatcherExtension eventDispatcherExtension)
	{
		this.eventDispatcherExtensionLock.lock();
		try
		{
			if(this.context != null)
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
			
			while(this.eventDispatcherExtensionList.remove(eventDispatcherExtension)) {}
			this.eventDispatcherExtensionListCopy = Collections.unmodifiableList(new ArrayList<IEventDispatcherExtension>(this.eventDispatcherExtensionList));
		}
		finally 
		{
			this.eventDispatcherExtensionLock.unlock();
		}
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindEventController(IEventController eventController,Map<String, ?> properties)
	{
		String dispatcherId = IEventDispatcher.DEFAULT_DISPATCHER_ID;
		if((properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) != null) && (properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString().length() > 0))
		{
			dispatcherId = properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString();
		}
		if(! dispatcherId.equals(this.id))
		{
			return;
		}
		
		controllerListLock.lock();
		try
		{
			ControllerContainer controllerContainer = new ControllerContainer();
			controllerContainer.setEventController(eventController);
			controllerContainer.setProperties(properties);
			
			this.controllerList.add(controllerContainer);
			this.controllerListCopy = Collections.unmodifiableList( new ArrayList<ControllerContainer>(this.controllerList));
			
			if(this.counterConfigurationSize != null)
			{
				this.counterConfigurationSize.inc();
			}
			
			for(IEventDispatcherExtension extension : this.eventDispatcherExtensionListCopy)
			{
				try
				{
					extension.registerEventController(this,eventController, properties);
				}
				catch (Exception e) 
				{
					if(logService != null)
					{
						logService.log(context.getServiceReference(), LogService.LOG_ERROR, "register eventcontroller to extension", e);
					}
					else
					{
						System.err.println("Exception on register eventcontroller to extension" + e.getMessage());
					}
				}
			}
		}
		finally 
		{
			controllerListLock.unlock();
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
		
		String queueConfigurationFilter = (String)properties.get(IEventDispatcher.PROPERTY_QUEUE_CONFIGURATION_FILTER);
		String queueId = (String)properties.get(IEventDispatcher.PROPERTY_QUEUE_ID);
		if( ((queueId == null) || queueId.isEmpty()) && ((queueConfigurationFilter == null) || queueConfigurationFilter.isEmpty()) )
		{
			if(this.logService != null)
			{
				this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_WARNING, "Missing QueueId or ConfigurationFilter");
			}
			return false;
		}
		
		boolean enableMetrics = (eventController instanceof IEnableMetricsOnQueueObserve);
		boolean disableMetrics = (eventController instanceof IDisableMetricsOnQueueObserve);
		if(! disableMetrics)
		{
			Object disableMetricsProperty = properties.get(IEventController.PROPERTY_DISABLE_METRICS);
			if(disableMetricsProperty != null)
			{
				if(disableMetricsProperty instanceof Boolean)
				{
					disableMetrics = (Boolean)disableMetricsProperty;
				}
				else if (disableMetricsProperty instanceof String)
				{
					disableMetrics = ((String)disableMetricsProperty).equalsIgnoreCase("true");
				}
				else
				{
					disableMetrics = disableMetricsProperty.toString().equalsIgnoreCase("true");
				}
			}
		}
		if(disableMetrics)
		{
			enableMetrics = false;
		}
		
		Map<QueueImpl,QueueImpl> relatedQueueIndex = new HashMap<QueueImpl,QueueImpl>();
		QueueImpl queue = null;
		
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
						String name  = eventController.getClass().getSimpleName();
						if((properties.get(IEventController.PROPERTY_JMX_NAME) != null) && (! properties.get(IEventController.PROPERTY_JMX_NAME).toString().isEmpty()))
						{
							name = properties.get(IEventController.PROPERTY_JMX_NAME).toString();
						}
						
						String category = null;
						if((properties.get(IEventController.PROPERTY_JMX_CATEGORY) != null) && (! properties.get(IEventController.PROPERTY_JMX_CATEGORY).toString().isEmpty()))
						{
							category = properties.get(IEventController.PROPERTY_JMX_CATEGORY).toString();
						}
						
						
						queue = new QueueImpl(queueId,this, ! disableMetrics, name,category);
						this.queueIndex.put(queueId,queue);
						if(this.counterQueueSize != null)
						{
							this.counterQueueSize.inc();
						}
						
						for(IEventDispatcherExtension extension : this.eventDispatcherExtensionListCopy)
						{
							try
							{
								extension.registerEventQueue(this, queue);
								queue.registerOnExtension(extension);
							}
							catch (Exception e) 
							{
								if(this.logService != null)
								{
									this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "register new queue to extension");
								}
								else
								{
									System.err.println("register new queue to extension");
									e.printStackTrace();
								}
							}
						}
						
						serviceListListLock.lock();
						try
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
						finally 
						{
							serviceListListLock.unlock();
						}
					}
				}
				finally 
				{
					this.queueIndexWriteLock.unlock();
				}
				
				relatedQueueIndex.put(queue, queue);
			}
		}
		
		if((queueConfigurationFilter != null) && (! queueConfigurationFilter.isEmpty()))
		{
			try
			{
				Filter filter = this.context.getBundleContext().createFilter(queueConfigurationFilter);
				this.queueIndexReadLock.lock();
				try
				{
					for(Entry<String,QueueImpl> entry : queueIndex.entrySet())
					{
						if(relatedQueueIndex.containsKey(entry.getValue()))
						{
							continue;
						}
						if(entry.getValue().getConfigurationPropertyBlock().isEmpty())
						{
							continue;
						}
						if(filter.matches(entry.getValue().getConfigurationPropertyBlock().getProperties()))
						{
							relatedQueueIndex.put(entry.getValue(), entry.getValue());
						}
						
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
		
		for(Entry<QueueImpl, QueueImpl> queueEntry : relatedQueueIndex.entrySet())
		{
			QueueImpl relatedQueue = queueEntry.getKey();
			
			relatedQueue.addConfiguration(eventController, properties);
			
			if(disableMetrics)
			{
				try
				{
					relatedQueue.setMetricsEnabled(false);
				}
				catch (Exception e) 
				{
					if(logService != null)
					{
						logService.log(context.getServiceReference(), LogService.LOG_ERROR, "queue.setMetricsEnabled(false)", e);
					}
					else
					{
						System.err.println("queue.setMetricsEnabled(false) " + e.getMessage());
					}
				}
			}
			else if(enableMetrics)
			{
				try
				{
					relatedQueue.setMetricsEnabled(true);
				}
				catch (Exception e) 
				{
					if(logService != null)
					{
						logService.log(context.getServiceReference(), LogService.LOG_ERROR, "queue.setMetricsEnabled(true)", e);
					}
					else
					{
						System.err.println("queue.setMetricsEnabled(true) " + e.getMessage());
					}
				}
			}
			
			if(eventController instanceof IOnQueueObserve)
			{
				relatedQueue.addOnQueueObserver((IOnQueueObserve)eventController);
			}
		}
		
		return queueIndex.size() > 0;
	}
	
	public void unbindEventController(IEventController eventController,Map<String, ?> properties)
	{
		this.controllerListLock.lock();
		try
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
			this.controllerListCopy = Collections.unmodifiableList( new ArrayList<ControllerContainer>(this.controllerList));
		}
		finally 
		{
			controllerListLock.unlock();
		}
		
		if(this.context != null)
		{
			this.unregisterEventController(eventController);
		}
		for(IEventDispatcherExtension extension : this.eventDispatcherExtensionListCopy)
		{
			try
			{
				extension.unregisterEventController(this,eventController);
			}
			catch (Exception e) 
			{
				if(logService != null)
				{
					logService.log(context.getServiceReference(), LogService.LOG_ERROR, "unregister eventcontroller from extension", e);
				}
				else
				{
					System.err.println("Exception on unregister eventcontroller from extension" + e.getMessage());
				}
			}
		}
	}
	
	private boolean unregisterEventController(IEventController eventController)
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
				if(entry.getValue().removeConfiguration(eventController))
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
		
		if(eventController instanceof IOnQueueReverse)
		{
			try
			{
				if(registeredOnQueueList != null)
				{
					for(QueueImpl queue : registeredOnQueueList)
					{	
						((IOnQueueReverse)eventController).onQueueReverse(queue);
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
					for(IEventDispatcherExtension extension : this.eventDispatcherExtensionListCopy)
					{
						try
						{
							extension.unregisterEventQueue(this, queue);
						}
						catch (Exception e) 
						{
							if(this.logService != null)
							{
								this.logService.log(this.context == null ? null : this.context.getServiceReference(), LogService.LOG_ERROR, "register new queue to extension");
							}
							else
							{
								System.err.println("unregister new queue to extension");
								e.printStackTrace();
							}
						}
					}
					
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
		String dispatcherId = IEventDispatcher.DEFAULT_DISPATCHER_ID;
		if((properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) != null) && (properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString().length() > 0))
		{
			dispatcherId = properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID).toString();
		}
		if(! dispatcherId.equals(this.id))
		{
			return;
		}
		
		serviceListListLock.lock();
		try
		{
			ServiceContainer serviceContainer = new ServiceContainer();
			serviceContainer.setQueueService(queueService);
			serviceContainer.setProperties(properties);
			
			this.serviceList.add(serviceContainer);
			
			if(this.context != null)
			{
				registerQueueService(queueService, properties);
			}
			
		}
		finally 
		{
			serviceListListLock.unlock();
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
		this.serviceListListLock.lock();
		try
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
			
			if(this.context != null)
			{
				this.unregisterQueueService(queueService);
			}
		}
		finally 
		{
			this.serviceListListLock.unlock();
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
				entry.getValue().removeService(queueService);
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
		return eventDispatcherExtensionListCopy;
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
		CountDownLatch countDownLatch = new CountDownLatch(1);
		
		this.onJobTimeOutExecuterService.execute(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					controller.onJobTimeout(job);
				}
				catch (Exception e) {}
				countDownLatch.countDown();
			}
		});
		
		try
		{
			countDownLatch.await(7, TimeUnit.SECONDS);
		}
		catch (Exception ie) {}
	}
	
	public void executeOnJobStopExecuter(QueueWorker worker, IQueueJob job)
	{
		this.onJobStopExecuterService.execute(new Runnable()
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
	
	public void onConfigurationModify(QueueImpl queue)
	{
		try
		{
			
			Map<IEventController,ControllerContainer> controllerIndex = new HashMap<IEventController,ControllerContainer>();
			for(ControllerContainer controllerContainer : queue.getConfigurationList())
			{
				controllerIndex.put(controllerContainer.getEventController(),controllerContainer);
			}
			
			for(ControllerContainer controllerContainer : controllerListCopy)
			{
				String queueConfigurationFilter = (String)controllerContainer.getProperties().get(IEventDispatcher.PROPERTY_QUEUE_CONFIGURATION_FILTER);
				if(queueConfigurationFilter == null)
				{
					continue;
				}
				
				if(queueConfigurationFilter.isEmpty())
				{
					continue;
				}
				
				String queueId = (String)controllerContainer.getProperties().get(IEventDispatcher.PROPERTY_QUEUE_ID);
				if((queueId != null) && (!queueId.isEmpty()) && (queueId.equals(queue.getQueueId())))
				{
					// Observe by QueueId
					continue;
				}
						
				Filter filter = this.context.getBundleContext().createFilter(queueConfigurationFilter);
				
				try
				{
					if((! queue.getConfigurationPropertyBlock().isEmpty()) && filter.matches(queue.getConfigurationPropertyBlock().getProperties()))
					{
						if(controllerIndex.get(controllerContainer.getEventController()) == null)
						{
							ControllerContainer container = queue.addConfiguration(controllerContainer.getEventController(),controllerContainer.getProperties());
							if(container != null)
							{
								if(controllerContainer.getEventController() instanceof IOnQueueObserve)
								{
									queue.addOnQueueObserver((IOnQueueObserve)controllerContainer.getEventController());
								}
								controllerIndex.put(controllerContainer.getEventController(),controllerContainer);
							}
						}
					}
					else
					{
						if(controllerIndex.get(controllerContainer.getEventController()) != null)
						{
							
							queue.removeConfiguration(controllerContainer.getEventController());
							
							if(controllerContainer.getEventController() instanceof IOnQueueReverse)
							{
								((IOnQueueReverse)controllerContainer.getEventController()).onQueueReverse(queue);
							}
							controllerIndex.remove(controllerContainer.getEventController());
						}
					}
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"check queue binding for controller by configuration filter on queue configuration modify",e);
				}
			}
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"check queue binding for controller by configuration filter on queue configuration modify",e);
		}
	}
}
