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
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Filter;
import org.osgi.service.event.Event;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
public class QueueImpl implements IQueue
{
	public QueueImpl(String queueId,EventDispatcherImpl eventDispatcher)
	{
		super();
	
		this.queueId = queueId;
		this.eventDispatcher = eventDispatcher;
		
		this.genericQueueSpoolLock = new ReentrantLock();
		
		this.configurationList = new ArrayList<ControllerContainer>();
		this.configurationListLock = new ReentrantReadWriteLock(true);
		this.configurationListReadLock = this.configurationListLock.readLock();
		this.configurationListWriteLock = this.configurationListLock.writeLock();
		
		this.eventList = new ArrayList<QueuedEventImpl>();
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
		
		this.newScheduledList = new ArrayList<QueuedEventImpl>();
		this.removedEventList = new ArrayList<QueuedEventImpl>();
		this.firedEventList = new ArrayList<Event>();
		
		this.metrics = new MetricImpl(this);
		this.propertyBlock = new PropertyBlockImpl();
	}
	
	private MetricImpl metrics;
	private EventDispatcherImpl eventDispatcher = null;
	private String queueId = null;
	
	private List<ControllerContainer> configurationList;
	private volatile List<ControllerContainer> configurationListCopy = null;
	private ReentrantReadWriteLock configurationListLock;
	private ReadLock configurationListReadLock;
	private WriteLock configurationListWriteLock;
	
	private List<QueuedEventImpl> eventList = null;
	private ReentrantReadWriteLock eventListLock;
	private ReadLock eventListReadLock;
	private WriteLock eventListWriteLock;
	
	private List<JobContainer> jobList = null;
	private Map<String,JobContainer> jobIndex = null;
	private ReentrantReadWriteLock jobListLock;
	private ReadLock jobListReadLock;
	private WriteLock jobListWriteLock;
	
	private volatile boolean signalListUpdate = false;
	private List<String> signalList = null;
	private ReentrantLock signalListLock = null;
	
	private volatile QueueWorker queueWorker = null;
	private PropertyBlockImpl propertyBlock = null;
	
	private volatile boolean newScheduledListUpdate = false;
	private List<QueuedEventImpl> newScheduledList = null;
	private volatile boolean removedEventListUpdate = false;
	private List<QueuedEventImpl> removedEventList = null;
	private volatile boolean firedEventListUpdate = false;
	private List<Event> firedEventList = null;
	
	private ReentrantLock genericQueueSpoolLock = null;
	
	public void scheduleEvent(Event event)
	{
		QueuedEventImpl queuedEvent = null;
		eventListWriteLock.lock();
		try
		{
			queuedEvent = new QueuedEventImpl(event,this);
			this.eventList.add(queuedEvent);
		}
		finally 
		{
			eventListWriteLock.unlock();
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			newScheduledListUpdate = true;
			this.newScheduledList.add(queuedEvent);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		
		this.notifyOrCreateWorker(-1);
	}
	
	
	public void addConfiguration(IEventController eventQueueConfiguration,Map<String, ?> properties)
	{
		configurationListWriteLock.lock();
		try
		{
			for(ControllerContainer configurationContainer : this.configurationList)
			{
				if(configurationContainer.getEventController() == eventQueueConfiguration)
				{
					configurationContainer.setProperties(properties);
					return;
				}
			}
			
			ControllerContainer configurationContainer = new ControllerContainer();
			configurationContainer.setProperties(properties);
			configurationContainer.setEventController(eventQueueConfiguration);
			
			this.configurationList.add(configurationContainer);
			this.configurationListCopy = null;
		}
		finally 
		{
			configurationListWriteLock.unlock();
		}
	}
	
	public boolean removeConfiguration(IEventController eventQueueConfiguration)
	{
		configurationListWriteLock.lock();
		try
		{
			List<ControllerContainer> toDeleteList = new ArrayList<ControllerContainer>();
			for(ControllerContainer configurationContainer : this.configurationList)
			{
				if(configurationContainer.getEventController() == eventQueueConfiguration)
				{
					toDeleteList.add(configurationContainer);
				}
			}
			for(ControllerContainer toDelete : toDeleteList)
			{
				this.configurationList.remove(toDelete);
			}
			this.configurationListCopy = null;
			return toDeleteList.size() > 0;
		}
		finally 
		{
			configurationListWriteLock.unlock();
		}
	}
	
	public int getConfigurationSize()
	{
		configurationListReadLock.lock();
		try
		{
			return this.configurationList.size();
		}
		finally 
		{
			configurationListReadLock.unlock();
		}
	}
	
	@Override
	public IMetrics getMetrics()
	{
		return this.metrics;
	}
	
	@Override
	public IPropertyBlock getPropertyBlock()
	{
		return this.propertyBlock;
	}
	

	public String getQueueId()
	{
		return queueId;
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
				if(jobContainer.getJobControl().getExecutionTimeStamp() > timeStamp)
				{
					if(nextRun > jobContainer.getJobControl().getExecutionTimeStamp())
					{
						nextRun = jobContainer.getJobControl().getExecutionTimeStamp();
					}
					continue;
				}
				dueJobList.add(jobContainer);
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
		return scheduleJob(null,job, null, -1, -1, -1);
	}
	
	@Override
	public String scheduleJob(String id, IQueueJob job, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut )
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
				toRemove = null;
			}
			
