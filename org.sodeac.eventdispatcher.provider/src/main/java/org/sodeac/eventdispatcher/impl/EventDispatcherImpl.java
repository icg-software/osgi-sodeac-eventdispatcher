/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
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
import org.sodeac.eventdispatcher.api.IOnTaskStop;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueComponentConfigurable;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;
import org.sodeac.eventdispatcher.api.IQueueEventResult;
import org.sodeac.eventdispatcher.api.ITimer;
import org.sodeac.eventdispatcher.api.MetricsRequirement;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.eventdispatcher.api.QueueNotFoundException;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration.BoundedByQueueConfiguration;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration.BoundedByQueueId;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;
import org.sodeac.eventdispatcher.extension.api.IExtensibleEventDispatcher;
import org.sodeac.eventdispatcher.api.EventType;
import org.sodeac.eventdispatcher.api.ICounter;
import org.sodeac.eventdispatcher.api.IQueueController;

import com.codahale.metrics.MetricRegistry;

@Component(name="EventDispatcherProvider" ,service=IEventDispatcher.class,property={IEventDispatcher.PROPERTY_ID + "=" + IEventDispatcher.DEFAULT_DISPATCHER_ID})
public class EventDispatcherImpl implements IEventDispatcher,IExtensibleEventDispatcher
{
	private Map<String,QueueImpl> queueIndex;
	private ReentrantReadWriteLock queueIndexLock;
	private ReadLock queueIndexReadLock;
	private WriteLock queueIndexWriteLock;
	
	private ReentrantReadWriteLock osgiLifecycleLock;
	private ReadLock osgiLifecycleReadLock;
	private WriteLock osgiLifecycleWriteLock;
	
	private LinkedList<QueueWorker> workerPool;
	private ReentrantReadWriteLock workerPoolLock;
	private ReadLock workerPoolReadLock;
	private WriteLock workerPoolWriteLock;
	
	private DispatcherGuardian dispatcherGuardian;
	private SpooledQueueWorkerScheduler spooledQueueWorkerScheduler;
	
	private List<ControllerContainer> controllerList = null;
	private Map<IQueueController,ControllerContainer> controllerReverseIndex = null; 
	private List<ScheduledController> controllerListScheduled = null;
	private ReentrantReadWriteLock controllerListLock;
	private ReadLock controllerListReadLock;
	private WriteLock controllerListWriteLock;
	
	private List<ServiceContainer> serviceList = null;
	private Map<IQueueService ,ServiceContainer> serviceReverseIndex = null; 
	private List<ScheduledService> serviceListScheduled = null;
	private ReentrantReadWriteLock serviceListLock;
	private ReadLock serviceListReadLock;
	private WriteLock serviceListWriteLock;
	
	private PropertyBlockImpl propertyBlock;
	
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
	
	private ConfigurationPropertyBindingRegistry configurationPropertyBindingRegistry = null;
	
	@Override
	public String getId()
	{
		return this.context == null ? null : this.id;
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
		this.controllerListScheduled = new ArrayList<ScheduledController>();
		this.controllerReverseIndex = new HashMap<IQueueController,ControllerContainer>();
		this.controllerListLock = new ReentrantReadWriteLock(true);
		this.controllerListReadLock = this.controllerListLock.readLock();
		this.controllerListWriteLock = this.controllerListLock.writeLock();
		
		this.extensionList = new ArrayList<IEventDispatcherExtension>();
		this.extensionListCopy = Collections.unmodifiableList(new ArrayList<IEventDispatcherExtension>());
		this.extensionListScheduled  = new ArrayList<IEventDispatcherExtension>();
		this.extensionListLock = new ReentrantReadWriteLock(true);
		this.extensionListReadLock = extensionListLock.readLock();
		this.extensionListWriteLock = extensionListLock.writeLock();
		
		this.workerPool = new LinkedList<QueueWorker>();
		this.workerPoolLock = new ReentrantReadWriteLock(true);
		this.workerPoolReadLock = this.workerPoolLock.readLock();
		this.workerPoolWriteLock = this.workerPoolLock.writeLock();
		
		this.queueIsMissingLogIndex = new HashMap<String,Long>();
		this.queueIsMissingLogIndexLock = new ReentrantLock();
		
		this.serviceList = new ArrayList<ServiceContainer>();
		this.serviceReverseIndex = new HashMap<IQueueService ,ServiceContainer>();
		this.serviceListScheduled = new ArrayList<ScheduledService>();
		this.serviceListLock = new ReentrantReadWriteLock(true);
		this.serviceListReadLock = this.serviceListLock.readLock();
		this.serviceListWriteLock = this.serviceListLock.writeLock();
		
		this.metrics = new MetricImpl(this, new PropertyBlockImpl(this),true);
		this.propertyBlock = createPropertyBlock();
		this.configurationPropertyBindingRegistry = new ConfigurationPropertyBindingRegistry();

	}
	
