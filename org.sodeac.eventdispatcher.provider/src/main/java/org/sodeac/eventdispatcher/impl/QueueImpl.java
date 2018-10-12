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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.log.LogService;
import org.sodeac.multichainlist.LinkageDefinition;
import org.sodeac.multichainlist.MultiChainList;
import org.sodeac.multichainlist.Partition;
import org.sodeac.multichainlist.Node;
import org.sodeac.multichainlist.Snapshot;

import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueue.ILinkageDefinitionDispatcherBuilder;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IGauge;
import org.sodeac.eventdispatcher.api.ILinkageDefinitionDispatcher;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IQueueEventResult;
import org.sodeac.eventdispatcher.api.ITimer;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.eventdispatcher.api.QueueIsFullException;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;
import org.sodeac.eventdispatcher.extension.api.IExtensibleQueue;
import org.sodeac.eventdispatcher.impl.ControllerContainer.ControllerFilterObjects;
import org.sodeac.eventdispatcher.impl.ServiceContainer.ServiceFilterObjects;

public class QueueImpl implements IQueue,IExtensibleQueue
{
	public QueueImpl(String queueId,EventDispatcherImpl eventDispatcher, boolean enableMetrics, String name, String category,Map<String, Object> configurationProperties, Map<String, Object> stateProperties)
	{
		super();
	
		this.name = name;
		this.category = category;
		
		this.queueId = queueId;
		this.eventDispatcher = eventDispatcher;
		this.enableMetrics = enableMetrics;
		
		this.genericQueueSpoolLock = new ReentrantLock();
		this.workerSpoolLock = new ReentrantLock();
		
		this.controllerList = new ArrayList<ControllerContainer>();
		this.controllerIndex = new HashMap<ControllerContainer,ControllerContainer>();
		this.controllerListLock = new ReentrantReadWriteLock(true);
		this.controllerListReadLock = this.controllerListLock.readLock();
		this.controllerListWriteLock = this.controllerListLock.writeLock();
		
		this.serviceList = new ArrayList<ServiceContainer>();
		this.serviceIndex = new HashMap<ServiceContainer,ServiceContainer>();
		this.serviceListLock = new ReentrantReadWriteLock(true);
		this.serviceListReadLock = this.serviceListLock.readLock();
		this.serviceListWriteLock = this.serviceListLock.writeLock();
		
		this.eventQueue = new MultiChainList<>();
		this.mainPartitionEventQueue = this.eventQueue.getPartition(null);
		
		this.newEventQueue = new MultiChainList<>();
		this.mainPartitionNewEventQueue = this.newEventQueue.getPartition(null);
				
		this.removedEventQueue = new MultiChainList<>();
		this.mainPartitionRemovedEventQueue = this.removedEventQueue.getPartition(null);
		
		this.fireEventQueue = new MultiChainList<>();
		this.mainPartitionFireEventQueue = this.fireEventQueue.getPartition(null);
		
		this.eventListLock = new ReentrantReadWriteLock(true);
		this.eventListReadLock = this.eventListLock.readLock();
		this.eventListWriteLock = this.eventListLock.writeLock();
		
		this.jobList = new ArrayList<JobContainer>();
		this.jobIndex = new HashMap<String,JobContainer>();
		this.jobListLock = new ReentrantReadWriteLock(true);
		this.jobListReadLock = this.jobListLock.readLock();
		this.jobListWriteLock = this.jobListLock.writeLock();
		
		this.signalList = new ArrayList<String>();
		this.signalListLock = new ReentrantLock(true);
		
		this.onQueueObserveList = new ArrayList<IOnQueueObserve>();
		this.onQueueObserveListLock = new ReentrantLock(true);
		
		this.lastWorkerAction = System.currentTimeMillis();
		
		this.queueScopeList = new ArrayList<QueueSessionScopeImpl>();
		this.queueScopeIndex = new HashMap<UUID,QueueSessionScopeImpl>();
		this.queueScopeListCopy = Collections.unmodifiableList(new ArrayList<IQueueSessionScope>());
		this.queueScopeListLock = new ReentrantReadWriteLock(true);
		this.queueScopeListReadLock = this.queueScopeListLock.readLock();
		this.queueScopeListWriteLock = this.queueScopeListLock.writeLock();
		
		this.consumeEventHandlerListLock = new ReentrantReadWriteLock();
		this.consumeEventHandlerListWriteLock = consumeEventHandlerListLock.writeLock();
		this.consumeEventHandlerListReadLock = consumeEventHandlerListLock.readLock();
		
		PropertyBlockImpl qualityValues = (PropertyBlockImpl)eventDispatcher.createPropertyBlock();
		qualityValues.setProperty(IMetrics.QUALITY_VALUE_CREATED, System.currentTimeMillis());
		this.metrics = new MetricImpl(this,qualityValues, null,enableMetrics);
		
		this.configurationPropertyBlock = (PropertyBlockImpl)eventDispatcher.createPropertyBlock();
		if(configurationProperties != null)
		{
			this.configurationPropertyBlock.setPropertySet(configurationProperties, false);
		}
		
		this.statePropertyBlock = (PropertyBlockImpl)eventDispatcher.createPropertyBlock();
		if(stateProperties != null)
		{
			this.statePropertyBlock.setPropertySet(stateProperties,false);
		}
		
		this.metrics.registerGauge(new IGauge<Long>()
		{
			@Override
			public Long getValue()
			{
				return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_LAST_SEND_EVENT);
			}
		}, IMetrics.GAUGE_LAST_SEND_EVENT);
		
