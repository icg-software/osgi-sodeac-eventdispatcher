/*******************************************************************************
 * Copyright (c) 2017, 2019 Sebastian Palarus
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import org.sodeac.multichainlist.ChainView;
import org.sodeac.multichainlist.Linker;
import org.sodeac.multichainlist.MultiChainList;
import org.sodeac.multichainlist.Node;
import org.sodeac.multichainlist.Snapshot;

import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IGauge;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueChildScope;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.ITaskControl.ExecutionTimestampSource;
import org.sodeac.eventdispatcher.api.IQueueEventResult;
import org.sodeac.eventdispatcher.api.ITimer;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.eventdispatcher.api.QueueIsFullException;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;
import org.sodeac.eventdispatcher.extension.api.IExtensibleQueue;
import org.sodeac.eventdispatcher.impl.ControllerContainer.ControllerFilterObjects;
import org.sodeac.eventdispatcher.impl.ServiceContainer.ServiceFilterObjects;
import org.sodeac.eventdispatcher.impl.TaskControlImpl.RescheduleTimestampPredicate;
import org.sodeac.eventdispatcher.impl.TaskControlImpl.ScheduleTimestampPredicate;

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
		this.chainEventQueue = this.eventQueue.createChainView(null,this.eventQueue.getPartition(null));
		this.eventQueue.lockDefaultLinker();
		this.chainEventQueue.lockDefaultLinker();
		
		this.newEventQueue = new MultiChainList<>();
		this.chainNewEventQueue = this.newEventQueue.createChainView(null,this.newEventQueue.getPartition(null));
		this.newEventQueue.lockDefaultLinker();
		this.chainNewEventQueue.lockDefaultLinker();
		
		this.removedEventQueue = new MultiChainList<>();
		this.chainRemovedEventQueue = this.removedEventQueue.createChainView(null,this.removedEventQueue.getPartition(null));
		this.removedEventQueue.lockDefaultLinker();
		this.chainRemovedEventQueue.lockDefaultLinker();
		
		this.fireEventQueue = new MultiChainList<>();
		this.chainFireEventQueue = this.fireEventQueue.createChainView(null,this.fireEventQueue.getPartition(null));
		this.fireEventQueue.lockDefaultLinker();
		this.chainFireEventQueue.lockDefaultLinker();
		
		this.taskList = new ArrayList<TaskContainer>();
		this.taskIndex = new HashMap<String,TaskContainer>();
		this.taskListLock = new ReentrantReadWriteLock(true);
		this.taskListReadLock = this.taskListLock.readLock();
		this.taskListWriteLock = this.taskListLock.writeLock();
		
		this.signalQueue = new MultiChainList<String>();
		this.chainSignalQueue = this.signalQueue.createChainView(null,this.signalQueue.getPartition(null));
		this.signalQueue.lockDefaultLinker();
		this.chainSignalQueue.lockDefaultLinker();
		
		this.onQueueAttachQueue = new MultiChainList<>();
		this.chainOnQueueAttachQueue = this.onQueueAttachQueue.createChainView(null,this.onQueueAttachQueue.getPartition(null));
		this.onQueueAttachQueue.lockDefaultLinker();
		this.chainOnQueueAttachQueue.lockDefaultLinker();
		
		this.lastWorkerAction = System.currentTimeMillis();
		
		this.queueScopeList = new ArrayList<QueueSessionScopeImpl>();
		this.queueScopeIndex = new HashMap<UUID,QueueSessionScopeImpl>();
		this.queueScopeListCopy = Collections.unmodifiableList(new ArrayList<IQueueChildScope>());
		this.queueScopeListLock = new ReentrantReadWriteLock(true);
		this.queueScopeListReadLock = this.queueScopeListLock.readLock();
		this.queueScopeListWriteLock = this.queueScopeListLock.writeLock();
		
		this.consumeEventHandlerListLock = new ReentrantReadWriteLock();
		this.consumeEventHandlerListWriteLock = consumeEventHandlerListLock.writeLock();
		this.consumeEventHandlerListReadLock = consumeEventHandlerListLock.readLock();
		
		this.dummyQueueResult = new DummyQueueEventResult();
		
		PropertyBlockImpl qualityValues = (PropertyBlockImpl)eventDispatcher.createPropertyBlock();
		qualityValues.setProperty(IMetrics.QUALITY_VALUE_CREATED, System.currentTimeMillis());
		this.metrics = new MetricImpl(this,qualityValues, null,enableMetrics);
		
		this.configurationPropertyBlock = (PropertyBlockImpl)eventDispatcher.createPropertyBlock();
		if(configurationProperties != null)
		{
			this.configurationPropertyBlock.setPropertyEntrySet(configurationProperties.entrySet(), false);
		}
		
		this.statePropertyBlock = (PropertyBlockImpl)eventDispatcher.createPropertyBlock();
		if(stateProperties != null)
		{
			this.statePropertyBlock.setPropertyEntrySet(stateProperties.entrySet(),false);
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
		
		this.snapshotsByWorkerThread = new LinkedList<Snapshot<IQueuedEvent>>();
		this.sharedEventLock = new ReentrantLock(true);
		
		this.chainDispatcher = new ChainDispatcher(this);
		this.disabledRules = new HashSet<String>();
		
		this.registrationTypes = new RegistrationTypes();
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
	
	
	protected MultiChainList<QueuedEventImpl> eventQueue = null;
	protected ChainView<QueuedEventImpl> chainEventQueue = null;
	
	protected MultiChainList<QueuedEventImpl> newEventQueue = null;
	protected ChainView<QueuedEventImpl> chainNewEventQueue = null;
	
	protected MultiChainList<QueuedEventImpl> removedEventQueue = null;
	protected ChainView<QueuedEventImpl> chainRemovedEventQueue = null;
	
	protected MultiChainList<Event> fireEventQueue = null;
	protected ChainView<Event> chainFireEventQueue = null;
	
	protected List<TaskContainer> taskList = null;
	protected Map<String,TaskContainer> taskIndex = null;
	protected ReentrantReadWriteLock taskListLock;
	protected ReadLock taskListReadLock;
	protected WriteLock taskListWriteLock;
	
	protected volatile boolean signalListUpdate = false;
	protected MultiChainList<String> signalQueue = null;
	protected ChainView<String> chainSignalQueue = null;
	
	protected volatile boolean onQueueAttachListUpdate = false;
	protected MultiChainList<IOnQueueAttach> onQueueAttachQueue = null;
	protected ChainView<IOnQueueAttach> chainOnQueueAttachQueue = null;
	
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
	protected volatile List<IQueueChildScope> queueScopeListCopy = null;
	protected ReentrantReadWriteLock queueScopeListLock;
	protected ReadLock queueScopeListReadLock;
	protected WriteLock queueScopeListWriteLock;
	
	private volatile List<ConsumeEventHandler> consumeEventHandlerList = null;
	private volatile List<ConsumeEventHandler> consumeEventHandlerCopyList = null;
	private ReentrantReadWriteLock consumeEventHandlerListLock;
	private ReadLock consumeEventHandlerListReadLock;
	private WriteLock consumeEventHandlerListWriteLock;
	
	protected DummyQueueEventResult dummyQueueResult = null;
	
	protected QueueImpl parent = null;
	protected LinkedList<Snapshot<IQueuedEvent>> snapshotsByWorkerThread;
	
	protected ReentrantLock sharedEventLock = null;
	protected ChainDispatcher chainDispatcher = null;
	protected Set<String> disabledRules = null;
	
	protected volatile RegistrationTypes registrationTypes = null;
	
	@Override
	public void queueEvent(Event event)
	{
		QueuedEventImpl queuedEvent = null;
		
		if(this.eventListLimit <= this.eventQueue.getNodeSize())
		{
			throw new QueueIsFullException(this.queueId, this.eventListLimit);
		}
		queuedEvent = new QueuedEventImpl(event,this);
		queuedEvent.setScheduleResultObject(dummyQueueResult);
		queuedEvent.setNode(this.chainDispatcher.getLinker(queuedEvent).append(queuedEvent));
			
		try
		{
			getMetrics().meter(IMetrics.METRICS_SCHEDULE_EVENT).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		if(this.registrationTypes.onQueuedEvent)
		{
			this.newEventQueue.defaultLinker().append(queuedEvent);
			this.newScheduledListUpdate = true; 
			this.notifyOrCreateWorker(-1);
		}
	}
	
	@Override
	public void queueEvents(Collection<Event> events)
	{
		if(events == null)
		{
			return;
		}
		if(events.isEmpty())
		{
			return;
		}
		
		List<QueuedEventImpl> queuedEventList = new ArrayList<QueuedEventImpl>(events.size());
		if(this.eventListLimit < ((eventQueue.getNodeSize()) + events.size()))
		{
			throw new QueueIsFullException(this.queueId, this.eventListLimit);
		}
		
		boolean linkerIsUnique = true;
		Linker<QueuedEventImpl> uniqueLinker = null;
		Linker<QueuedEventImpl> linker = null;
		for(Event event : events)
		{
			QueuedEventImpl queuedEvent = new QueuedEventImpl(event,this);
			queuedEvent.setScheduleResultObject(dummyQueueResult);
			linker = chainDispatcher.getLinker(queuedEvent);
			queuedEvent.setLinker(linker);
			if(uniqueLinker == null)
			{
				uniqueLinker = linker;
			}
			else if(linkerIsUnique)
			{
				if(linker != uniqueLinker)
				{
					linkerIsUnique = false;
				}
			}
			
			queuedEventList.add(queuedEvent);
		}
		
		if(linkerIsUnique)
		{
			Node<QueuedEventImpl>[] nodes = uniqueLinker.appendAll(queuedEventList);
			for(int i = 0; i < nodes.length; i++)
			{
				queuedEventList.get(i).setNode(nodes[i]);
				queuedEventList.get(i).setLinker(null);
			}
		}
		else
		{
			for(QueuedEventImpl queuedEvent : queuedEventList)
			{
				queuedEvent.setNode(this.chainDispatcher.getLinker(queuedEvent).append(queuedEvent));
			}
		}	
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_SCHEDULE_EVENT).mark(events.size());
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		if(this.registrationTypes.onQueuedEvent)
		{
			this.newEventQueue.defaultLinker().appendAll(queuedEventList);
			this.newScheduledListUpdate = true;
			this.notifyOrCreateWorker(-1);
		}
	}
	
	@Override
	public Future<IQueueEventResult> queueEventWithResult(Event event)
	{
		QueuedEventImpl queuedEvent = null;
		QueueEventResultImpl resultImpl = new QueueEventResultImpl();
		
		if(this.eventListLimit <= this.eventQueue.getNodeSize())
		{
			throw new QueueIsFullException(this.queueId, this.eventListLimit);
		}
		queuedEvent = new QueuedEventImpl(event,this);
		queuedEvent.setScheduleResultObject(resultImpl);
		queuedEvent.setNode(this.chainDispatcher.getLinker(queuedEvent).append(queuedEvent));
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_SCHEDULE_EVENT).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		if(this.registrationTypes.onQueuedEvent)
		{
			this.newEventQueue.defaultLinker().append(queuedEvent);
			this.newScheduledListUpdate = true;
			this.notifyOrCreateWorker(-1);
		}
		
		return this.eventDispatcher.createFutureOfScheduleResult(resultImpl);
	}
	
	@Override
	public Future<IQueueEventResult> queueEventsWithResult(Collection<Event> events)
	{
		List<QueuedEventImpl> queuedEventList = new ArrayList<QueuedEventImpl>(events.size());
		QueueEventResultImpl resultImpl = new QueueEventResultImpl();
		
		if(this.eventListLimit < ((eventQueue.getNodeSize()) + events.size()))
		{
			throw new QueueIsFullException(this.queueId, this.eventListLimit);
		}
		
		boolean linkerIsUnique = true;
		Linker<QueuedEventImpl> uniqueLinker = null;
		Linker<QueuedEventImpl> linker = null;
		for(Event event : events)
		{
			QueuedEventImpl queuedEvent = new QueuedEventImpl(event,this);
			queuedEvent.setScheduleResultObject(resultImpl);
			linker = chainDispatcher.getLinker(queuedEvent);
			queuedEvent.setLinker(linker);
			if(uniqueLinker == null)
			{
				uniqueLinker = linker;
			}
			else if(linkerIsUnique)
			{
				if(linker != uniqueLinker)
				{
					linkerIsUnique = false;
				}
			}
			
			queuedEventList.add(queuedEvent);
		}
		
		if(linkerIsUnique)
		{
			Node<QueuedEventImpl>[] nodes = uniqueLinker.appendAll(queuedEventList);
			for(int i = 0; i < nodes.length; i++)
			{
				queuedEventList.get(i).setNode(nodes[i]);
				queuedEventList.get(i).setLinker(null);
			}
		}
		else
		{
			for(QueuedEventImpl queuedEvent : queuedEventList)
			{
				queuedEvent.setNode(this.chainDispatcher.getLinker(queuedEvent).append(queuedEvent));
			}
		}
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_SCHEDULE_EVENT).mark(events.size());
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		if(this.registrationTypes.onQueuedEvent)
		{
			this.newEventQueue.defaultLinker().appendAll(queuedEventList);
			this.newScheduledListUpdate = true;
			this.notifyOrCreateWorker(-1);
		}
		
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
			
			this.recalcRegistrationTypes();
			
			// merge OSGi event subscriptions
			if(subscriptionMerger != null)
			{
				subscriptionMerger.merge();
			}
			
			// enable disable metrics
			setQueueMetricsEnabled(! metricsDisabledMerger.disableMetrics());
			
			// attachEvent
			if(controllerContainer.isImplementingIOnQueueAttach())
			{
				addOnQueueAttach((IOnQueueAttach)controllerContainer.getQueueController());
			}
			
			// chain dispatcher
			if((controllerContainer.getChainDispatcherRuleConfigurationList() != null) && (! controllerContainer.getChainDispatcherRuleConfigurationList().isEmpty()))
			{
				chainDispatcher.reset();
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
			
			// IOnQueueDetach
			
			if(unlinkFromQueue.isImplementingIOnQueueDetach())
			{
				this.eventDispatcher.executeOnQueueDetach((IOnQueueDetach)unlinkFromQueue.getQueueController(), this);
			}
			
			// chain dispatcher
			if((configurationContainer.getChainDispatcherRuleConfigurationList() != null) && (! configurationContainer.getChainDispatcherRuleConfigurationList().isEmpty()))
			{
				chainDispatcher.reset();
			}
			
			this.recalcRegistrationTypes();
			
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
			taskListWriteLock.lock();
			
			this.enableMetrics = enabled;
			
			// don't switch of on simple tasks
			for(TaskContainer taskContainer : taskList)
			{
				if(taskContainer.getTask() instanceof IQueueService)
				{
					continue;
				}
				
				try
				{
					IMetrics taskMetrics = taskContainer.getMetrics();
					if(taskMetrics == null)
					{
						continue;
					}
					if(taskMetrics instanceof MetricImpl)
					{
						if(enabled)
						{
							((MetricImpl)taskMetrics).enable();
						}
						else
						{
							((MetricImpl)taskMetrics).disable();
						}
					}
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR, "enabletask queue metrics", e);
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
			taskListWriteLock.unlock();
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
				
				// chain dispatcher
				if((serviceContainer.getChainDispatcherRuleConfigurationList() != null) && (! serviceContainer.getChainDispatcherRuleConfigurationList().isEmpty()))
				{
					chainDispatcher.reset();
				}
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
				this.rescheduleTask(serviceId, System.currentTimeMillis() + delay, timeout, hbtimeout);
				return;
			}
			IPropertyBlock servicePropertyBlock = this.eventDispatcher.createPropertyBlock();
			for(Entry<String,?> entry : properties.entrySet())
			{
				servicePropertyBlock.setProperty(entry.getKey(), entry.getValue());
			}
			if(configuration.getPeriodicRepetitionIntervalMS() < 0L)
			{
				servicePropertyBlock.removeProperty(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL);
			}
			else
			{
				servicePropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL, configuration.getPeriodicRepetitionIntervalMS());
			}
			
			this.scheduleTask(serviceId, queueService, servicePropertyBlock, System.currentTimeMillis() + delay, timeout, hbtimeout);
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
			
			// chain dispatcher
			if((serviceContainer.getChainDispatcherRuleConfigurationList() != null) && (! serviceContainer.getChainDispatcherRuleConfigurationList().isEmpty()))
			{
				chainDispatcher.reset();
			}
			
			taskListReadLock.lock();
			try
			{
				for(Entry<String,TaskContainer> taskContainerEntry : this.taskIndex.entrySet())
				{
					try
					{
						if(taskContainerEntry.getValue().getTask() == serviceContainer.getQueueService())
						{
							taskContainerEntry.getValue().getTaskControl().setDone();
							((MetricImpl)taskContainerEntry.getValue().getMetrics()).dispose();
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
				taskListReadLock.unlock();
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

	protected int cleanDoneTasks()
	{
		List<TaskContainer> toRemove = null;
		taskListWriteLock.lock();
		try
		{
			
			for(TaskContainer taskContainer : this.taskList)
			{
				if(taskContainer.getTaskControl().isDone())
				{
					if(toRemove == null)
					{
						toRemove = new ArrayList<TaskContainer>();
					}
					toRemove.add(taskContainer);
				}
			}
			
			if(toRemove == null)
			{
				return 0;
			}
			
			for(TaskContainer taskContainer : toRemove)
			{
				String id = taskContainer.getId();
				this.taskList.remove(taskContainer);
				
				TaskContainer containerById = this.taskIndex.get(id);
				if(containerById == null)
				{
					continue;
				}
				if(containerById == taskContainer)
				{
					this.taskIndex.remove(id);
				}
				
				try
				{
					((MetricImpl)taskContainer.getMetrics()).dispose();
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
			taskListWriteLock.unlock();
		}
	}
	
	protected long getDueTasks(List<TaskContainer> dueTaskList)
	{
		taskListReadLock.lock();
		long timeStamp = System.currentTimeMillis();
		long nextRun = timeStamp + QueueWorker.DEFAULT_WAIT_TIME;
		try
		{
			
			for(TaskContainer taskContainer : taskList)
			{
				if(taskContainer.getTaskControl().isDone())
				{
					continue;
				}
				long executionTimeStampIntern = taskContainer.getTaskControl().getExecutionTimeStampIntern();
				if(executionTimeStampIntern < nextRun)
				{
					nextRun = executionTimeStampIntern;
				}
				
				if( executionTimeStampIntern <= timeStamp)
				{
					dueTaskList.add(taskContainer);
				}
			}
		}
		finally 
		{
			taskListReadLock.unlock();
		}
		
		return nextRun;
	}
	
	protected long getNextRun()
	{
		taskListReadLock.lock();
		long timeStamp = System.currentTimeMillis();
		long nextRun = timeStamp + QueueWorker.DEFAULT_WAIT_TIME;
		try
		{
			
			for(TaskContainer taskContainer : taskList)
			{
				if(taskContainer.getTaskControl().isDone())
				{
					continue;
				}
				long executionTimeStampIntern = taskContainer.getTaskControl().getExecutionTimeStampIntern();
				if(executionTimeStampIntern < nextRun)
				{
					nextRun = executionTimeStampIntern;
				}
			}
		}
		finally 
		{
			taskListReadLock.unlock();
		}
		
		return nextRun;
	}
	
	@Override
	public IPropertyBlock getTaskPropertyBlock(String id)
	{
		taskListReadLock.lock();
		try
		{
			TaskContainer  taskContainer = this.taskIndex.get(id);
			if(taskContainer != null)
			{
				if(! taskContainer.getTaskControl().isDone())
				{
					return taskContainer.getPropertyBlock();
				}
			}
		}
		finally 
		{
			taskListReadLock.unlock();
		}
		
		return null;
	}
	
	@Override
	public List<IQueueTask> getTaskList(Filter filter)
	{
		List<IQueueTask> queryTaskList = new ArrayList<IQueueTask>();
		
		taskListReadLock.lock();
		try
		{
			for(TaskContainer taskContainer : taskList)
			{
				if(taskContainer.getTaskControl().isDone()) 
				{
					continue;
				}
				if(filter == null)
				{
					queryTaskList.add(taskContainer.getTask());
				}
				else if(filter.matches(taskContainer.getPropertyBlock().getProperties()))
				{
					queryTaskList.add(taskContainer.getTask());
				}
			}
			return Collections.unmodifiableList(queryTaskList);
		}
		finally 
		{
			taskListReadLock.unlock();
		}
	}
	
	@Override
	public Map<String,IQueueTask> getTaskIndex(Filter filter)
	{
		Map<String,IQueueTask> queryTaskIndex = new HashMap<String,IQueueTask>();
		
		taskListReadLock.lock();
		try
		{
			String id = null;
			TaskContainer taskContainer = null;
			
			for(Entry<String,TaskContainer> taskContainerEntry : this.taskIndex.entrySet())
			{
				id = taskContainerEntry.getKey();
				taskContainer = taskContainerEntry.getValue();
				
				if(taskContainer.getTaskControl().isDone()) 
				{
					continue;
				}
				if(filter == null)
				{
					queryTaskIndex.put(id,taskContainer.getTask());
				}
				else if(filter.matches(taskContainer.getPropertyBlock().getProperties()))
				{
					queryTaskIndex.put(id,taskContainer.getTask());
				}
			}
			return Collections.unmodifiableMap(queryTaskIndex);
		}
		finally 
		{
			taskListReadLock.unlock();
		}
	}


	@Override
	public String scheduleTask(IQueueTask task)
	{
		return scheduleTask(null,task);
	}
	
	@Override
	public String scheduleTask(String id, IQueueTask task)
	{
		return scheduleTask(id,task, null, -1, -1, -1);
	}
	
	@Override
	public String scheduleTask(String id, IQueueTask task, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut )
	{
		return scheduleTask(id,task, propertyBlock, executionTimeStamp, timeOutValue, heartBeatTimeOut, false);
	}
	
	@Override
	public String scheduleTask(String id, IQueueTask task, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut, boolean stopOnTimeOut )
	{

		TaskContainer taskContainer = null;
		
		taskListWriteLock.lock();
		try
		{
			TaskContainer toRemove =  null;
			for(TaskContainer alreadyInList : this.taskList)
			{
				if(alreadyInList.getTask() == task)
				{
					if(alreadyInList.getTaskControl().isDone())
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
				this.taskIndex.remove(toRemove.getId());
				this.taskList.remove(toRemove);
				
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
				taskContainer = new TaskContainer();
			}
			else
			{
				taskContainer = this.taskIndex.get(id);
				if(taskContainer != null)
				{
					if(taskContainer.getTaskControl().isDone())
					{
						this.taskIndex.remove(taskContainer.getId());
						this.taskList.remove(taskContainer);
						
						try
						{
							((MetricImpl)taskContainer.getMetrics()).dispose();
						}
						catch(Exception e)
						{
							log(LogService.LOG_ERROR, "dispose metrics", e);
						}
						
						taskContainer = null;
					}
					else
					{
						return id;
					}
				}
				
				taskContainer = new TaskContainer();
				taskContainer.setNamedTask(true);
			}
			
			PropertyBlockImpl qualityValues = (PropertyBlockImpl)this.getDispatcher().createPropertyBlock();
			qualityValues.setProperty(IMetrics.QUALITY_VALUE_CREATED, System.currentTimeMillis());
			
			
			MetricImpl metric = new MetricImpl(this,qualityValues, id, this.enableMetrics);
			
			if(propertyBlock == null)
			{
				propertyBlock = (PropertyBlockImpl)this.getDispatcher().createPropertyBlock();
			}
			
			TaskControlImpl taskControl = new TaskControlImpl(propertyBlock);
			if(executionTimeStamp > 0)
			{
				taskControl.setExecutionTimeStamp(executionTimeStamp, ExecutionTimestampSource.SCHEDULE, ScheduleTimestampPredicate.getInstance());
			}
			if(heartBeatTimeOut > 0)
			{
				taskControl.setHeartbeatTimeout(heartBeatTimeOut);
			}
			if(timeOutValue > 0)
			{
				taskControl.setTimeout(timeOutValue);
			}
			
			taskControl.setStopOnTimeoutFlag(stopOnTimeOut);
			
			propertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_TASK_ID, id);
			
			taskContainer.setId(id);
			taskContainer.setTask(task);
			taskContainer.setMetrics(metric);
			taskContainer.setPropertyBlock(propertyBlock);
			taskContainer.setTaskControl(taskControl);
			
			qualityValues.setProperty(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, -1L);
			
			if(taskContainer.isNamedTask())
			{
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_CREATED);
					}
				}, IMetrics.GAUGE_TASK_CREATED);
				
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_FINISHED_TIMESTAMP);
					}
				}, IMetrics.GAUGE_TASK_FINISHED);
				
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_STARTED_TIMESTAMP);
					}
				}, IMetrics.GAUGE_TASK_STARTED);
				
				metric.registerGauge(new IGauge<Long>()
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
					}
				}, IMetrics.GAUGE_TASK_LAST_HEARTBEAT);
			}
		}
		finally 
		{
			taskListWriteLock.unlock();
		}
		
		taskContainer.getTask().configure(this, id, taskContainer.getMetrics(), taskContainer.getPropertyBlock(), taskContainer.getTaskControl());
		
		taskListWriteLock.lock();
		try
		{
			taskList.add(taskContainer);
			taskIndex.put(id, taskContainer);
		}
		finally 
		{
			taskListWriteLock.unlock();
		}
		notifyOrCreateWorker(executionTimeStamp);
		
		return id;
	}
	
	@Override
	public IQueueTask rescheduleTask(String id, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut)
	{
		TaskContainer taskContainer = null;
		
		if((id == null) || (id.isEmpty()))
		{
			return null;
		}
		
		taskListWriteLock.lock();
		try
		{
			taskContainer = this.taskIndex.get(id);
			if(taskContainer == null)
			{
				return null;
			}
			
			if(taskContainer.getTaskControl().isDone())
			{
				this.taskIndex.remove(taskContainer.getId());
				this.taskList.remove(taskContainer);
				
				try
				{
					((MetricImpl)taskContainer.getMetrics()).dispose();
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "dispose metrics", e);
				}
				return null;
			}
			
			TaskControlImpl taskControl = taskContainer.getTaskControl();
			
			if(heartBeatTimeOut > 0)
			{
				taskControl.setHeartbeatTimeout(heartBeatTimeOut);
			}
			if(timeOutValue > 0)
			{
				taskControl.setTimeout(timeOutValue);
			}
			
			if(executionTimeStamp > 0)
			{
				if(taskControl.setExecutionTimeStamp(executionTimeStamp, ExecutionTimestampSource.RESCHEDULE, RescheduleTimestampPredicate.getInstance()))
				{
					this.notifyOrCreateWorker(executionTimeStamp);
				}
			}
			
			return taskContainer.getTask();
		}
		finally 
		{
			taskListWriteLock.unlock();
		}
	}
	
	@Override
	public IQueueTask getTask(String id)
	{

		taskListReadLock.lock();
		try
		{
			TaskContainer  taskContainer = this.taskIndex.get(id);
			if(taskContainer != null)
			{
				if(! taskContainer.getTaskControl().isDone())
				{
					return taskContainer.getTask();
				}
			}
		}
		finally 
		{
			taskListReadLock.unlock();
		}
		
		return null;
	}
	
	@Override
	public IQueueTask removeTask(String id)
	{
		taskListWriteLock.lock();
		try
		{
			TaskContainer  taskContainer = this.taskIndex.get(id);
			if(taskContainer != null)
			{
				this.taskIndex.remove(id);
				this.taskList.remove(taskContainer);
				
				try
				{
					((MetricImpl)taskContainer.getMetrics()).dispose();
				}
				catch(Exception e)
				{
					log(LogService.LOG_ERROR, "dispose metrics", e);
				}
			}
		}
		finally 
		{
			taskListWriteLock.unlock();
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
		
		Snapshot<QueuedEventImpl> snapshot = this.chainEventQueue.createImmutableSnapshot();
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
		Snapshot<QueuedEventImpl> snapshot = this.chainEventQueue.createImmutableSnapshot();
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Snapshot<IQueuedEvent> getEventSnapshot(String chainName)
	{
		if(Thread.currentThread() == this.queueWorker)
		{
			Snapshot snaphot = (Snapshot)this.eventQueue.createChainView(chainName).createImmutableSnapshot();
			snapshotsByWorkerThread.add(snaphot);
			return snaphot;
		}
		return (Snapshot)this.eventQueue.createChainView(chainName).createImmutableSnapshot();
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Snapshot<IQueuedEvent> getEventSnapshotPoll(String chainName)
	{
		if(Thread.currentThread() == this.queueWorker)
		{
			Snapshot snaphot = (Snapshot)this.eventQueue.createChainView(chainName).createImmutableSnapshotPoll();
			snapshotsByWorkerThread.add(snaphot);
			return snaphot;
		}
		return (Snapshot)this.eventQueue.createChainView(chainName).createImmutableSnapshotPoll();
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
	
	protected boolean removeEvent(QueuedEventImpl event)
	{
		if(event == null)
		{
			return false;
		}
		
		Node<QueuedEventImpl> node = event.getNode();
		if(node != null)
		{
			node.unlinkFromAllChains();
		}
		event.setNode(null);
		
		if(this.registrationTypes.onRemoveEvents)
		{
			this.removedEventQueue.defaultLinker().append(event);
			this.removedEventListUpdate = true;
			this.notifyOrCreateWorker(-1);
		}
	
		return true;
	}

	@Override
	public boolean removeEvent(String uuid)
	{
		if(uuid == null)
		{
			return false;
		}
		
		QueuedEventImpl removed = null;
		Snapshot<QueuedEventImpl> snapshot = this.chainEventQueue.createImmutableSnapshot();
		
		try
		{
			for(QueuedEventImpl event : snapshot)
			{
				if(uuid.equals(event.getUUID()))
				{
					Node<QueuedEventImpl> node = event.getNode();
					if(node != null)
					{
						node.unlinkFromAllChains();
					}
					event.setNode(null);
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
		
		if(this.registrationTypes.onRemoveEvents)
		{
			this.removedEventQueue.defaultLinker().append(removed);
			this.removedEventListUpdate = true;
			this.notifyOrCreateWorker(-1);
		}
		
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
		Snapshot<QueuedEventImpl> snapshot = this.chainEventQueue.createImmutableSnapshot();
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
						Node<QueuedEventImpl> node = event.getNode();
						if(node != null)
						{
							node.unlinkFromAllChains();
						}
						event.setNode(null);
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
		
		if(this.registrationTypes.onRemoveEvents)
		{
			this.removedEventQueue.defaultLinker().appendAll(removeEventList);
			this.removedEventListUpdate = true;
			this.notifyOrCreateWorker(-1);
		}
		
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
			AtomicBoolean stopTask = new AtomicBoolean(false);
			try
			{
				timeOut = worker.checkTimeOut(stopTask);
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
					if(controllerContainer.isImplementingIOnQueueDetach())
					{
						this.eventDispatcher.executeOnQueueDetach(((IOnQueueDetach)controllerContainer.getQueueController()), this);
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
		
		this.sharedEventLock = null;
		
		try
		{
			taskListReadLock.lock();
			try
			{
				for(Entry<String,TaskContainer> taskContainerEntry : this.taskIndex.entrySet())
				{
					try
					{
						taskContainerEntry.getValue().getTaskControl().setDone();
						((MetricImpl)taskContainerEntry.getValue().getMetrics()).dispose();
					}
					catch (Exception e) 
					{
						log(LogService.LOG_ERROR, "set queue task / service done", e);
					}
				}
			}
			finally 
			{
				taskListReadLock.unlock();
			}
		}
		catch (Exception e) {}
		
		try
		{
			this.disabledRules.clear();
			this.disabledRules = null;
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
			chainEventQueue.dispose();
		}
		catch (Exception e) { log(LogService.LOG_ERROR, "dispose event queue", e);}
		
		try
		{
			eventQueue.dispose();
		}
		catch (Exception e) { log(LogService.LOG_ERROR, "dispose event queue", e);}
		
		try
		{
			chainNewEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose new event queue", e);}
		
		try
		{
			newEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose new event queue", e);}
		
		try
		{
			chainRemovedEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose removed event queue", e);}
		
		try
		{
			removedEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose removed event queue", e);}
		
		try
		{
			chainFireEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose fire vent queue", e);}
		
		try
		{
			fireEventQueue.dispose();
		}
		catch (Exception e) {log(LogService.LOG_ERROR, "dispose fire vent queue", e);}
		
		this.chainDispatcher = null;
		this.registrationTypes = null;
	
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
				List<IQueueChildScope> copyList = this.queueScopeListCopy;
				if(!((copyList == null) || copyList.isEmpty()))
				{
					QueueSessionScopeImpl scopeTest;
					for(IQueueChildScope copyItem : copyList)
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
			this.queueScopeListCopy = Collections.unmodifiableList(new ArrayList<IQueueChildScope>(this.queueScopeList));
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

	public Snapshot<String> getSignalsSnapshot()
	{
		if(! signalListUpdate)
		{
			return null;
		}
		
		this.signalListUpdate = false;
		return this.chainSignalQueue.createImmutableSnapshotPoll();
	}
	
	public Snapshot<? extends IQueuedEvent> getNewScheduledEventsSnaphot()
	{
		if(! newScheduledListUpdate)
		{
			return null;
		}
		
		this.newScheduledListUpdate = false;
		return this.chainNewEventQueue.createImmutableSnapshotPoll();
	}

	public Snapshot<? extends IQueuedEvent> getRemovedEventsSnapshot()
	{
		if(! removedEventListUpdate)
		{
			return null;
		}
		
		this.removedEventListUpdate = false;
		return this.chainRemovedEventQueue.createImmutableSnapshotPoll();
	}
	
	public Snapshot<Event> getFiredEventsSnapshot()
	{
		if(! firedEventListUpdate)
		{
			return null;
		}
		
		this.firedEventListUpdate = false;
		return this.chainFireEventQueue.createImmutableSnapshotPoll();
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
			
			if(this.registrationTypes.onFireEvents)
			{
				this.fireEventQueue.defaultLinker().append(event);
				this.firedEventListUpdate = true;
				this.notifyOrCreateWorker(-1);
			}
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
		
		if(this.registrationTypes.onFireEvents)
		{
			this.fireEventQueue.defaultLinker().append(event);
			this.firedEventListUpdate = true;
			this.notifyOrCreateWorker(-1);
		}
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
			
			if(this.chainNewEventQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainRemovedEventQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainFireEventQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainSignalQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainOnQueueAttachQueue.getSize() > 0)
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
			if(this.chainNewEventQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainRemovedEventQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainFireEventQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainSignalQueue.getSize() > 0)
			{
				return false;
			}
			if(this.chainOnQueueAttachQueue.getSize() > 0)
			{
				return false;
			}
			
			taskListReadLock.lock();
			try
			{
				if(! taskList.isEmpty())
				{
					return false;
				}
			}
			finally 
			{
				taskListReadLock.unlock();
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
	
	public TaskContainer getCurrentRunningTask()
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
		
		return worker.getCurrentRunningTask();
	}


	@Override
	public IEventDispatcher getDispatcher()
	{
		return this.eventDispatcher;
	}

	@Override
	public void signal(String signal)
	{
		this.chainSignalQueue.defaultLinker().append(signal);
		this.signalListUpdate = true;
		
		try
		{
			getMetrics().meter(IMetrics.METRICS_SIGNAL).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric signal", e);
		}
		
		if(this.registrationTypes.onSignal)
		{
			this.notifyOrCreateWorker(-1);
		}
	}
	
	public Snapshot<IOnQueueAttach> getOnQueueAttachList()
	{
		if(! onQueueAttachListUpdate)
		{
			return null;
		}
		
		this.onQueueAttachListUpdate = false;
		return this.chainOnQueueAttachQueue.createImmutableSnapshotPoll();
	}
	
	public void addOnQueueAttach(IOnQueueAttach onQueueAttach)
	{
		this.onQueueAttachListUpdate = true;
		this.chainOnQueueAttachQueue.defaultLinker().append(onQueueAttach);
		
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
	public IQueueChildScope createChildScope(UUID scopeId, String scopeName, IQueueChildScope parentScope, Map<String, Object> configurationProperties, Map<String, Object> stateProperties, boolean adoptContoller, boolean adoptServices)
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
			this.queueScopeListCopy = Collections.unmodifiableList(new ArrayList<IQueueChildScope>(this.queueScopeList));
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
	public List<IQueueChildScope> getChildScopes()
	{
		return this.queueScopeListCopy;
	}


	@Override
	public List<IQueueChildScope> getChildScopes(Filter filter)
	{
		List<IQueueChildScope> copyList = this.queueScopeListCopy;
		if(copyList.isEmpty())
		{
			return copyList;
		}
		if(filter == null)
		{
			return copyList;
		}
		List<IQueueChildScope> filterList = new ArrayList<IQueueChildScope>();
		for(IQueueChildScope scope : copyList)
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
	
	protected List<IQueueChildScope> getChildSessionScopes(UUID parentScopeId)
	{
		List<IQueueChildScope> copyList = this.queueScopeListCopy;
		if(copyList.isEmpty())
		{
			return copyList;
		}
		List<IQueueChildScope> filterList = new ArrayList<IQueueChildScope>();
		for(IQueueChildScope scope : copyList)
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
	public IQueueChildScope getChildScope(UUID scopeId)
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
	public IQueue getGlobalScope()
	{
		return this;
	}

	protected ReentrantLock getSharedEventLock()
	{
		return sharedEventLock;
	}

	@Override
	public void enableRule(String ruleId)
	{
		if(this.disabledRules.contains(ruleId))
		{
			genericQueueSpoolLock.lock();
			try
			{
				Set<String> newDisabledRules = new HashSet<String>(this.disabledRules);
				newDisabledRules.remove(ruleId);
				this.disabledRules = newDisabledRules;
			}
			finally 
			{
				genericQueueSpoolLock.unlock();
			}
			this.chainDispatcher.reset();
		}
	}

	@Override
	public void disableRule(String ruleId)
	{
		if(!this.disabledRules.contains(ruleId))
		{
			genericQueueSpoolLock.lock();
			try
			{
				Set<String> newDisabledRules = new HashSet<String>(this.disabledRules);
				newDisabledRules.add(ruleId);
				this.disabledRules = newDisabledRules;
			}
			finally 
			{
				genericQueueSpoolLock.unlock();
			}
			this.chainDispatcher.reset();
		}
	}
	
	public boolean ruleIsDisabled(String ruleId)
	{
		return this.disabledRules.contains(ruleId);
	}
	
	public void recalcRegistrationTypes()
	{
		RegistrationTypes newRegistrationTypes = new RegistrationTypes();
		
		for(ControllerContainer controllerContainer : getConfigurationList())
		{
			if(controllerContainer.isImplementingIOnScheduleEvent())
			{
				newRegistrationTypes.onQueuedEvent = true;
			}
			if(controllerContainer.isImplementingIOnScheduleEventList())
			{
				newRegistrationTypes.onQueuedEvent = true;
			}
			if(controllerContainer.isImplementingIOnRemoveEvent())
			{
				newRegistrationTypes.onRemoveEvents = true;
			}
			if(controllerContainer.isImplementingIOnFireEvent())
			{
				newRegistrationTypes.onFireEvents = true;
			}
			if(controllerContainer.isImplementingIOnQueueSignal())
			{
				newRegistrationTypes.onSignal = true;
			}
		}
		
		this.registrationTypes = newRegistrationTypes;
	}
	
	public class RegistrationTypes
	{
		boolean onQueuedEvent = false;
		boolean onRemoveEvents = false;
		boolean onFireEvents = false;
		boolean onSignal = false;
	}
}