	@Override
	public Future<IQueueEventResult> queueEvent(String queueId, Event event)
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
			
			return queue.queueEvent(event);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
	}
	
	@Override
	public Future<IQueueEventResult> queueEventList(String queueId, List<Event> eventList)
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
			
			return queue.queueEventList(eventList);
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
	
	public void registerTimeOut(QueueImpl queue, TaskContainer taskContainer)
	{
		this.dispatcherGuardian.registerTimeOut(queue,taskContainer);
	}
	
	public void unregisterTimeOut(QueueImpl queue, TaskContainer taskContainer)
	{
		this.dispatcherGuardian.unregisterTimeOut(queue,taskContainer);
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
			try
			{
				this.bindEventDispatcherExtension(eventDispatcherExtension);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR, "Exception bind scheduled extension", e);
			}
		}
		
		this.extensionListScheduled.clear();
		this.extensionListScheduled = null;
		
		for(ScheduledService schedulesService : this.serviceListScheduled)
		{
			try
			{
				this.bindQueueService(schedulesService.queueService, schedulesService.serviceReference, schedulesService.properties);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR, "Exception bind scheduled queue service", e);
			}
		}
		this.serviceListScheduled.clear();
		this.serviceListScheduled = null;
		
		for(ScheduledController container : this.controllerListScheduled)
		{
			try
			{
				this.bindQueueController(container.queueController, container.serviceReference, container.properties);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR, "Exception bind scheduled queue controller", e);
			}
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
		
		try
		{
			this.configurationPropertyBindingRegistry.clear();
		}
		catch (Exception e) {}
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
				eventDispatcherExtension.registerEventController(this,controllerContainer.getQueueController(), controllerContainer.getProperties());
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
	public void bindQueueController(IQueueController queueController,ServiceReference<IQueueController> serviceReference, Map<String, ?> properties)
	{
		IQueueComponentConfigurable configurable = null;
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
					
					ScheduledController controllerContainer = new ScheduledController();
					controllerContainer.queueController = queueController;
					controllerContainer.serviceReference = serviceReference;
					controllerContainer.properties = properties;
					
					this.controllerListScheduled.add(controllerContainer);
				}
				finally 
				{
					controllerListWriteLock.unlock();
				}
				return;
			}
			
			if(queueController instanceof IQueueComponentConfigurable)
			{
				configurable = (IQueueComponentConfigurable)queueController;
			}
			else
			{
				configurable = new IQueueComponentConfigurable()
				{
					private List<QueueComponentConfiguration> controllerConfigurationList = new ArrayList<QueueComponentConfiguration>();
					private List<QueueComponentConfiguration> serviceConfigurationList = new ArrayList<QueueComponentConfiguration>();
					
					@Override
					public List<QueueComponentConfiguration> configureQueueService()
					{
						return this.serviceConfigurationList;
					}
					
					@Override
					public List<QueueComponentConfiguration> configureQueueController()
					{
						return this.controllerConfigurationList;
					}
				};
				
				List<QueueComponentConfiguration> controllerConfigurationList = configurable.configureQueueController();
				
				String eventDispatcherId = IEventDispatcher.DEFAULT_DISPATCHER_ID;
				if
				(
					(properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) != null) && 
					(properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) instanceof String) && 
					(!((String)properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID)).isEmpty())
				)
				{
					eventDispatcherId = (String)properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID);
				}
				
				List<String> queueIdList = getQueueIdsFromServiceProperties(properties);
				List<String> queueConfigurationFilterList = getQueueConfigurationFilterListFromServiceProperties(properties);
				Boolean dispableMetrics = getDisableMetricsFromServiceProperties(properties);
				String name = properties.get(IQueueController.PROPERTY_JMX_NAME) instanceof String ? (String)properties.get(IQueueController.PROPERTY_JMX_NAME) : null;
				String category = properties.get(IQueueController.PROPERTY_JMX_CATEGORY) instanceof String ? (String)properties.get(IQueueController.PROPERTY_JMX_CATEGORY) : null;
				if(queueIdList != null)
				{
					for(String queueId : queueIdList)
					{
						if((queueId == null) || (queueId.isEmpty()))
						{
							continue;
						}
						QueueComponentConfiguration.BoundedByQueueId boundedByQueueId = new QueueComponentConfiguration.BoundedByQueueId(queueId)
								.setDispatcherId(eventDispatcherId)
								.setName(name)
								.setCategory(category)
								.setAutoCreateQueue(true);
						
						if(dispableMetrics != null)
						{
							boundedByQueueId.setQueueMetricsRequirement(dispableMetrics.booleanValue() ? MetricsRequirement.PreferNoMetrics : MetricsRequirement.PreferMetrics);
						}
						
						controllerConfigurationList.add(boundedByQueueId);
					}
				}
				if(queueConfigurationFilterList != null)
				{
					for(String queueConfigurationFilter : queueConfigurationFilterList)
					{
						if((queueConfigurationFilter == null) || (queueConfigurationFilter.isEmpty()))
						{
							continue;
						}
						QueueComponentConfiguration.BoundedByQueueConfiguration boundedByQueueId = new QueueComponentConfiguration.BoundedByQueueConfiguration(queueConfigurationFilter)
								.setDispatcherId(eventDispatcherId)
								.setName(name)
								.setCategory(category);
						
						if(dispableMetrics != null)
						{
							boundedByQueueId.setQueueMetricsRequirement(dispableMetrics.booleanValue() ? MetricsRequirement.PreferNoMetrics : MetricsRequirement.PreferMetrics);
						}
						
						controllerConfigurationList.add(boundedByQueueId);
					}
				}
				
				if(properties.get(IQueueController.PROPERTY_CONSUME_EVENT_TOPIC) != null)
				{
					if(properties.get(IQueueController.PROPERTY_CONSUME_EVENT_TOPIC) instanceof String)
					{
						String topic = (String)properties.get(IQueueController.PROPERTY_CONSUME_EVENT_TOPIC);
						if(! topic.isEmpty())
						{
							controllerConfigurationList.add(new QueueComponentConfiguration.SubscribeEvent(topic,null,EventType.PublishedByEventAdmin));
						}
					}
					else
					{
						List<String> topicList = new ArrayList<String>();
						if(properties.get(IQueueController.PROPERTY_CONSUME_EVENT_TOPIC) instanceof List)
						{
							for(Object item : (List<?>)properties.get(IQueueController.PROPERTY_CONSUME_EVENT_TOPIC))
							{
								if(item == null)
								{
									continue;
								}
								if(!(item instanceof String))
								{
									continue;
								}
								String topic = (String) item;
								if(topic.isEmpty())
								{
									continue;
								}
								topicList.add(topic);
							}
						}
						
						if(properties.get(IQueueController.PROPERTY_CONSUME_EVENT_TOPIC) instanceof String[])
						{
							for(Object item : (String[])properties.get(IQueueController.PROPERTY_CONSUME_EVENT_TOPIC))
							{
								if(item == null)
								{
									continue;
								}
								if(!(item instanceof String))
								{
									continue;
								}
								String topic = (String) item;
								if(topic.isEmpty())
								{
									continue;
								}
								topicList.add(topic);
							}
						}
						
						for(String topic : topicList)
						{
							if(! topic.isEmpty())
							{
								controllerConfigurationList.add(new QueueComponentConfiguration.SubscribeEvent(topic,null,EventType.PublishedByEventAdmin));
							}
						}
					}
				}
			}
			this.registerQueueControllerLifecycled(queueController, configurable, serviceReference.getBundle(), properties);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
	}
	
	@Override
	public void registerQueueController(IQueueController queueController, IQueueComponentConfigurable configuration, Bundle bundle, Map<String, ?> properties)
	{
		if(configuration == null)
		{
			return;
		}
		
		if(this.context == null)
		{
			return;
		}
		
		osgiLifecycleReadLock.lock();
		try
		{
			registerQueueControllerLifecycled(queueController, configuration, bundle, properties);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
		
	}
	
	private void registerQueueControllerLifecycled(IQueueController queueController, IQueueComponentConfigurable configuration, Bundle bundle, Map<String, ?> properties)
	{
		if(configuration == null)
		{
			return;
		}
		
		if(this.context == null)
		{
			return;
		}
		List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList = null;
		List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList = null;
		List<QueueComponentConfiguration.SubscribeEvent> subscribeEventList = null;
		
		if(configuration.configureQueueController() == null)
		{
			return;
		}
		
		QueueComponentConfiguration.BoundedByQueueId boundedById;
		QueueComponentConfiguration.BoundedByQueueConfiguration boundedByQueueConfiguration;
		QueueComponentConfiguration.SubscribeEvent subscribeEvent;
		for(QueueComponentConfiguration config : configuration.configureQueueController())
		{
			if(config instanceof QueueComponentConfiguration.BoundedByQueueId)
			{
				boundedById = (QueueComponentConfiguration.BoundedByQueueId)config;
				if((boundedById.getDispatcherId() != null) && (! boundedById.getDispatcherId().equals(this.id)))
				{
					continue;
				}
				if(boundByIdList == null)
				{
					boundByIdList = new ArrayList<QueueComponentConfiguration.BoundedByQueueId>();
				}
				boundByIdList.add(boundedById.copy());
			}
			if(config instanceof QueueComponentConfiguration.BoundedByQueueConfiguration)
			{
				boundedByQueueConfiguration = (QueueComponentConfiguration.BoundedByQueueConfiguration)config;
				if((boundedByQueueConfiguration.getDispatcherId() != null) && (! boundedByQueueConfiguration.getDispatcherId().equals(this.id)))
				{
					continue;
				}
				if(boundedByQueueConfigurationList == null)
				{
					boundedByQueueConfigurationList = new ArrayList<QueueComponentConfiguration.BoundedByQueueConfiguration>();
				}
				boundedByQueueConfigurationList.add(boundedByQueueConfiguration.copy());
			}
			if(config instanceof QueueComponentConfiguration.SubscribeEvent)
			{
				subscribeEvent = (QueueComponentConfiguration.SubscribeEvent)config;
				if(subscribeEventList == null)
				{
					subscribeEventList = new ArrayList<QueueComponentConfiguration.SubscribeEvent>();
				}
				subscribeEventList.add(subscribeEvent.copy());
			}
		}
		
		if
		(
			((boundByIdList ==  null) || boundByIdList.isEmpty()) &&
			((boundedByQueueConfigurationList == null) || boundedByQueueConfigurationList.isEmpty())
		)
		{
			return;
		}
		
		ControllerContainer controllerContainer = null;
		
		controllerListWriteLock.lock();
		try
		{
			controllerContainer = this.controllerReverseIndex.get(queueController);
			
			if(controllerContainer == null)
			{
				controllerContainer = new ControllerContainer(this,queueController,properties,boundByIdList, boundedByQueueConfigurationList, subscribeEventList);
				
				this.controllerReverseIndex.put(queueController,controllerContainer);
				this.controllerList.add(controllerContainer);
				
				if(this.counterConfigurationSize != null)
				{
					this.counterConfigurationSize.inc();
				}
				
				this.configurationPropertyBindingRegistry.register(controllerContainer);
			}
			registerQueueController(controllerContainer);
		}
		finally 
		{
			controllerListWriteLock.unlock();
		}
	}
	
	private boolean registerQueueController(ControllerContainer controllerContainer)
	{
		this.extensionListReadLock.lock();
		try
		{
			if(controllerContainer.isRegistered())
			{
				return false;
			}
			
			controllerContainer.setRegistered(true);
			
			
			if
			(
				(
					(controllerContainer.getBoundByIdList() == null) || 
					controllerContainer.getBoundByIdList().isEmpty()
				) && 
				(
					(controllerContainer.getBoundedByQueueConfigurationList() == null) ||
					controllerContainer.getBoundedByQueueConfigurationList().isEmpty()
				)
			)
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
			QueueBindingModifyFlags modifyFlags = new QueueBindingModifyFlags();
			
			if(controllerContainer.getBoundByIdList() != null)
			{
				for(BoundedByQueueId boundedQueueId : controllerContainer.getBoundByIdList())
				{	
					if(boundedQueueId.getQueueId() == null)
					{
						continue;
					}
					if(boundedQueueId.getQueueId().isEmpty())
					{
						continue;
					}
					if((boundedQueueId.getDispatcherId() != null) && (! boundedQueueId.getDispatcherId().equals(this.id)))
					{
						continue;
					}
					
					this.queueIndexReadLock.lock();
					try
					{
						queue = this.queueIndex.get(boundedQueueId.getQueueId());
					}
					finally 
					{
						this.queueIndexReadLock.unlock();
					}
					if(queue != null)
					{
						queue.setController(controllerContainer);
					}
					else if((queue == null) && boundedQueueId.isAutoCreateQueue()) // autocreate
					{
						this.queueIndexWriteLock.lock();
						try
						{
							queue = this.queueIndex.get(boundedQueueId.getQueueId());
						
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
								
								String name  = (boundedQueueId.getName() == null || boundedQueueId.getName().isEmpty() ) ? 
												controllerContainer.getQueueController().getClass().getSimpleName() :
												boundedQueueId.getName();
								String category = (boundedQueueId.getCategory() == null || boundedQueueId.getCategory().isEmpty() ) ?
												null :
												boundedQueueId.getCategory();
								
								queue = new QueueImpl(boundedQueueId.getQueueId(),this, ! controllerContainer.isDisableQueueMetrics(), name,category,null,null);
								this.queueIndex.put(boundedQueueId.getQueueId(),queue);
								
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
					} // end autocreate
				}
			}
		
			if(controllerContainer.getBoundedByQueueConfigurationList() != null)
			{
				for(BoundedByQueueConfiguration boundedByQueueConfiguration : controllerContainer.getBoundedByQueueConfigurationList())
				{
					if(boundedByQueueConfiguration.getLdapFilter() == null)
					{
						continue;
					}
					if(boundedByQueueConfiguration.getLdapFilter().isEmpty())
					{
						continue;
					}
					
					try
					{
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
	
	@Override
	public void unregisterQueueController(IQueueController queueController)
	{
		this.unbindQueueController(queueController);
	}
	
	public void unbindQueueController(IQueueController queueController)
	{
		ControllerContainer controllerContainer = null;
		osgiLifecycleReadLock.lock();
		try
		{
			
			this.controllerListWriteLock.lock();
			try
			{
				controllerContainer = this.controllerReverseIndex.get(queueController);
				
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
				this.controllerReverseIndex.remove(queueController);
				this.configurationPropertyBindingRegistry.unregister(controllerContainer);
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
				extension.unregisterEventController(this,queueController);
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
				if(entry.getValue().unsetController(controllerContainer, true))
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
					
					this.queueIndex.remove(queue.getId());
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
	
	private void checkControllerForQueue(QueueImpl queue)
	{
		if(queue.getControllerSize() > 0)
		{
			return;
		}
		
		if(queue instanceof IQueueSessionScope)
		{
			return;
		}
		
		this.queueIndexWriteLock.lock();
		try
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
				log(LogService.LOG_ERROR,"dispose queue after removed all controller",e);
			}
					
			this.queueIndex.remove(queue.getId());
			counterQueueSize.dec();
			
			if(removeQueueTimerContext != null)
			{
				try {removeQueueTimerContext.stop();}catch (Exception e) {}
			}
		}
		finally 
		{
			this.queueIndexWriteLock.unlock();
		}
	}
	
	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC)
	public void bindQueueService(IQueueService queueService,ServiceReference<IQueueController> serviceReference,Map<String, ?> properties)
	{
		IQueueComponentConfigurable configurable = null;
		osgiLifecycleReadLock.lock();
		try
		{
			if(this.context ==  null)
			{
				// wait for activate, => collect in serviceListScheduled
				
				serviceListWriteLock.lock();
				try
				{
					if(this.serviceListScheduled == null)
					{
						return;
					}
					
					ScheduledService scheduledService = new ScheduledService();
					scheduledService.queueService = queueService;
					scheduledService.properties = properties;
					scheduledService.serviceReference = serviceReference;
					
					this.serviceListScheduled.add(scheduledService);
				}
				finally 
				{
					serviceListWriteLock.unlock();
				}
				return;
			}
			
			if(queueService instanceof IQueueComponentConfigurable)
			{
				configurable = (IQueueComponentConfigurable)queueService;
			}
			else
			{
				configurable = new IQueueComponentConfigurable()
				{
					private List<QueueComponentConfiguration> controllerConfigurationList = new ArrayList<QueueComponentConfiguration>();
					private List<QueueComponentConfiguration> serviceConfigurationList = new ArrayList<QueueComponentConfiguration>();
					
					@Override
					public List<QueueComponentConfiguration> configureQueueService()
					{
						return this.serviceConfigurationList;
					}
					
					@Override
					public List<QueueComponentConfiguration> configureQueueController()
					{
						return this.controllerConfigurationList;
					}
				};
				
				List<QueueComponentConfiguration> serviceConfigurationList = configurable.configureQueueService();
				
				String eventDispatcherId = IEventDispatcher.DEFAULT_DISPATCHER_ID;
				if
				(
					(properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) != null) && 
					(properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID) instanceof String) && 
					(!((String)properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID)).isEmpty())
				)
				{
					eventDispatcherId = (String)properties.get(IEventDispatcher.PROPERTY_DISPATCHER_ID);
				}
				
				List<String> queueIdList = getQueueIdsFromServiceProperties(properties);
				List<String> queueConfigurationFilterList = getQueueConfigurationFilterListFromServiceProperties(properties);
				Boolean dispableMetrics = getDisableMetricsFromServiceProperties(properties);
				String name = properties.get(IQueueController.PROPERTY_JMX_NAME) instanceof String ? (String)properties.get(IQueueController.PROPERTY_JMX_NAME) : null;
				String category = properties.get(IQueueController.PROPERTY_JMX_CATEGORY) instanceof String ? (String)properties.get(IQueueController.PROPERTY_JMX_CATEGORY) : null;
				if(queueIdList != null)
				{
					for(String queueId : queueIdList)
					{
						if((queueId == null) || (queueId.isEmpty()))
						{
							continue;
						}
						QueueComponentConfiguration.BoundedByQueueId boundedByQueueId = new QueueComponentConfiguration.BoundedByQueueId(queueId)
								.setDispatcherId(eventDispatcherId)
								.setName(name)
								.setCategory(category)
								.setAutoCreateQueue(true);
						
						if(dispableMetrics != null)
						{
							boundedByQueueId.setQueueMetricsRequirement(dispableMetrics.booleanValue() ? MetricsRequirement.PreferNoMetrics : MetricsRequirement.PreferMetrics);
						}
						
						serviceConfigurationList.add(boundedByQueueId);
					}
				}
				if(queueConfigurationFilterList != null)
				{
					for(String queueConfigurationFilter : queueConfigurationFilterList)
					{
						if((queueConfigurationFilter == null) || (queueConfigurationFilter.isEmpty()))
						{
							continue;
						}
						QueueComponentConfiguration.BoundedByQueueConfiguration boundedByQueueId = new QueueComponentConfiguration.BoundedByQueueConfiguration(queueConfigurationFilter)
								.setDispatcherId(eventDispatcherId)
								.setName(name)
								.setCategory(category);
						
						if(dispableMetrics != null)
						{
							boundedByQueueId.setQueueMetricsRequirement(dispableMetrics.booleanValue() ? MetricsRequirement.PreferNoMetrics : MetricsRequirement.PreferMetrics);
						}
						
						serviceConfigurationList.add(boundedByQueueId);
					}
				}
				
				String serviceId = null;
				long startDelayMS = 0L;
				long timeOutMS = -1L;
				long hbTimeOutMS = -1L;
				long periodicRepetitionInterval = -1L;
				
				try
				{
					serviceId = (String)properties.get(IQueueService.PROPERTY_SERVICE_ID);
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"problems with serviceid",e);
				}
				
				try
				{
					startDelayMS = getMillis(properties.get(IQueueService.PROPERTY_START_DELAY_MS),startDelayMS);
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"problems with startdelayvalue",e);
				}
				
				try
				{
					timeOutMS = getMillis(properties.get(IQueueService.PROPERTY_TIMEOUT_MS),timeOutMS);
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"problems with timeoutvalue",e);
				}
				
				try
				{
					hbTimeOutMS = getMillis(properties.get(IQueueService.PROPERTY_HB_TIMEOUT_MS),hbTimeOutMS);
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"problems with heartbeattimeoutvalue",e);
				}
				
				
				try
				{
					periodicRepetitionInterval = getMillis(properties.get(IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL),periodicRepetitionInterval);
				}
				catch (Exception e)
				{
					log(LogService.LOG_ERROR,"problems with periodic repetition interval",e);
				}
				
				if((serviceId == null) || serviceId.isEmpty())
				{
					serviceId = "SERVICE_" + UUID.randomUUID().toString();
				}
				
				serviceConfigurationList.add
				(
					new QueueComponentConfiguration.QueueServiceConfiguration(serviceId)
						.setName(name)
						.setCategory(category)
						.setStartDelayInMS(startDelayMS)
						.setTimeOutInMS(timeOutMS)
						.setHeartbeatTimeOutInMS(hbTimeOutMS)
						.setPeriodicRepetitionIntervalMS(periodicRepetitionInterval)
				);
			}
			registerQueueServiceLifecycled(queueService, configurable, serviceReference.getBundle(), properties);
		}
		finally 
		{
			osgiLifecycleReadLock.unlock();
		}
	}
	
	@Override
	public void registerQueueService(IQueueService queueService,IQueueComponentConfigurable configuration, Bundle bundle, Map<String, ?> properties)
	{
		if(configuration == null)
		{
			return;
		}
		
		if(this.context == null)
		{
			return;
		}
		
		osgiLifecycleReadLock.lock();
		try
		{
			registerQueueServiceLifecycled(queueService, configuration, bundle, properties);
		}
		finally 
		{
			extensionListReadLock.unlock();
		}
	}
	
	public void registerQueueServiceLifecycled(IQueueService queueService,IQueueComponentConfigurable configuration, Bundle bundle, Map<String, ?> properties)
	{
		if(configuration == null)
		{
			return;
		}
		
		if(this.context == null)
		{
			return;
		}
		List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList = null;
		List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList = null;
		List<QueueComponentConfiguration.QueueServiceConfiguration> serviceConfigurationList = null;
		
		if(configuration.configureQueueService() == null)
		{
			return;
		}
		
		QueueComponentConfiguration.BoundedByQueueId boundedById;
		QueueComponentConfiguration.BoundedByQueueConfiguration boundedByQueueConfiguration;
		QueueComponentConfiguration.QueueServiceConfiguration serviceConfiguration;
		for(QueueComponentConfiguration config : configuration.configureQueueService())
		{
			if(config instanceof QueueComponentConfiguration.BoundedByQueueId)
			{
				boundedById = (QueueComponentConfiguration.BoundedByQueueId)config;
				if((boundedById.getDispatcherId() != null) && (! boundedById.getDispatcherId().equals(this.id)))
				{
					continue;
				}
				if(boundByIdList == null)
				{
					boundByIdList = new ArrayList<QueueComponentConfiguration.BoundedByQueueId>();
				}
				boundByIdList.add(boundedById.copy());
			}
			if(config instanceof QueueComponentConfiguration.BoundedByQueueConfiguration)
			{
				boundedByQueueConfiguration = (QueueComponentConfiguration.BoundedByQueueConfiguration)config;
				if((boundedByQueueConfiguration.getDispatcherId() != null) && (! boundedByQueueConfiguration.getDispatcherId().equals(this.id)))
				{
					continue;
				}
				if(boundedByQueueConfigurationList == null)
				{
					boundedByQueueConfigurationList = new ArrayList<QueueComponentConfiguration.BoundedByQueueConfiguration>();
				}
				boundedByQueueConfigurationList.add(boundedByQueueConfiguration.copy());
			}
			if(config instanceof QueueComponentConfiguration.QueueServiceConfiguration)
			{
				serviceConfiguration = (QueueComponentConfiguration.QueueServiceConfiguration)config;
				if(serviceConfigurationList == null)
				{
					serviceConfigurationList = new ArrayList<QueueComponentConfiguration.QueueServiceConfiguration>();
				}
				serviceConfigurationList.add(serviceConfiguration.copy());
			}
		}
		
		if
		(
			((boundByIdList ==  null) || boundByIdList.isEmpty()) &&
			((boundedByQueueConfigurationList == null) || boundedByQueueConfigurationList.isEmpty())
		)
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
				serviceContainer = new ServiceContainer(this, boundByIdList, boundedByQueueConfigurationList, serviceConfigurationList);
				serviceContainer.setQueueService(queueService);
				serviceContainer.setProperties(properties);
				
				this.serviceList.add(serviceContainer);
				this.serviceReverseIndex.put(queueService,serviceContainer);
				this.configurationPropertyBindingRegistry.register(serviceContainer);
			}
			registerQueueService(serviceContainer);
		}
		finally 
		{
			serviceListWriteLock.unlock();
		}
	}
	
	private boolean registerQueueService(ServiceContainer serviceContainer)
	{
		this.extensionListReadLock.lock();
		try
		{
			if(serviceContainer.isRegistered())
			{
				return false;
			}
			
			serviceContainer.setRegistered(true);
			
			
			if
			(
				(
					(serviceContainer.getBoundByIdList() == null) || 
					serviceContainer.getBoundByIdList().isEmpty()
				) && 
				(
					(serviceContainer.getBoundedByQueueConfigurationList() == null) ||
					serviceContainer.getBoundedByQueueConfigurationList().isEmpty()
				)
			)
			{
				if(this.logService != null)
				{
					log(LogService.LOG_WARNING, "Missing queueId or queueConfigurationFilter for service",null);
				}
				return false;
			}
			
			QueueBindingModifyFlags modifyFlags = new QueueBindingModifyFlags();
			
			if(serviceContainer.getBoundByIdList() != null)
			{
				for(QueueComponentConfiguration.BoundedByQueueId boundedByQueueId : serviceContainer.getBoundByIdList())
				{
					if(boundedByQueueId.getQueueId() == null)
					{
						continue;
					}
					if(boundedByQueueId.getQueueId().isEmpty())
					{
						continue;
					}
					this.queueIndexReadLock.lock();
					QueueImpl queue = null;
					try
					{
						queue = this.queueIndex.get(boundedByQueueId.getQueueId());
						
					}
					finally 
					{
						this.queueIndexReadLock.unlock();
					}
					
					if(queue != null)
					{
						modifyFlags.reset();
						queue.checkForService(serviceContainer, modifyFlags);
					}
				}
			}
			if(serviceContainer.getBoundedByQueueConfigurationList() != null)
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
	
	@Override
	public void unregisterQueueService(IQueueService queueService)
	{
		this.unbindQueueService(queueService);
	}
	
	public void unbindQueueService(IQueueService queueService)
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
				
				this.configurationPropertyBindingRegistry.unregister(serviceContainer);
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
				entry.getValue().unsetService(serviceContainer, true);
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
	public PropertyBlockImpl createPropertyBlock()
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
			this.workerPool.addFirst(worker);
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
				foundWorker = this.workerPool.removeFirst();
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
			LinkedList<QueueWorker> removeList = new LinkedList<QueueWorker>();
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
			removeList.clear();
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
	
	public void executeOnTaskTimeOut(IOnTaskTimeout controller, IQueue queue, IQueueTask task)
	{
		try
		{
			this.executorService.submit(new Callable<IQueueTask>()
			{
				@Override
				public IQueueTask call()
				{
					try
					{
						controller.onTaskTimeout(queue, task);
					}
					catch (Exception e) {}
					return task;
				}
			}).get(7, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
	}
	
	public void executeOnTaskStopExecuter(QueueWorker worker, IQueueTask task)
	{
		this.executorService.execute(new Runnable()
		{
			@Override
			@SuppressWarnings("deprecation")
			public void run()
			{
				if(worker.isAlive())
				{
					if(task instanceof IOnTaskStop)
					{
						long number = 0L;
						long moreTimeUntilNow = 0L;
						long moreTime;
						
						while(worker.isAlive() && ((moreTime = ((IOnTaskStop)task).requestForMoreLifeTime(number, moreTimeUntilNow, worker.getWorkerWrapper())) > 0) )
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
	
	public Future<IQueueEventResult> createFutureOfScheduleResult(QueueEventResultImpl scheduleResult)
	{
		Callable<IQueueEventResult> call = new Callable<IQueueEventResult>()
		{
			@Override
			public IQueueEventResult call() throws Exception
			{
				scheduleResult.waitForProcessingIsFinished();
				return scheduleResult;
			}
			
		};
		return this.executorService.submit(call);
	}
	
	
	public void onConfigurationModify(QueueImpl queue, String... attributes)
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
			Set<ControllerContainer> matchedControllerContainer = configurationPropertyBindingRegistry.getControllerContainer(attributes);
			if(matchedControllerContainer != null)
			{
				controllerListReadLock.lock(); // TODO required ? 
				try
				{	
				
					for(ControllerContainer controllerContainer : matchedControllerContainer)
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
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"check queue binding for controller by configuration filter on queue configuration modify",e);
		}
		
		try
		{
			Set<ServiceContainer> matchedServiceContainer = configurationPropertyBindingRegistry.getServiceContainer(attributes);
			if(matchedServiceContainer != null)
			{
				serviceListReadLock.lock(); // TODO required?
				try
				{
					for(ServiceContainer serviceContainer : matchedServiceContainer)
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
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"check queue binding for services by configuration filter on queue configuration modify",e);
		}
		
		if(timerContext != null)
		{
			try {timerContext.stop();}catch (Exception e) {}
		}
		
		if(queue.getControllerSize() < 1)
		{
			checkControllerForQueue(queue);
		}
	}

	@Override
	public IPropertyBlock getPropertyBlock()
	{
		return this.propertyBlock;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getQueueIdsFromServiceProperties(Map<String,?> properties)
	{
		List<String> queueIdList = null;
		if(properties.get(IEventDispatcher.PROPERTY_QUEUE_ID) instanceof String)
		{
			queueIdList = new ArrayList<String>();
			queueIdList.add((String)properties.get(IEventDispatcher.PROPERTY_QUEUE_ID));
		}
		else if(properties.get(IEventDispatcher.PROPERTY_QUEUE_ID) instanceof String[])
		{
			queueIdList = new ArrayList<String>(Arrays.asList((String[])properties.get(IEventDispatcher.PROPERTY_QUEUE_ID)));
		}
		else if(properties.get(IEventDispatcher.PROPERTY_QUEUE_ID) instanceof Collection<?>)
		{
			queueIdList = new ArrayList<String>((Collection<String>)properties.get(IEventDispatcher.PROPERTY_QUEUE_ID));
		}
		
		return queueIdList;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getQueueConfigurationFilterListFromServiceProperties(Map<String,?> properties)
	{
		List<String> queueConfigurationFilterList = null;
		if(properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER) instanceof String)
		{
			queueConfigurationFilterList = new ArrayList<String>();
			queueConfigurationFilterList.add((String)properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER));
		}
		else if(properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER) instanceof String[])
		{
			queueConfigurationFilterList = new ArrayList<String>(Arrays.asList((String[])properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER)));
		}
		else if(properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER) instanceof Collection<?>)
		{
			queueConfigurationFilterList = new ArrayList<String>((Collection<String>)properties.get(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER));
		}
		return queueConfigurationFilterList;
	}
	
	private Boolean getDisableMetricsFromServiceProperties(Map<String,?> properties)
	{
		Object disableMetricsProperty = properties.get(IQueueController.PROPERTY_DISABLE_METRICS);
		if(disableMetricsProperty != null)
		{
			if(disableMetricsProperty instanceof Boolean)
			{
				return (Boolean)disableMetricsProperty;
			}
			else if (disableMetricsProperty instanceof String)
			{
				return ((String)disableMetricsProperty).equalsIgnoreCase("true");
			}
			else
			{
				return disableMetricsProperty.toString().equalsIgnoreCase("true");
			}
		}
		return null;
	}
	
	private long getMillis(Object property, long defaultValue)
	{
		if(property == null)
		{
			return defaultValue;
		}
		if(property instanceof String)
		{
			return Long.parseLong(((String)property).trim());
		}
		if(property instanceof Integer)
		{
			return (long)((Integer)property);
		}
		return (Long)property;
	}
	
	private class ScheduledController
	{
		IQueueController queueController;
		ServiceReference<IQueueController> serviceReference;
		Map<String, ?> properties;
	}
	
	private class ScheduledService
	{
		IQueueService queueService;
		ServiceReference<IQueueController> serviceReference;
		Map<String, ?> properties;
	}
}