		this.metrics.registerGauge(new IGauge<Long>()
		{
			@Override
			public Long getValue()
			{
				return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_LAST_POST_EVENT);
			}
		}, IMetrics.GAUGE_LAST_POST_EVENT);
		
		this.metrics.registerGauge(new IGauge<Long>()
		{
			@Override
			public Long getValue()
			{
				return eventQueue.getNodeSize();
			}
		}, IMetrics.GAUGE_SIZE_EVENTQUEUE);
		
		this.metrics.registerGauge(new IGauge<Long>()
		{
			@Override
			public Long getValue()
			{
				return newEventQueue.getNodeSize();
			}
		}, IMetrics.GAUGE_SIZE_NEW_EVENTQUEUE);
		
		this.metrics.registerGauge(new IGauge<Long>()
		{
			@Override
			public Long getValue()
			{
				return removedEventQueue.getNodeSize();
			}
		}, IMetrics.GAUGE_SIZE_REMOVED_EVENTQUEUE);
		
		this.metrics.registerGauge(new IGauge<Long>()
		{
			@Override
			public Long getValue()
			{
				return fireEventQueue.getNodeSize();
			}
		}, IMetrics.GAUGE_SIZE_FIRE_EVENTQUEUE);
		
		this.queueConfigurationModifyListener = new QueueConfigurationModifyListener(this);
		this.configurationPropertyBlock.addModifyListener(this.queueConfigurationModifyListener);
		
		this.linkageDefinitionDispatcherIndex = new HashMap<String,LinkageDefinitionDispatcherImpl>();
		this.snapshotsByWorkerThread = new LinkedList<Snapshot<IQueuedEvent>>();
	}
	
	protected String category = null;
	protected String name = null;
	protected volatile int eventListLimit = Integer.MAX_VALUE;
	
	protected MetricImpl metrics;
	protected EventDispatcherImpl eventDispatcher = null;
	protected String queueId = null;
	
	protected List<ControllerContainer> controllerList;
	protected volatile List<ControllerContainer> controllerListCopy = null;
	protected Map<ControllerContainer,ControllerContainer> controllerIndex = null;
	protected ReentrantReadWriteLock controllerListLock;
	protected ReadLock controllerListReadLock;
	protected WriteLock controllerListWriteLock;
	
	protected List<ServiceContainer> serviceList;
	protected Map<ServiceContainer,ServiceContainer> serviceIndex = null;
	protected volatile List<ServiceContainer> serviceListCopy = null;
	protected ReentrantReadWriteLock serviceListLock;
	protected ReadLock serviceListReadLock;
	protected WriteLock serviceListWriteLock;
	
	protected ReentrantReadWriteLock eventListLock;
	protected ReadLock eventListReadLock;
	protected WriteLock eventListWriteLock;
	
	protected MultiChainList<QueuedEventImpl> eventQueue = null;
	protected Partition<QueuedEventImpl> mainPartitionEventQueue = null;
	
	protected MultiChainList<QueuedEventImpl> newEventQueue = null;
	protected Partition<QueuedEventImpl> mainPartitionNewEventQueue = null;
	
	protected MultiChainList<QueuedEventImpl> removedEventQueue = null;
	protected Partition<QueuedEventImpl> mainPartitionRemovedEventQueue = null;
	
	protected MultiChainList<Event> fireEventQueue = null;
	protected Partition<Event> mainPartitionFireEventQueue = null;
	
	protected List<JobContainer> jobList = null;
	protected Map<String,JobContainer> jobIndex = null;
	protected ReentrantReadWriteLock jobListLock;
	protected ReadLock jobListReadLock;
	protected WriteLock jobListWriteLock;
	
	protected volatile boolean signalListUpdate = false;
	protected List<String> signalList = null;
	protected ReentrantLock signalListLock = null;
	
	protected volatile boolean onQueueObserveListUpdate = false;
	protected List<IOnQueueObserve> onQueueObserveList = null;
	protected ReentrantLock onQueueObserveListLock = null;
	
	protected volatile QueueWorker queueWorker = null;
	protected volatile SpooledQueueWorker currentSpooledQueueWorker = null;
	protected volatile long lastWorkerAction;
	protected PropertyBlockImpl configurationPropertyBlock = null;
	protected PropertyBlockImpl statePropertyBlock = null;
	
	protected volatile boolean newScheduledListUpdate = false;
	protected volatile boolean removedEventListUpdate = false;
	protected volatile boolean firedEventListUpdate = false;
	
	protected ReentrantLock genericQueueSpoolLock = null;
	protected ReentrantLock workerSpoolLock = null;
	
	protected volatile boolean enableMetrics = true;
	protected volatile boolean disposed = false; 
	protected volatile boolean privateWorker = false;
	
	protected volatile QueueConfigurationModifyListener queueConfigurationModifyListener = null;
	
	protected List<QueueSessionScopeImpl> queueScopeList;
	protected Map<UUID,QueueSessionScopeImpl> queueScopeIndex;
	protected volatile List<IQueueSessionScope> queueScopeListCopy = null;
	protected ReentrantReadWriteLock queueScopeListLock;
	protected ReadLock queueScopeListReadLock;
	protected WriteLock queueScopeListWriteLock;
	
	private volatile List<ConsumeEventHandler> consumeEventHandlerList = null;
	private volatile List<ConsumeEventHandler> consumeEventHandlerCopyList = null;
	private ReentrantReadWriteLock consumeEventHandlerListLock;
	private ReadLock consumeEventHandlerListReadLock;
	private WriteLock consumeEventHandlerListWriteLock;
	
	protected QueueImpl parent = null;
	
	protected Map<String,LinkageDefinitionDispatcherImpl> linkageDefinitionDispatcherIndex = null;
	protected LinkedList<Snapshot<IQueuedEvent>> snapshotsByWorkerThread;
	
	@Override
	public Future<IQueueEventResult> queueEvent(Event event)
	{
		QueuedEventImpl queuedEvent = null;
		QueueEventResultImpl resultImpl = new QueueEventResultImpl();
		eventListWriteLock.lock();
		try
		{
			if(this.eventListLimit <= this.eventQueue.getNodeSize())
			{
				throw new QueueIsFullException(this.queueId, this.eventListLimit);
			}
			queuedEvent = new QueuedEventImpl(event,this);
			queuedEvent.setScheduleResultObject(resultImpl);
			queuedEvent.setNode(this.eventQueue.append(queuedEvent));
		}
		finally 
		{
			eventListWriteLock.unlock();
		}
		
		
		this.genericQueueSpoolLock.lock();
		try
		{
			newScheduledListUpdate = true; 
			this.newEventQueue.append(queuedEvent);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_SCHEDULE_EVENT).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		this.notifyOrCreateWorker(-1);
		
		return this.eventDispatcher.createFutureOfScheduleResult(resultImpl);
	}
	
	@Override
	public Future<IQueueEventResult> queueEventList(List<Event> eventList)
	{
		List<QueuedEventImpl> queuedEventList = new ArrayList<QueuedEventImpl>(eventList.size());
		QueueEventResultImpl resultImpl = new QueueEventResultImpl();
		eventListWriteLock.lock();
		try
		{
			if(this.eventListLimit < ((eventQueue.getNodeSize()) + eventList.size()))
			{
				throw new QueueIsFullException(this.queueId, this.eventListLimit);
			}
			for(Event event : eventList)
			{
				QueuedEventImpl queuedEvent = new QueuedEventImpl(event,this);
				queuedEvent.setScheduleResultObject(resultImpl);
				queuedEventList.add(queuedEvent);
			}
			Node<QueuedEventImpl>[] nodes = eventQueue.append(queuedEventList);
			for(int i = 0; i < nodes.length; i++)
			{
				queuedEventList.get(i).setNode(nodes[i]);
			}
		}
		finally 
		{
			eventListWriteLock.unlock();
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			newScheduledListUpdate = true;
			for(QueuedEventImpl queuedEvent : queuedEventList)
			{
				this.newEventQueue.append(queuedEvent);
			}
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_SCHEDULE_EVENT).mark(eventList.size());
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		this.notifyOrCreateWorker(-1);
		
		return this.eventDispatcher.createFutureOfScheduleResult(resultImpl);
	}
	
	// Controller
	
	public void checkForController(ControllerContainer controllerContainer,QueueBindingModifyFlags bindingModifyFlags)
	{
		boolean controllerMatch = false;
		if(controllerContainer.getBoundByIdList() != null)
		{
			for(QueueComponentConfiguration.BoundedByQueueId boundedById : controllerContainer.getBoundByIdList())
			{
				if(boundedById.getQueueId() == null)
				{
					continue;
				}
				if(boundedById.getQueueId().isEmpty())
				{
					continue;
				}
				if(boundedById.getQueueId().equals(this.queueId))
				{
					controllerMatch = true;
					break;
				}
			}
		}
		if(! controllerMatch)
		{
			if(controllerContainer.getBoundedByQueueConfigurationList() != null)
			{
				if(controllerContainer.getFilterObjectList() != null)
				{
					for(ControllerFilterObjects controllerFilterObjects : controllerContainer.getFilterObjectList())
					{
						if(controllerFilterObjects.filter == null)
						{
							continue;
						}
						try
						{
							if(controllerFilterObjects.filter.matches(this.configurationPropertyBlock.getProperties()))
							{
								controllerMatch = true;
								break;
							}
						}
						catch (Exception e) 
						{
							log(LogService.LOG_ERROR,"check queue binding for controller",e);
						}
					}
				}
			}
		}
		
		boolean add = false;
		boolean remove = false;
		
		if(controllerMatch)
		{
			add = setController(controllerContainer);
		}
		else
		{
			remove = unsetController(controllerContainer);
		}
		
		if(this instanceof QueueSessionScopeImpl)
		{	
			if(controllerMatch){bindingModifyFlags.setScopeSet(true);}
			if(add) {bindingModifyFlags.setScopeAdd(true);}
			if(remove) {bindingModifyFlags.setScopeRemove(true);}
		}
		else
		{
			this.queueScopeListReadLock.lock();
			try
			{
				for(QueueSessionScopeImpl scope : this.queueScopeList)
				{
					if(scope.isAdoptContoller() && controllerMatch)
					{
						bindingModifyFlags.setScopeSet(true);
						if(scope.setController(controllerContainer))
						{
							bindingModifyFlags.setScopeAdd(true);
						}
					}
					else
					{
						scope.checkForController(controllerContainer, bindingModifyFlags);
					}
				}
			}
			finally 
			{
				this.queueScopeListReadLock.unlock();
			}
			
			if(controllerMatch){bindingModifyFlags.setGlobalSet(true);}
			if(add) {bindingModifyFlags.setGlobalAdd(true);}
			if(remove) {bindingModifyFlags.setGlobalRemove(true);}
		}
	}
	
	public boolean setController(ControllerContainer controllerContainer)
	{
		controllerListReadLock.lock();
		try
		{
			if(this.controllerIndex.get(controllerContainer) != null)
			{
				return false;
			}
		}
		finally 
		{
			controllerListReadLock.unlock();
		}
		
		EventSubscriptionsMerger subscriptionMerger = null;
		if(controllerContainer.getSubscribeEventList() != null)
		{
			subscriptionMerger = new EventSubscriptionsMerger(this, null, controllerContainer);
		}
		
		MetricsDisabledMerger metricsDisabledMerger = new MetricsDisabledMerger(this, null, controllerContainer);
		
		controllerListWriteLock.lock();
		try
		{
			if(this.controllerIndex.get(controllerContainer) != null)
			{
				// already registered
				return false;
			}

			
			this.controllerList.add(controllerContainer);
			this.controllerIndex.put(controllerContainer,controllerContainer);
			this.controllerListCopy = null;
			
			// merge OSGi event subscriptions
			if(subscriptionMerger != null)
			{
				subscriptionMerger.merge();
			}
			
			// enable disable metrics
			setQueueMetricsEnabled(! metricsDisabledMerger.disableMetrics());
			
			// observeEvent
			
			if(controllerContainer.isImplementingIOnQueueObserve())
			{
				addOnQueueObserver((IOnQueueObserve)controllerContainer.getQueueController());
			}
			return true;
		}
		finally 
		{
			controllerListWriteLock.unlock();
		}
	}
	
	private boolean unsetController(ControllerContainer configurationContainer)
	{
		return unsetController(configurationContainer,false);
	}
	
	public boolean unsetController(ControllerContainer configurationContainer, boolean unregisterInScope)
	{
		if(unregisterInScope)
		{
			this.queueScopeListReadLock.lock();
			try
			{
				for(QueueSessionScopeImpl scope : this.queueScopeList)
				{
					scope.unsetController(configurationContainer, false);
				}
			}
			finally 
			{
				this.queueScopeListReadLock.unlock();
			}
		}
		controllerListReadLock.lock();
		try
		{
			if(this.controllerIndex.get(configurationContainer)  ==  null)
			{
				return false;
			}
		}
		finally 
		{
			controllerListReadLock.unlock();
		}
		
		EventSubscriptionsMerger subscriptionMerger = null;
		if(configurationContainer.getSubscribeEventList() != null)
		{
			subscriptionMerger = new EventSubscriptionsMerger(this, configurationContainer,null);
		}
		MetricsDisabledMerger metricsDisabledMerger = new MetricsDisabledMerger(this, null, configurationContainer);
		
		controllerListWriteLock.lock();
		try
		{
			ControllerContainer unlinkFromQueue = this.controllerIndex.get(configurationContainer);
			if(unlinkFromQueue  ==  null)
			{
				return false;
			}
			
			while(this.controllerList.remove(unlinkFromQueue)) {}
			this.controllerIndex.remove(unlinkFromQueue);
			this.controllerListCopy = null;
			
			// merge OSGi event subscriptions
			if(subscriptionMerger != null)
			{
				subscriptionMerger.merge();
			}
			
			// enable disable metrics
			setQueueMetricsEnabled(! metricsDisabledMerger.disableMetrics());
			
			// IOnQueueReverse
			
			if(unlinkFromQueue.isImplementingIOnQueueReverse())
			{
				this.eventDispatcher.executeOnQueueReverse((IOnQueueReverse)unlinkFromQueue.getQueueController(), this);
			}
			
			return true;
		}
		finally 
		{
			controllerListWriteLock.unlock();
		}
	}
	
	public int getControllerSize()
	{
		controllerListReadLock.lock();
		try
		{
			return this.controllerList.size();
		}
		finally 
		{
			controllerListReadLock.unlock();
		}
	}
	
	// Services
	
	public void checkForService(ServiceContainer serviceContainer, QueueBindingModifyFlags bindingModifyFlags)
	{
		boolean serviceMatch = false;
		
		if(serviceContainer.getBoundByIdList() != null)
		{
			for(QueueComponentConfiguration.BoundedByQueueId boundedById : serviceContainer.getBoundByIdList())
			{
				if(boundedById.getQueueId() == null)
				{
					continue;
				}
				if(boundedById.getQueueId().isEmpty())
				{
					continue;
				}
				if(boundedById.getQueueId().equals(this.queueId))
				{
					serviceMatch = true;
					break;
				}
			}
		}
		if(! serviceMatch)
		{
			if(serviceContainer.getBoundedByQueueConfigurationList() != null)
			{
				for(ServiceFilterObjects serviceFilterObjects : serviceContainer.getFilterObjectList())
				{
					try
					{
						if(serviceFilterObjects.filter.matches(this.configurationPropertyBlock.getProperties()))
						{
							serviceMatch = true;
							break;
						}
					}
					catch (Exception e) 
					{
						log(LogService.LOG_ERROR,"check queue binding for service",e);
					}
				}
			}
		}
		
		boolean add = false;
		boolean remove = false;
		
		if(serviceMatch)
		{
			add = setService(serviceContainer,true);
		}
		else
		{
			remove = unsetService(serviceContainer);
		}
		
		if(this instanceof QueueSessionScopeImpl)
		{	
			if(serviceMatch){bindingModifyFlags.setScopeSet(true);}
			if(add) {bindingModifyFlags.setScopeAdd(true);}
			if(remove) {bindingModifyFlags.setScopeRemove(true);}
		}
		else
		{
			this.queueScopeListReadLock.lock();
			try
			{
				for(QueueSessionScopeImpl scope : this.queueScopeList)
				{
					if(scope.isAdoptServices() && serviceMatch)
					{
						bindingModifyFlags.setScopeSet(true);
						if(scope.setService(serviceContainer, true))
						{
							bindingModifyFlags.setScopeAdd(true);
						}
					}
					else
					{
						scope.checkForService(serviceContainer, bindingModifyFlags);
					}
				}
			}
			finally 
			{
				this.queueScopeListReadLock.unlock();
			}
			
			if(serviceMatch){bindingModifyFlags.setGlobalSet(true);}
			if(add) {bindingModifyFlags.setGlobalAdd(true);}
			if(remove) {bindingModifyFlags.setGlobalRemove(true);}
		}
	}

	@Override
	public void setQueueMetricsEnabled(boolean enabled)
	{
		try
		{
			jobListWriteLock.lock();
			
			this.enableMetrics = enabled;
			
			// don't switch of on simple jobs
			for(JobContainer jobContainer : jobList)
			{
				if(jobContainer.getJob() instanceof IQueueService)
				{
					continue;
				}
				
				try
				{
					IMetrics jobMetrics = jobContainer.getMetrics();
					if(jobMetrics == null)
					{
						continue;
					}
					if(jobMetrics instanceof MetricImpl)
					{
						if(enabled)
						{
							((MetricImpl)jobMetrics).enable();
						}
						else
						{
							((MetricImpl)jobMetrics).disable();
						}
					}
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR, "enablejob queue metrics", e);
				}
			}
				
			try
			{
				if(enabled)
				{
					this.metrics.enable();
				}
				else
				{
					this.metrics.disable();
				}
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR, "enable/disable metrics", e);
			}
			
		}
		finally 
		{
			jobListWriteLock.unlock();
		}
	}


	@Override
	public boolean isMetricsEnabled()
	{
		return this.enableMetrics;
	}
	
	public boolean setService(ServiceContainer serviceContainer, boolean createOnly)
	{
		if(createOnly)
		{
			serviceListReadLock.lock();
			try
			{
				if(serviceIndex.get(serviceContainer) != null)
				{
					return false;
				}
			}
			finally
			{
				serviceListReadLock.unlock();
			}
		}
		
		boolean reschedule = false;
		serviceListWriteLock.lock();
		try
		{
			if(serviceIndex.get(serviceContainer) != null)
			{
				if(createOnly)
				{
					return false;
				}
				reschedule = true;
			}
			
			if(! reschedule)
			{
				this.serviceList.add(serviceContainer);
				serviceIndex.put(serviceContainer,serviceContainer);
				this.serviceListCopy = null;
			}
		}
		finally 
		{
			serviceListWriteLock.unlock();
		}
		
		this.scheduleService(serviceContainer.getQueueService(), serviceContainer.getServiceConfiguration(), serviceContainer.getProperties(), reschedule);
		
		return true;
		
	}
	
	private void scheduleService(IQueueService queueService,QueueComponentConfiguration.QueueServiceConfiguration configuration, Map<String, ?> properties, boolean reschedule)
	{
		String serviceId = configuration.getServiceId();
		long delay = configuration.getStartDelayInMS() < 0L ? 0L : configuration.getStartDelayInMS() ;
		long timeout = configuration.getTimeOutInMS() < 0L ? -1L : configuration.getTimeOutInMS();
		long hbtimeout = configuration.getHeartbeatTimeOutInMS() < 0L ? -1L : configuration.getHeartbeatTimeOutInMS();
	
		try
		{
			if(reschedule)
			{
				this.rescheduleJob(serviceId, System.currentTimeMillis() + delay, timeout, hbtimeout);
				return;
			}
			IPropertyBlock servicePropertyBlock = this.eventDispatcher.createPropertyBlock();
			for(Entry<String,?> entry : properties.entrySet())
			{
				servicePropertyBlock.setProperty(entry.getKey(), entry.getValue());
			}
			if(configuration.getPeriodicRepetitionIntervalMS() < 0L)
			{
				servicePropertyBlock.removeProperty(IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL);
			}
			else
			{
				servicePropertyBlock.setProperty(IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL, configuration.getPeriodicRepetitionIntervalMS());
			}
			
			this.scheduleJob(serviceId, queueService, servicePropertyBlock, System.currentTimeMillis() + delay, timeout, hbtimeout);
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"problems scheduling service with id " + serviceId,e);
		}
	}
	
	private boolean unsetService(ServiceContainer serviceContainer)
	{
		return unsetService(serviceContainer, false);
	}
	
	public boolean unsetService(ServiceContainer serviceContainer, boolean unregisterInScope )
	{
		if(unregisterInScope)
		{
			this.queueScopeListReadLock.lock();
			try
			{
				for(QueueSessionScopeImpl scope : this.queueScopeList)
				{
					scope.unsetService(serviceContainer, false);
				}
			}
			finally 
			{
				this.queueScopeListReadLock.unlock();
			}
		}
		
		serviceListReadLock.lock();
		try
		{
			if(this.serviceIndex.get(serviceContainer) == null)
			{
				return false;
			}
		}
		finally 
		{
			serviceListReadLock.unlock();
		}
		
		serviceListWriteLock.lock();
		try
		{
			ServiceContainer toDelete = this.serviceIndex.get(serviceContainer);
			if(toDelete == null)
			{
				return false;
			}
			while(this.serviceList.remove(toDelete)) {}
			this.serviceIndex.remove(serviceContainer);
			this.serviceListCopy = null;
			
			jobListReadLock.lock();
			try
			{
				for(Entry<String,JobContainer> jobContainerEntry : this.jobIndex.entrySet())
				{
					try
					{
						if(jobContainerEntry.getValue().getJob() == serviceContainer.getQueueService())
						{
							jobContainerEntry.getValue().getJobControl().setDone();
							((MetricImpl)jobContainerEntry.getValue().getMetrics()).dispose();
						}
					}
					catch (Exception e) 
					{
						log(LogService.LOG_ERROR, "set queue service done", e);
					}
				}
			}
			finally 
			{
				jobListReadLock.unlock();
			}
			
			return true;
			
		}
		finally 
		{
			serviceListWriteLock.unlock();
		}
	}
	
	public int getServiceSize()
	{
		serviceListReadLock.lock();
		try
		{
			return this.serviceList.size();
		}
		finally 
		{
			serviceListReadLock.unlock();
		}
	}
	
	@Override
	public IMetrics getMetrics()
	{
		return this.metrics;
	}
	
	@Override
	public IPropertyBlock getConfigurationPropertyBlock()
	{
		return this.configurationPropertyBlock;
	}
	
	@Override
	public IPropertyBlock getStatePropertyBlock()
	{
		return this.statePropertyBlock;
	}

	@Override
	public String getId()
	{
		return this.queueId;
	}

	protected int cleanDoneJobs()
	{
		List<JobContainer> toRemove = null;
		jobListWriteLock.lock();
		try
		{
			
			for(JobContainer jobContainer : this.jobList)
			{
				if(jobContainer.getJobControl().isDone())
				{
					if(toRemove == null)
					{
						toRemove = new ArrayList<JobContainer>();
					}
					toRemove.add(jobContainer);
				}
			}
			
			if(toRemove == null)
			{
				return 0;
			}
			
			for(JobContainer jobContainer : toRemove)
			{
				String id = jobContainer.getId();
				this.jobList.remove(jobContainer);
				
				JobContainer containerById = this.jobIndex.get(id);
				if(containerById == null)
				{
					continue;
				}
				if(containerById == jobContainer)
				{
					this.jobIndex.remove(id);
				}
				
				try
				{
					((MetricImpl)jobContainer.getMetrics()).dispose();
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "dispose metrics", e);
				}
			}
			return toRemove.size();
		}
		finally 
		{
			jobListWriteLock.unlock();
		}
	}
	
	protected long getDueJobs(List<JobContainer> dueJobList)
	{
		jobListReadLock.lock();
		long timeStamp = System.currentTimeMillis();
		long nextRun = timeStamp + QueueWorker.DEFAULT_WAIT_TIME;
		try
		{
			
			for(JobContainer jobContainer : jobList)
			{
				if(jobContainer.getJobControl().isDone())
				{
					continue;
				}
				long executionTimeStampIntern = jobContainer.getJobControl().getExecutionTimeStampIntern();
				if(executionTimeStampIntern < nextRun)
				{
					nextRun = executionTimeStampIntern;
				}
				
				if( executionTimeStampIntern <= timeStamp)
				{
					dueJobList.add(jobContainer);
				}
			}
		}
		finally 
		{
			jobListReadLock.unlock();
		}
		
		return nextRun;
	}
	
	protected long getNextRun()
	{
		jobListReadLock.lock();
		long timeStamp = System.currentTimeMillis();
		long nextRun = timeStamp + QueueWorker.DEFAULT_WAIT_TIME;
		try
		{
			
			for(JobContainer jobContainer : jobList)
			{
				if(jobContainer.getJobControl().isDone())
				{
					continue;
				}
				long executionTimeStampIntern = jobContainer.getJobControl().getExecutionTimeStampIntern();
				if(executionTimeStampIntern < nextRun)
				{
					nextRun = executionTimeStampIntern;
				}
			}
		}
		finally 
		{
			jobListReadLock.unlock();
		}
		
		return nextRun;
	}
	
	@Override
	public IPropertyBlock getJobPropertyBlock(String id)
	{
		jobListReadLock.lock();
		try
		{
			JobContainer  jobContainer = this.jobIndex.get(id);
			if(jobContainer != null)
			{
				if(! jobContainer.getJobControl().isDone())
				{
					return jobContainer.getPropertyBlock();
				}
			}
		}
		finally 
		{
			jobListReadLock.unlock();
		}
		
		return null;
	}
	
	@Override
	public List<IQueueJob> getJobList(Filter filter)
	{
		List<IQueueJob> queryJobList = new ArrayList<IQueueJob>();
		
		jobListReadLock.lock();
		try
		{
			for(JobContainer jobContainer : jobList)
			{
				if(jobContainer.getJobControl().isDone()) 
				{
					continue;
				}
				if(filter == null)
				{
					queryJobList.add(jobContainer.getJob());
				}
				else if(filter.matches(jobContainer.getPropertyBlock().getProperties()))
				{
					queryJobList.add(jobContainer.getJob());
				}
			}
			return Collections.unmodifiableList(queryJobList);
		}
		finally 
		{
			jobListReadLock.unlock();
		}
	}
	
	@Override
	public Map<String,IQueueJob> getJobIndex(Filter filter)
	{
		Map<String,IQueueJob> queryJobIndex = new HashMap<String,IQueueJob>();
		
		jobListReadLock.lock();
		try
		{
			String id = null;
			JobContainer jobContainer = null;
			
			for(Entry<String,JobContainer> jobContainerEntry : this.jobIndex.entrySet())
			{
				id = jobContainerEntry.getKey();
				jobContainer = jobContainerEntry.getValue();
				
				if(jobContainer.getJobControl().isDone()) 
				{
					continue;
				}
				if(filter == null)
				{
					queryJobIndex.put(id,jobContainer.getJob());
				}
				else if(filter.matches(jobContainer.getPropertyBlock().getProperties()))
				{
					queryJobIndex.put(id,jobContainer.getJob());
				}
			}
			return Collections.unmodifiableMap(queryJobIndex);
		}
		finally 
		{
			jobListReadLock.unlock();
		}
	}


	@Override
	public String scheduleJob(IQueueJob job)
	{
		return scheduleJob(null,job);
	}
	
	@Override
	public String scheduleJob(String id, IQueueJob job)
	{
		return scheduleJob(id,job, null, -1, -1, -1);
	}
	
	@Override
	public String scheduleJob(String id, IQueueJob job, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut )
	{
		return scheduleJob(id,job, propertyBlock, executionTimeStamp, timeOutValue, heartBeatTimeOut, false);
	}
	
	@Override
	public String scheduleJob(String id, IQueueJob job, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut, boolean stopOnTimeOut )
	{

		JobContainer jobContainer = null;
		
		jobListWriteLock.lock();
		try
		{
			JobContainer toRemove =  null;
			for(JobContainer alreadyInList : this.jobList)
			{
				if(alreadyInList.getJob() == job)
				{
					if(alreadyInList.getJobControl().isDone())
					{
						toRemove = alreadyInList;
						break;
					}
					if((id == null) || (id.isEmpty()))
					{
						return null;
					}
					return alreadyInList.getId();
				}
			}
			if(toRemove != null)
			{
				this.jobIndex.remove(toRemove.getId());
				this.jobList.remove(toRemove);
				
				try
				{
					((MetricImpl)toRemove.getMetrics()).dispose();
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "dispose metrics", e);
				}
				
				toRemove = null;
			}
			
			if((id == null) || (id.isEmpty()))
			{
				id = UUID.randomUUID().toString();
				jobContainer = new JobContainer();
			}
			else
			{
				jobContainer = this.jobIndex.get(id);
				if(jobContainer != null)
				{
					if(jobContainer.getJobControl().isDone())
					{
						this.jobIndex.remove(jobContainer.getId());
						this.jobList.remove(jobContainer);
						
						try
						{
							((MetricImpl)jobContainer.getMetrics()).dispose();
						}
						catch(Exception e)
						{
							log(LogService.LOG_ERROR, "dispose metrics", e);
						}
						
						jobContainer = null;
					}
					else
					{
						return id;
					}
				}
				
				jobContainer = new JobContainer();
				jobContainer.setNamedJob(true);
			}
			
			PropertyBlockImpl qualityValues = (PropertyBlockImpl)this.getDispatcher().createPropertyBlock();
			qualityValues.setProperty(IMetrics.QUALITY_VALUE_CREATED, System.currentTimeMillis());
			
			
			MetricImpl metric = new MetricImpl(this,qualityValues, id, this.enableMetrics);
			
			if(propertyBlock == null)
			{
				propertyBlock = (PropertyBlockImpl)this.getDispatcher().createPropertyBlock();
			}
			
			JobControlImpl jobControl = new JobControlImpl(propertyBlock);
			if(executionTimeStamp > 0)
			{
				jobControl.setExecutionTimeStampSchedule(executionTimeStamp);
			}
			if(heartBeatTimeOut > 0)
			{
				jobControl.setHeartBeatTimeOut(heartBeatTimeOut);
			}
			if(timeOutValue > 0)
			{
				jobControl.setTimeOut(timeOutValue);
			}
			
			jobControl.setStopOnTimeOutFlag(stopOnTimeOut);
			
			propertyBlock.setProperty(IQueueJob.PROPERTY_KEY_JOB_ID, id);
			
			jobContainer.setId(id);
			jobContainer.setJob(job);
			jobContainer.setMetrics(metric);
			jobContainer.setPropertyBlock(propertyBlock);
			jobContainer.setJobControl(jobControl);
			
			qualityValues.setProperty(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, -1L);
			
			if(jobContainer.isNamedJob())
			{
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_CREATED);
					}
				}, IMetrics.GAUGE_JOB_CREATED);
				
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_FINISHED_TIMESTAMP);
					}
				}, IMetrics.GAUGE_JOB_FINISHED);
				
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_STARTED_TIMESTAMP);
					}
				}, IMetrics.GAUGE_JOB_STARTED);
				
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
					}
				}, IMetrics.GAUGE_JOB_LAST_HEARTBEAT);
			}
		}
		finally 
		{
			jobListWriteLock.unlock();
		}
		
		jobContainer.getJob().configure(id, jobContainer.getMetrics(), jobContainer.getPropertyBlock(), jobContainer.getJobControl());
		
		jobListWriteLock.lock();
		try
		{
			jobList.add(jobContainer);
			jobIndex.put(id, jobContainer);
		}
		finally 
		{
			jobListWriteLock.unlock();
		}
		notifyOrCreateWorker(executionTimeStamp);
		
		return id;
	}
	
	@Override
	public IQueueJob rescheduleJob(String id, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut)
	{
		JobContainer jobContainer = null;
		
		if((id == null) || (id.isEmpty()))
		{
			return null;
		}
		
		jobListWriteLock.lock();
		try
		{
			jobContainer = this.jobIndex.get(id);
			if(jobContainer == null)
			{
				return null;
			}
			
			if(jobContainer.getJobControl().isDone())
			{
				this.jobIndex.remove(jobContainer.getId());
				this.jobList.remove(jobContainer);
				
				try
				{
					((MetricImpl)jobContainer.getMetrics()).dispose();
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "dispose metrics", e);
				}
				return null;
			}
			
			JobControlImpl jobControl = jobContainer.getJobControl();
			
			if(heartBeatTimeOut > 0)
			{
				jobControl.setHeartBeatTimeOut(heartBeatTimeOut);
			}
			if(timeOutValue > 0)
			{
				jobControl.setTimeOut(timeOutValue);
			}
			
			if(executionTimeStamp > 0)
			{
				jobControl.setExecutionTimeStampReschedule(executionTimeStamp);
				this.notifyOrCreateWorker(executionTimeStamp);
			}
			
			return jobContainer.getJob();
		}
		finally 
		{
			jobListWriteLock.unlock();
		}
	}
	
	@Override
	public IQueueJob getJob(String id)
	{

		jobListReadLock.lock();
		try
		{
			JobContainer  jobContainer = this.jobIndex.get(id);
			if(jobContainer != null)
			{
				if(! jobContainer.getJobControl().isDone())
				{
					return jobContainer.getJob();
				}
			}
		}
		finally 
		{
			jobListReadLock.unlock();
		}
		
		return null;
	}
	
	@Override
	public IQueueJob removeJob(String id)
	{
		jobListWriteLock.lock();
		try
		{
			JobContainer  jobContainer = this.jobIndex.get(id);
			if(jobContainer != null)
			{
				this.jobIndex.remove(id);
				this.jobList.remove(jobContainer);
				
				try
				{
					((MetricImpl)jobContainer.getMetrics()).dispose();
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "dispose metrics", e);
				}
			}
		}
		finally 
		{
			jobListWriteLock.unlock();
		}
		
		return null;
	}

	@Override
	public IQueuedEvent getEvent(String uuid)
	{
		if(uuid == null)
		{
			return null;
		}
		
		if(uuid.isEmpty())
		{
			return null;
		}
		
		Snapshot<QueuedEventImpl> snapshot = this.eventQueue.createImmutableSnapshot(null, null);
		try
		{
			for(IQueuedEvent queuedEvent : snapshot)
			{
				if(uuid.equals(queuedEvent.getUUID()))
				{
					return queuedEvent;
				}
			}
		}
		finally 
		{
			try
			{
				snapshot.close();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"close multichain snapshot",e);
			}
		}
		
		return null;
	}

	@Override
	public List<IQueuedEvent> getEventList(String[] topics, Filter eventFilter, Filter nativeEventFilter)
	{
		boolean match = true;
		List<IQueuedEvent> queryList = new ArrayList<>();
		Snapshot<QueuedEventImpl> snapshot = this.eventQueue.createImmutableSnapshot(null, null);
		try
		{
			for(IQueuedEvent queuedEvent : snapshot)
			{
				match = true;
				if((topics != null) && (topics.length != 0))
				{
					match = false;
					for(String topic : topics)
					{
						if(topic == null)
						{
							continue;
						}
						if("*".equals(topic))
						{
							match = true;
							break;
						}
						if(topic.equals(queuedEvent.getEvent().getTopic()))
						{
							match = true;
							break;
						}
						if(topic.endsWith("*"))
						{
							while(topic.endsWith("*"))
							{
								topic = topic.substring(0, topic.length() -1);
							}
							if(queuedEvent.getEvent().getTopic().startsWith(topic))
							{
								match = true;
								break;
							}
						}
						if(topic.startsWith("*"))
						{
							while(topic.startsWith("*"))
							{
								topic = topic.substring(1, topic.length());
							}
							if(queuedEvent.getEvent().getTopic().endsWith(topic))
							{
								match = true;
								break;
							}
						}
					}
				}
				
				if(! match)
				{
					continue;
				}
				
				if(eventFilter != null)
				{
					match = eventFilter.matches(queuedEvent.getProperties());
				}
				
				if(! match)
				{
					continue;
				}
				
				if(nativeEventFilter != null)
				{
					match = nativeEventFilter.matches(queuedEvent.getNativeEventProperties());
				}
				
				if(! match)
				{
					continue;
				}
				
				queryList.add(queuedEvent);
			}
			return queryList;
		}
		finally 
		{
			try
			{
				snapshot.close();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"close multichain snapshot",e);
			}
		}
		
		
	}	

	@Override
	public Snapshot<IQueuedEvent> getEventSnapshot(String chainName)
	{
		if(Thread.currentThread() == this.queueWorker)
		{
			Snapshot snaphot = (Snapshot)this.eventQueue.chain(chainName).createImmutableSnapshot();
			snapshotsByWorkerThread.add(snaphot);
			return snaphot;
		}
		return (Snapshot)this.eventQueue.chain(chainName).createImmutableSnapshot();
	}

	@Override
	public Snapshot<IQueuedEvent> getEventSnapshotPoll(String chainName)
	{
		if(Thread.currentThread() == this.queueWorker)
		{
			Snapshot snaphot = (Snapshot)this.eventQueue.chain(chainName).createImmutableSnapshotPoll();
			snapshotsByWorkerThread.add(snaphot);
			return snaphot;
		}
		return (Snapshot)this.eventQueue.chain(chainName).createImmutableSnapshotPoll();
	}
	
	public void closeWorkerSnapshots()
	{
		if(this.snapshotsByWorkerThread.isEmpty())
		{
			return;
		}
		
		try
		{
			for(Snapshot<IQueuedEvent> snapshot : this.snapshotsByWorkerThread)
			{
				try
				{
					snapshot.close();
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"close multichain worker snapshots",e);
				}
			}
			this.snapshotsByWorkerThread.clear();
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"close multichain worker snapshots",e);
		}
	}

	@Override
	public boolean removeEvent(String uuid)
	{
		if(uuid == null)
		{
			return false;
		}
		
		QueuedEventImpl removed = null;
		Snapshot<QueuedEventImpl> snapshot = this.eventQueue.createImmutableSnapshot(null, null);
		
		try
		{
			for(QueuedEventImpl event : snapshot)
			{
				if(uuid.equals(event.getUUID()))
				{
					event.getNode().unlinkAllChains();
					removed = event;
					break;
				}
			}
			if(removed == null)
			{
				return false;
			}
		}
		finally 
		{
			try
			{
				snapshot.close();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"close multichain snapshot",e);
			}
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			removedEventListUpdate = true;
			this.removedEventQueue.append(removed);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		this.notifyOrCreateWorker(-1);
	
		
		return true;
	}

	@Override
	public boolean removeEventList(List<String> uuidList)
	{
		if(uuidList == null)
		{
			return false;
		}
		
		if(uuidList.isEmpty())
		{
			return false;
		}
	
		boolean removed = false;
		List<QueuedEventImpl> removeEventList = new ArrayList<QueuedEventImpl>(uuidList.size());
		Snapshot<QueuedEventImpl> snapshot = this.eventQueue.createImmutableSnapshot(null, null);
		try
		{
			for(QueuedEventImpl event : snapshot)
			{
				for(String uuid : uuidList)
				{
					if(uuid == null)
					{
						continue;
					}
					if(uuid.isEmpty())
					{
						continue;
					}
					if(uuid.equals(event.getUUID()))
					{
						removed = true;
						event.getNode().unlinkAllChains();
						removeEventList.add(event);
					}
				}
			}
			if(! removed)
			{
				return false;
			}
		}
		finally 
		{
			try
			{
				snapshot.close();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"close multichain snapshot",e);
			}
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			removedEventListUpdate = true;
			for(QueuedEventImpl event : removeEventList)
			{
				this.removedEventQueue.append(event);
			}
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		this.notifyOrCreateWorker(-1);
		
		
		return true;
	}

	public List<ControllerContainer> getConfigurationList()
	{
		List<ControllerContainer> list = controllerListCopy;
		if(list != null)
		{
			return list; 
		}
		controllerListReadLock.lock();
		try
		{
			list = new ArrayList<ControllerContainer>();
			for(ControllerContainer configuration : controllerList)
			{
				list.add(configuration);
			}
			list = Collections.unmodifiableList(list);
			controllerListCopy = list;
		}
		finally 
		{
			controllerListReadLock.unlock();
		}
		 
		return list;
	}
	
	public List<ServiceContainer> getServiceList()
	{
		List<ServiceContainer> list = serviceListCopy;
		if(list != null)
		{
			return list; 
		}
		serviceListReadLock.lock();
		try
		{
			list = new ArrayList<ServiceContainer>();
			for(ServiceContainer service : serviceList)
			{
				list.add(service);
			}
			list = Collections.unmodifiableList(list);
			serviceListCopy = list;
		}
		finally 
		{
			serviceListReadLock.unlock();
		}
		 
		return list;
	}
	
	public boolean checkTimeOut()
	{
		QueueWorker worker = null;
		this.workerSpoolLock.lock();
		try
		{
			worker = this.queueWorker;
		}
		finally 
		{
			this.workerSpoolLock.unlock();
		}
		
		boolean timeOut = false;
		if(worker != null)
		{
			AtomicBoolean stopJob = new AtomicBoolean(false);
			try
			{
				timeOut = worker.checkTimeOut(stopJob);
				if(timeOut)
				{
					this.workerSpoolLock.lock();
					try
					{
						if(worker == this.queueWorker)
						{
							this.queueWorker = null;
						}
					}
					finally 
					{
						this.workerSpoolLock.unlock();
					}
				}
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"check worker timeout",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"check worker timeout",e);
			}
		}
		return timeOut;
	}
	
	public void dispose()
	{
		if(this.disposed)
		{
			return;
		}
		this.disposed = true;
		
		this.queueScopeListWriteLock.lock();
		try
		{
			if(!  this.queueScopeList.isEmpty())
			{
				List<QueueSessionScopeImpl> scopeCopyList = new ArrayList<QueueSessionScopeImpl>(this.queueScopeList);
				for(QueueSessionScopeImpl scope : scopeCopyList)
				{
					scope.dispose();
				}
				scopeCopyList.clear();
			}
		}
		finally 
		{
			this.queueScopeListWriteLock.unlock();
		}
		
		if(this.queueConfigurationModifyListener != null)
		{
			this.configurationPropertyBlock.removeModifyListener(this.queueConfigurationModifyListener);
		}
		
		for(IEventDispatcherExtension extension : this.eventDispatcher.getEventDispatcherExtensionList())
		{
			try
			{
				extension.unregisterEventQueue(this.eventDispatcher, this);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR, "unregister queue from extension",e);
			}
		}
		
		
		
		stopQueueWorker();
		
		this.closeWorkerSnapshots();
		
		if((this instanceof QueueSessionScopeImpl) && (this.parent != null))
		{
			parent.removeScope((QueueSessionScopeImpl)this);
			
			controllerListReadLock.lock();
			try
			{
				for(ControllerContainer controllerContainer : this.controllerList)
				{
					if(controllerContainer.isImplementingIOnQueueReverse())
					{
						this.eventDispatcher.executeOnQueueReverse(((IOnQueueReverse)controllerContainer.getQueueController()), this);
					}
				}
			}
			finally 
			{
				controllerListReadLock.unlock();
			}
		}
		
		serviceListWriteLock.lock();
		try
		{
			this.serviceList.clear();
			this.serviceIndex.clear();
			this.serviceListCopy = null;
			
		}
		finally 
		{
			serviceListWriteLock.unlock();
		}
		
		try
		{
			jobListReadLock.lock();
			try
			{
				for(Entry<String,JobContainer> jobContainerEntry : this.jobIndex.entrySet())
				{
					try
					{
						jobContainerEntry.getValue().getJobControl().setDone();
						((MetricImpl)jobContainerEntry.getValue().getMetrics()).dispose();
					}
					catch (Exception e) 
					{
						log(LogService.LOG_ERROR, "set queue job / service done", e);
					}
				}
			}
			finally 
			{
				jobListReadLock.unlock();
			}
		}
		catch (Exception e) {}
		
		try
		{
			if(metrics != null)
			{
				metrics.dispose();
			}
		}
		catch (Exception e) {}
		
		try
		{
			eventQueue.dispose();
		}
		catch (Exception e) { log(LogService.LOG_ERROR, "dispose event queue", e);}
		
		try
		{
			newEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose new event queue", e);}
		
		try
		{
			removedEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose removed event queue", e);}
		
		try
		{
			fireEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose fire vent queue", e);}
	
	}
	
	private void removeScope(QueueSessionScopeImpl scope)
	{
		List<QueueSessionScopeImpl> childList = null;
		this.queueScopeListWriteLock.lock();
		try
		{
			UUID scopeId = scope.getScopeId();
			if(scopeId != null)
			{
				List<IQueueSessionScope> copyList = this.queueScopeListCopy;
				if(!((copyList == null) || copyList.isEmpty()))
				{
					QueueSessionScopeImpl scopeTest;
					for(IQueueSessionScope copyItem : copyList)
					{
						scopeTest = (QueueSessionScopeImpl)copyItem;
						if(scopeId.equals(scopeTest.getParentScopeId()))
						{
							if(childList == null)
							{
								childList = new ArrayList<QueueSessionScopeImpl>();
							}
							childList.add(scopeTest);
							scopeTest.unlinkFromParent();
						}
					}
				}
			}
			this.queueScopeIndex.remove(scope.getScopeId());
			while(this.queueScopeList.remove(scope)){}
			this.queueScopeListCopy = Collections.unmodifiableList(new ArrayList<IQueueSessionScope>(this.queueScopeList));
		}
		finally 
		{
			this.queueScopeListWriteLock.unlock();
		}	
		
		if(childList != null)
		{
			for(QueueSessionScopeImpl childItem : childList)
			{
				childItem.dispose();
			}
		}
	}
	
	public void stopQueueWorker()
	{
		this.workerSpoolLock.lock();
		try
		{
			if(this.queueWorker != null)
			{
				this.queueWorker.stopWorker();
				this.queueWorker = null;
			}
		}
		finally 
		{
			this.workerSpoolLock.unlock();
		}
	}

	public EventDispatcherImpl getEventDispatcher()
	{
		return eventDispatcher;
	}

	public void fetchSignalList(List<String> fillList)
	{
		if(! signalListUpdate)
		{
			return;
		}
		
		this.signalListLock.lock();
		try
		{
			signalListUpdate = false;
			for(String signal : this.signalList)
			{
				fillList.add(signal);
			}
			this.signalList.clear();
		}
		finally 
		{
			this.signalListLock.unlock();
		}
	}
	
	public Snapshot<? extends IQueuedEvent> getNewScheduledEventsSnaphot()
	{
		if(! newScheduledListUpdate)
		{
			return null;
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			newScheduledListUpdate = false;
			return this.newEventQueue.createImmutableSnapshotPoll(null, null);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}

	public Snapshot<? extends IQueuedEvent> getRemovedEventsSnapshot()
	{
		if(! removedEventListUpdate)
		{
			return null;
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			removedEventListUpdate = false;
			return this.removedEventQueue.createImmutableSnapshotPoll(null, null);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}
	
	public Snapshot<Event> getFiredEventsSnapshot()
	{
		if(! firedEventListUpdate)
		{
			return null;
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			firedEventListUpdate = false;
			return this.fireEventQueue.createImmutableSnapshotPoll(null, null);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}


	@Override
	public void sendEvent(String topic, Map<String, ?> properties)
	{
		this.metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_SEND_EVENT, System.currentTimeMillis());
		
		ITimer.Context timerContext = null;
		try
		{
			timerContext = getMetrics().timer( IMetrics.METRICS_SEND_EVENT).time();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "metric timer service", e);
		}
		
		Event event = new Event(topic,properties);
		try
		{
			this.eventDispatcher.eventAdmin.sendEvent(event);
		}
		finally 
		{
			if(timerContext != null)
			{
				try {timerContext.stop();}catch (Exception e) {}
			}
			
			this.genericQueueSpoolLock.lock();
			try
			{
				firedEventListUpdate = true;
				this.fireEventQueue.append(event);
			}
			finally 
			{
				this.genericQueueSpoolLock.unlock();
			}
			this.notifyOrCreateWorker(-1);
		}
	}


	@Override
	public void postEvent(String topic, Map<String, ?> properties)
	{
		this.metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_POST_EVENT, System.currentTimeMillis());
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_POST_EVENT).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		Event event = new Event(topic,properties);
		this.eventDispatcher.eventAdmin.postEvent(event);
		this.genericQueueSpoolLock.lock();
		try
		{
			firedEventListUpdate = true;
			this.fireEventQueue.append(event);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		this.notifyOrCreateWorker(-1);
	}
	
	protected void notifyOrCreateWorker(long nextRuntimeStamp)
	{
		boolean notify = false;
		QueueWorker worker = null;
		
		this.workerSpoolLock.lock();
		
		try
		{
			if(this.queueWorker == null)
			{
				if(this.disposed)
				{
					return;
				}
				
				if(this.currentSpooledQueueWorker != null)
				{
					this.currentSpooledQueueWorker.setValid(false);
					this.currentSpooledQueueWorker = null;
				}
				
				QueueWorker queueWorker = this.eventDispatcher.getFromWorkerPool();
				if
				(
					(queueWorker != null) && 
					(queueWorker.isGo()) && 
					(queueWorker.getEventQueue() == null) && 
					queueWorker.setEventQueue(this)
				)
				{
					notify = true;
					this.queueWorker = queueWorker;
				}
				else
				{
					if(queueWorker != null)
					{
						log(LogService.LOG_WARNING, "something goes wrong wakeup a queueworker",null);
						try
						{
							queueWorker.stopWorker();
						}
						catch (Exception e) {this.log(LogService.LOG_ERROR, "stop worker", e);}
						catch (Error e) {this.log(LogService.LOG_ERROR, "stop worker", e);}
					}
					
					queueWorker = new QueueWorker(this);
					queueWorker.start();
					
					this.queueWorker = queueWorker;
				}
			}
			else
			{
				notify = true;
			}
			worker = this.queueWorker;
			
			worker.notifySoftUpdate();
		}
		finally 
		{
			this.workerSpoolLock.unlock();
		}
		
		if(notify)
		{
			if(nextRuntimeStamp < 1)
			{
				worker.notifyUpdate();
			}
			else
			{
				worker.notifyUpdate(nextRuntimeStamp);
			}
		}
	}
	
	protected boolean checkFreeWorker(QueueWorker worker, long nextRun)
	{
		if(worker == null)
		{
			return false;
		}
		
		if(this.privateWorker)
		{
			return false;
		}
		
		if(! worker.isGo())
		{
			return false;
		}
		
		this.workerSpoolLock.lock();
		try
		{
			if(worker != this.queueWorker)
			{
				worker.stopWorker();
				return false;
			}
			
			if(worker.isUpdateNotified || worker.isSoftUpdated)
			{
				return false;
			}
			
			if(this.mainPartitionNewEventQueue.getSize(null) > 0)
			{
				return false;
			}
			if(this.mainPartitionRemovedEventQueue.getSize(null) > 0)
			{
				return false;
			}
			if(this.mainPartitionFireEventQueue.getSize(null) > 0)
			{
				return false;
			}
			if(! this.signalList.isEmpty())
			{
				return false;
			}
			if(! this.onQueueObserveList.isEmpty())
			{
				return false;
			}
			
			if(! worker.setEventQueue(null))
			{
				return false;
			}
			
			if(this.currentSpooledQueueWorker != null)
			{
				this.currentSpooledQueueWorker.setValid(false);
			}
			this.currentSpooledQueueWorker = this.eventDispatcher.scheduleQueueWorker(this, nextRun - QueueWorker.RESCHEDULE_BUFFER_TIME);
			this.queueWorker = null;
			this.eventDispatcher.addToWorkerPool(worker);
			return true;
		}
		finally 
		{
			this.workerSpoolLock.unlock();
		}
	}
	
	protected boolean checkWorkerShutdown(QueueWorker worker)
	{
		if(worker == null)
		{
			return false;
		}
		
		if(this.privateWorker)
		{
			return false;
		}
		
		this.workerSpoolLock.lock();
		try
		{
			if(worker != this.queueWorker)
			{
				worker.stopWorker();
				return false;
			}
			
			if(worker.isUpdateNotified || worker.isSoftUpdated)
			{
				return false;
			}
			if(this.mainPartitionNewEventQueue.getSize(null) > 0)
			{
				return false;
			}
			if(this.mainPartitionRemovedEventQueue.getSize(null) > 0)
			{
				return false;
			}
			if(this.mainPartitionFireEventQueue.getSize(null) > 0)
			{
				return false;
			}
			if(! this.signalList.isEmpty())
			{
				return false;
			}
			if(! this.onQueueObserveList.isEmpty())
			{
				return false;
			}
			
			jobListReadLock.lock();
			try
			{
				if(! jobList.isEmpty())
				{
					return false;
				}
			}
			finally 
			{
				jobListReadLock.unlock();
			}
			
			if(this.currentSpooledQueueWorker != null)
			{
				this.currentSpooledQueueWorker.setValid(false);
			}
			if(!worker.setEventQueue(null))
			{
				return false;
			}
			this.eventDispatcher.addToWorkerPool(worker);
			this.queueWorker = null;
			
			return true;
		}
		finally 
		{
			this.workerSpoolLock.unlock();
		}
	}
	
	public JobContainer getCurrentRunningJob()
	{
		QueueWorker worker = this.queueWorker;
		if(worker == null)
		{
			this.workerSpoolLock.lock();
			try
			{
				worker = this.queueWorker;
			}
			finally 
			{
				this.workerSpoolLock.unlock();
			}
		}
		if(worker == null)
		{
			return null;
		}
		
		return worker.getCurrentRunningJob();
	}


	@Override
	public IEventDispatcher getDispatcher()
	{
		return this.eventDispatcher;
	}

	@Override
	public void signal(String signal)
	{
		this.signalListLock.lock();
		try
		{
			signalListUpdate = true;
			this.signalList.add(signal);
		}
		finally 
		{
			this.signalListLock.unlock();
		}
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_SIGNAL).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric signal", e);
		}
		this.notifyOrCreateWorker(-1);
	}
	
	public void fetchOnQueueObserveList(List<IOnQueueObserve> fillList)
	{
		if(! onQueueObserveListUpdate)
		{
			return;
		}
		
		this.onQueueObserveListLock.lock();
		try
		{
			onQueueObserveListUpdate = false;
			for(IOnQueueObserve onQueueObserve : this.onQueueObserveList)
			{
				fillList.add(onQueueObserve);
			}
			this.onQueueObserveList.clear();
		}
		finally 
		{
			this.onQueueObserveListLock.unlock();
		}
	}
	
	public void addOnQueueObserver(IOnQueueObserve onQueueObserve)
	{
		this.onQueueObserveListLock.lock();
		try
		{
			onQueueObserveListUpdate = true;
			this.onQueueObserveList.add(onQueueObserve);
		}
		finally 
		{
			this.onQueueObserveListLock.unlock();
		}
		
		this.notifyOrCreateWorker(-1);
	}
	
	protected void log(int logServiceLevel,String logMessage, Throwable e)
	{
		this.eventDispatcher.log(logServiceLevel, logMessage, e);
	}
	
	public void registerOnExtension(IEventDispatcherExtension extension)
	{
		this.metrics.registerOnExtension(extension);
	}

	public String getCategory()
	{
		return category;
	}

	public void setCategory(String category)
	{
		this.category = category;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public void touchLastWorkerAction()
	{
		this.lastWorkerAction = System.currentTimeMillis();
	}
	
	public long getLastWorkerAction()
	{
		return this.lastWorkerAction;
	}

	public QueueConfigurationModifyListener getQueueConfigurationModifyListener()
	{
		return queueConfigurationModifyListener;
	}


	public void setQueueConfigurationModifyListener(QueueConfigurationModifyListener queueConfigurationModifyListener)
	{
		this.queueConfigurationModifyListener = queueConfigurationModifyListener;
	}


	@Override
	public IQueueSessionScope createSessionScope(UUID scopeId, String scopeName, IQueueSessionScope parentScope, Map<String, Object> configurationProperties, Map<String, Object> stateProperties, boolean adoptContoller, boolean adoptServices)
	{
		if(disposed)
		{
			return null;
		}
		if(scopeId == null)
		{
			scopeId = UUID.randomUUID();
		}
		
		QueueSessionScopeImpl newScope = null;
		
		this.queueScopeListWriteLock.lock();
		try
		{
			if(disposed)
			{
				return null;
			}
			if(this.queueScopeIndex.get(scopeId) != null)
			{
				return null;
			}
			
			UUID parentScopeId = null;
			if(parentScope != null)
			{
				if((parentScope.getGlobalScope() == this) && (parentScope.getScopeId() != null))
				{
					if(this.queueScopeIndex.get(parentScope.getScopeId()) != null)
					{
						parentScopeId = parentScope.getScopeId();
					}
				}
			}
			
			newScope = new QueueSessionScopeImpl(scopeId,parentScopeId,this, scopeName,adoptContoller,adoptServices,configurationProperties,stateProperties);
			
			this.queueScopeList.add(newScope);
			this.queueScopeListCopy = Collections.unmodifiableList(new ArrayList<IQueueSessionScope>(this.queueScopeList));
			this.queueScopeIndex.put(scopeId, newScope);
		}
		finally 
		{
			this.queueScopeListWriteLock.unlock();
		}
		
		if((configurationProperties != null) && (!configurationProperties.isEmpty()))
		{
			this.eventDispatcher.onConfigurationModify(newScope,configurationProperties.keySet().toArray(new String[configurationProperties.size()]));
		}
		
		return newScope;
	}


	@Override
	public List<IQueueSessionScope> getSessionScopes()
	{
		return this.queueScopeListCopy;
	}


	@Override
	public List<IQueueSessionScope> getSessionScopes(Filter filter)
	{
		List<IQueueSessionScope> copyList = this.queueScopeListCopy;
		if(copyList.isEmpty())
		{
			return copyList;
		}
		if(filter == null)
		{
			return copyList;
		}
		List<IQueueSessionScope> filterList = new ArrayList<IQueueSessionScope>();
		for(IQueueSessionScope scope : copyList)
		{
			if(scope.getConfigurationPropertyBlock().isEmpty())
			{
				continue;
			}
			try
			{
				if(filter.matches(scope.getConfigurationPropertyBlock().getProperties()))
				{
					filterList.add(scope);
				}
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"get scopelist by filter",e);
			}
		}
		return Collections.unmodifiableList(filterList);
	}
	
	protected List<IQueueSessionScope> getChildSessionScopes(UUID parentScopeId)
	{
		List<IQueueSessionScope> copyList = this.queueScopeListCopy;
		if(copyList.isEmpty())
		{
			return copyList;
		}
		List<IQueueSessionScope> filterList = new ArrayList<IQueueSessionScope>();
		for(IQueueSessionScope scope : copyList)
		{
			UUID currentParentScopeId = ((QueueSessionScopeImpl)scope).getParentScopeId();
			if((parentScopeId == null) && (currentParentScopeId == null))
			{
				filterList.add(scope);
				continue;
			}
			if(currentParentScopeId == null)
			{
				continue;
			}
			if(! currentParentScopeId.equals(parentScopeId))
			{
				continue;
			}
			filterList.add(scope);
		}
		return Collections.unmodifiableList(filterList);
	}

	@Override
	public IQueueSessionScope getSessionScope(UUID scopeId)
	{
		this.queueScopeListReadLock.lock();
		try
		{
			return this.queueScopeIndex.get(scopeId);
		}
		finally 
		{
			this.queueScopeListReadLock.unlock();
		}
	}

	public int getEventListLimit()
	{
		return eventListLimit;
	}

	public void setEventListLimit(int eventListLimit)
	{
		this.eventListLimit = eventListLimit;
	}
	
	public ConsumeEventHandler addConsumeEventHandler(ConsumeEventHandler consumeEventHandler)
	{
		consumeEventHandlerListWriteLock.lock();
		try
		{
			if(this.consumeEventHandlerList == null)
			{
				this.consumeEventHandlerList = new ArrayList<ConsumeEventHandler>();
			}
			this.consumeEventHandlerList.add(consumeEventHandler);
			consumeEventHandlerCopyList = null;
			return consumeEventHandler;
		}
		finally
		{
			consumeEventHandlerListWriteLock.unlock();
		}
	}
	
	public void removeConsumeEventHandler(ConsumeEventHandler consumeEventHandler)
	{
		consumeEventHandlerListWriteLock.lock();
		try
		{
			consumeEventHandlerCopyList = null;
			
			if(this.consumeEventHandlerList == null)
			{
				return ;
			}
			
			while(this.consumeEventHandlerList.remove(consumeEventHandler)) {};
		}
		finally
		{
			consumeEventHandlerListWriteLock.unlock();
		}
	}
	
	public List<ConsumeEventHandler> getConsumeEventHandlerList()
	{
		if(this.consumeEventHandlerList == null)
		{
			return null;
		}
		consumeEventHandlerListReadLock.lock();
		try
		{
			if(consumeEventHandlerCopyList == null)
			{
				consumeEventHandlerCopyList = Collections.unmodifiableList(new ArrayList<ConsumeEventHandler>(consumeEventHandlerList));
			}
			return consumeEventHandlerCopyList;
		}
		finally 
		{
			consumeEventHandlerListReadLock.unlock();
		}
		
	}
	
	@Override
	public ILinkageDefinitionDispatcherBuilder registerLinkageDefinitionDispatcher(String linkageDefinitionDispatcherId)
	{
		if(linkageDefinitionDispatcherId == null)
		{
			return null;
		}
		if(linkageDefinitionDispatcherId.isEmpty())
		{
			return null;
		}
		return new LinkageDefinitionDispatcherBuilder(linkageDefinitionDispatcherId);
	}

	@Override
	public void unregisterLinkageDefinitionDispatcher(String linkageDefinitionDispatcherId)
	{
		this.linkageDefinitionDispatcherIndex.remove(linkageDefinitionDispatcherId);
	}

	public class LinkageDefinitionDispatcherBuilder implements ILinkageDefinitionDispatcherBuilder
	{
		private String linkageDefinitionDispatcherId;
		private LinkedList<Filter> filterList;
		private LinkedList<AddLinkageDefinition> addLinkageDefinitionList;
		private LinkedList<String> removeLinkageDefinitionList;
		
		public LinkageDefinitionDispatcherBuilder(String linkageDefinitionDispatcherId)
		{
			super();
			
			this.linkageDefinitionDispatcherId = linkageDefinitionDispatcherId;
			this.filterList = new LinkedList<Filter>();
			this.addLinkageDefinitionList = new LinkedList<AddLinkageDefinition>();
			this.removeLinkageDefinitionList = new LinkedList<String>();
		}

		@Override
		public ILinkageDefinitionDispatcher onMatchFilter(Filter filter)
		{
			if(filter != null)
			{
				this.filterList.add(filter);
			}
			return this;
		}
		
		@Override
		public ILinkageDefinitionDispatcher addLinkageDefinition(String chainName, String partitionName)
		{
			this.addLinkageDefinitionList.add(new AddLinkageDefinition(chainName, partitionName));
			return this;
		}
		
		@Override
		public ILinkageDefinitionDispatcher removeLinkageDefinition(String chainName)
		{
			this.removeLinkageDefinitionList.add(chainName);
			return this;
		}

		@Override
		public IQueue build()
		{
			if((this.removeLinkageDefinitionList.isEmpty() && (this.addLinkageDefinitionList.isEmpty())) || this.filterList.isEmpty() || QueueImpl.this.linkageDefinitionDispatcherIndex.containsKey(linkageDefinitionDispatcherId))
			{
				this.removeLinkageDefinitionList.clear();
				this.addLinkageDefinitionList.clear();
				this.filterList.clear();
				
				return QueueImpl.this;
			}
			
			QueueImpl.this.linkageDefinitionDispatcherIndex.put(linkageDefinitionDispatcherId, new LinkageDefinitionDispatcherImpl(filterList, addLinkageDefinitionList, removeLinkageDefinitionList));
			
			return QueueImpl.this;
		}
		
		protected class AddLinkageDefinition
		{
			private AddLinkageDefinition(String chainName,String partitionName)
			{
				super();
				this.chainName = chainName;
				this.partitionName = partitionName;
			}
			String chainName;
			String partitionName;
		}
		
	}
}