			if((id == null) || (id.isEmpty()))
			{
				id = UUID.randomUUID().toString();
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
						jobContainer = null;
					}
					else
					{
						return id;
					}
				}
			}
			
			jobContainer = new JobContainer();
			MetricImpl metric = new MetricImpl(this);
			
			if(propertyBlock == null)
			{
				propertyBlock = new PropertyBlockImpl();
			}
			
			JobControlImpl jobControl = new JobControlImpl(propertyBlock);
			if(executionTimeStamp > 0)
			{
				jobControl.setExecutionTimeStamp(executionTimeStamp);
			}
			if(heartBeatTimeOut > 0)
			{
				jobControl.setHeartBeatTimeOut(heartBeatTimeOut);
			}
			if(timeOutValue > 0)
			{
				jobControl.setTimeOut(timeOutValue);
			}
			
			propertyBlock.setProperty(IQueueJob.PROPERTY_KEY_JOB_ID, id);
			
			jobContainer.setId(id);
			jobContainer.setJob(job);
			jobContainer.setMetrics(metric);
			jobContainer.setPropertyBlock(propertyBlock);
			jobContainer.setJobControl(jobControl);
			jobList.add(jobContainer);
			jobIndex.put(id, jobContainer);
		}
		finally 
		{
			jobListWriteLock.unlock();
		}
		
		jobContainer.getJob().configure(id, jobContainer.getMetrics(), jobContainer.getPropertyBlock(), jobContainer.getJobControl());
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
				return null;
			}
			
			IJobControl jobControl = jobContainer.getJobControl();
			
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
				jobControl.setExecutionTimeStamp(executionTimeStamp);
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
		
		eventListReadLock.lock();
		try
		{
			for(IQueuedEvent queuedEvent : this.eventList)
			{
				if(uuid.equals(queuedEvent.getUUID()))
				{
					return queuedEvent;
				}
			}
		}
		finally 
		{
			eventListReadLock.unlock();
		}
		
		return null;
	}

	@Override
	public List<IQueuedEvent> getEventList(String[] topics, Filter eventFilter, Filter nativeEventFilter)
	{
		boolean match = true;
		List<IQueuedEvent> queryList = new ArrayList<>();
		eventListReadLock.lock();
		try
		{
			for(IQueuedEvent queuedEvent : this.eventList)
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
		}
		finally 
		{
			eventListReadLock.unlock();
		}
		return queryList;
	}

	@Override
	public boolean removeEvent(String uuid)
	{
		if(uuid == null)
		{
			return false;
		}
	
		eventListWriteLock.lock();
		
		List<QueuedEventImpl> removeList = null;
		try
		{
			for(QueuedEventImpl event : this.eventList)
			{
				if(uuid.equals(event.getUUID()))
				{
					if(removeList == null)
					{
						removeList = new ArrayList<QueuedEventImpl>();
					}
					removeList.add(event);
				}
			}
			if(removeList == null)
			{
				return false;
			}
			
			if(removeList.isEmpty())
			{
				return false;
			}
			
			for(IQueuedEvent event : removeList)
			{
				while(this.eventList.remove(event)){}
			}
		}
		finally 
		{
			eventListWriteLock.unlock();
		}
		if((removeList != null) && (! removeList.isEmpty()))
		{
			this.genericQueueSpoolLock.lock();
			try
			{
				removedEventListUpdate = true;
				for(QueuedEventImpl event : removeList)
				{
					this.removedEventList.add(event);
				}
			}
			finally 
			{
				this.genericQueueSpoolLock.unlock();
			}
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
	
		List<QueuedEventImpl> removeList = null;
		eventListWriteLock.lock();
		try
		{
			for(QueuedEventImpl event : this.eventList)
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
						if(removeList == null)
						{
							removeList = new ArrayList<QueuedEventImpl>();
						}
						removeList.add(event);
					}
				}
			}
			if(removeList == null)
			{
				return false;
			}
			
			for(QueuedEventImpl event : removeList)
			{
				while(this.eventList.remove(event)){}
			}
		}
		finally 
		{
			eventListWriteLock.unlock();
		}
		
		if((removeList != null) && (! removeList.isEmpty()))
		{
			this.genericQueueSpoolLock.lock();
			try
			{
				removedEventListUpdate = true;
				for(QueuedEventImpl event : removeList)
				{
					this.removedEventList.add(event);
				}
			}
			finally 
			{
				this.genericQueueSpoolLock.unlock();
			}
			this.notifyOrCreateWorker(-1);
		}
		
		return true;
	}

	public List<ControllerContainer> getConfigurationList()
	{
		List<ControllerContainer> list = configurationListCopy;
		if(list != null)
		{
			return list; 
		}
		configurationListReadLock.lock();
		try
		{
			list = new ArrayList<ControllerContainer>();
			for(ControllerContainer configuration : configurationList)
			{
				list.add(configuration);
			}
			list = Collections.unmodifiableList(list);
			configurationListCopy = list;
		}
		finally 
		{
			configurationListReadLock.unlock();
		}
		 
		return list;
	}
	
	public boolean checkTimeOut()
	{
		QueueWorker worker = null;
		this.genericQueueSpoolLock.lock();
		try
		{
			worker = this.queueWorker;
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		
		boolean timeOut = false;
		if(worker != null)
		{
			timeOut = worker.checkTimeOut();
			if(timeOut)
			{
				this.genericQueueSpoolLock.lock();
				try
				{
					if(worker == this.queueWorker)
					{
						this.queueWorker = null;
					}
				}
				finally 
				{
					this.genericQueueSpoolLock.unlock();
				}
			}
		}
		return timeOut;
	}
	
	public void stopQueueMonitor()
	{
		this.genericQueueSpoolLock.lock();
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
			this.genericQueueSpoolLock.unlock();
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
	
	public void fetchNewScheduledList(List<QueuedEventImpl> fillList)
	{
		if(! newScheduledListUpdate)
		{
			return;
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			newScheduledListUpdate = false;
			for(QueuedEventImpl event : this.newScheduledList)
			{
				fillList.add(event);
			}
			this.newScheduledList.clear();
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}

	public void fetchRemovedEventList(List<QueuedEventImpl> fillList)
	{
		if(! removedEventListUpdate)
		{
			return;
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			removedEventListUpdate = false;
			for(QueuedEventImpl event : this.removedEventList)
			{
				fillList.add(event);
			}
			this.removedEventList.clear();
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}

	public void fetchFiredEventList(List<Event> fillList)
	{
		if(! firedEventListUpdate)
		{
			return;
		}
		this.genericQueueSpoolLock.lock();
		try
		{
			firedEventListUpdate = false;
			for(Event event : this.firedEventList)
			{
				fillList.add(event);
			}
			this.firedEventList.clear();
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}


	@Override
	public void sendEvent(String topic, Map<String, ?> properties)
	{
		Event event = new Event(topic,properties);
		this.eventDispatcher.eventAdmin.sendEvent(event);
		this.genericQueueSpoolLock.lock();
		try
		{
			firedEventListUpdate = true;
			this.firedEventList.add(event);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		this.notifyOrCreateWorker(-1);
	}


	@Override
	public void postEvent(String topic, Map<String, ?> properties)
	{
		Event event = new Event(topic,properties);
		this.eventDispatcher.eventAdmin.postEvent(event);
		this.genericQueueSpoolLock.lock();
		try
		{
			firedEventListUpdate = true;
			this.firedEventList.add(event);
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
		this.notifyOrCreateWorker(-1);
	}
	
	private void notifyOrCreateWorker(long nextRuntimeStamp)
	{
		this.genericQueueSpoolLock.lock();
		try
		{
			if(this.queueWorker == null)
			{
				this.queueWorker = new QueueWorker(this);
				this.queueWorker.start();
			}
			else
			{
				if(nextRuntimeStamp < 1)
				{
					this.queueWorker.notifyUpdate();
				}
				else
				{
					this.queueWorker.notifyUpdate(nextRuntimeStamp);
				}
			}
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}
	
	protected void checkWorkerShutdown(QueueWorker worker)
	{
		if(worker == null)
		{
			return;
		}
		
		this.genericQueueSpoolLock.lock();
		try
		{
			if(worker != this.queueWorker)
			{
				worker.stopWorker();
				return;
			}
			
			if(! this.newScheduledList.isEmpty())
			{
				return;
			}
			if(! this.removedEventList.isEmpty())
			{
				return;
			}
			if(! this.firedEventList.isEmpty())
			{
				return;
			}
			
			jobListReadLock.lock();
			try
			{
				if(! jobList.isEmpty())
				{
					return;
				}
			}
			finally 
			{
				jobListReadLock.unlock();
			}
			
			this.queueWorker.stopWorker();
			this.queueWorker = null;
		}
		finally 
		{
			this.genericQueueSpoolLock.unlock();
		}
	}
	
	public JobContainer getCurrentRunningJob()
	{
		QueueWorker worker = this.queueWorker;
		if(worker == null)
		{
			this.genericQueueSpoolLock.lock();
			try
			{
				worker = this.queueWorker;
				if(worker == null)
				{
					this.queueWorker = new QueueWorker(this);
					this.queueWorker.start();
					worker = this.queueWorker;
				}
			}
			finally 
			{
				this.genericQueueSpoolLock.unlock();
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
		this.notifyOrCreateWorker(-1);
		
	}
}
