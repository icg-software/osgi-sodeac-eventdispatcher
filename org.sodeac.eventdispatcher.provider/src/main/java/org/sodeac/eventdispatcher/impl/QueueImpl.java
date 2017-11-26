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
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IGauge;
import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.ITimer;

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
		
		this.serviceList = new ArrayList<ServiceContainer>();
		this.serviceListLock = new ReentrantReadWriteLock(true);
		this.serviceListReadLock = this.serviceListLock.readLock();
		this.serviceListWriteLock = this.serviceListLock.writeLock();
		
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
		
		PropertyBlockImpl qualityValues = new PropertyBlockImpl();
		qualityValues.setProperty(IMetrics.QUALITY_VALUE_CREATED, System.currentTimeMillis()); // TODO test
		this.metrics = new MetricImpl(this,qualityValues, null);
		this.propertyBlock = new PropertyBlockImpl();
		
		this.metrics.registerGauge(new IGauge<Long>()											// TODO test
		{
			@Override
			public Long getValue()
			{
				return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_LAST_SEND_EVENT);
			}
		}, IMetrics.GAUGE_LAST_SEND_EVENT);
		
		this.metrics.registerGauge(new IGauge<Long>()											// TODO test
		{

			@Override
			public Long getValue()
			{
				return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_LAST_POST_EVENT);
			}
		}, IMetrics.GAUGE_LAST_POST_EVENT);
	}
	
	private MetricImpl metrics;
	private EventDispatcherImpl eventDispatcher = null;
	private String queueId = null;
	
	private List<ControllerContainer> configurationList;
	private volatile List<ControllerContainer> configurationListCopy = null;
	private ReentrantReadWriteLock configurationListLock;
	private ReadLock configurationListReadLock;
	private WriteLock configurationListWriteLock;
	
	private List<ServiceContainer> serviceList;
	private volatile List<ServiceContainer> serviceListCopy = null;
	private ReentrantReadWriteLock serviceListLock;
	private ReadLock serviceListReadLock;
	private WriteLock serviceListWriteLock;
	
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
	
	private volatile boolean disposed = false; 
	
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
		
		try
		{
			eventDispatcher.getMetrics().counter(Event.class.getName(), "Scheduled").inc();	// TODO test
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "increment metric counter", e);
		}
		
		try
		{
			eventDispatcher.getMetrics().meter(Event.class.getName(), "Scheduled").mark();	// TODO test
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
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
			
			if(properties.get(IEventController.CONSUME_EVENT_TOPIC) != null)
			{
				if(properties.get(IEventController.CONSUME_EVENT_TOPIC) instanceof String)
				{
					String topic = (String)properties.get(IEventController.CONSUME_EVENT_TOPIC);
					if(! topic.isEmpty())
					{
						ConsumeEventHandler handler = configurationContainer.addConsumeEventHandler(new ConsumeEventHandler(eventDispatcher, queueId, topic));
						Dictionary<String, Object> registerProperties = new Hashtable<String,Object>();
						registerProperties.put(EventConstants.EVENT_TOPIC,topic);
						ServiceRegistration<EventHandler> registration = eventDispatcher.getContext().getBundleContext().registerService(EventHandler.class, handler, registerProperties);
						handler.setRegistration(registration);
					}
				}
				else
				{
					List<String> topicList = new ArrayList<String>();
					if(properties.get(IEventController.CONSUME_EVENT_TOPIC) instanceof List)
					{
						for(Object item : (List<?>)properties.get(IEventController.CONSUME_EVENT_TOPIC))
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
					
					if(properties.get(IEventController.CONSUME_EVENT_TOPIC) instanceof String[])
					{
						for(Object item : (String[])properties.get(IEventController.CONSUME_EVENT_TOPIC))
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
					
					if(! topicList.isEmpty())
					{
						for(String topic : topicList)
						{
							configurationContainer.addConsumeEventHandler(new ConsumeEventHandler(eventDispatcher, queueId, topic));
						}
						
						if(configurationContainer.getConsumeEventHandlerList() != null)
						{
							for(ConsumeEventHandler handler :  configurationContainer.getConsumeEventHandlerList())
							{
								Dictionary<String, Object> registerProperties = new Hashtable<String,Object>();
								registerProperties.put(EventConstants.EVENT_TOPIC,handler.getTopic());
								ServiceRegistration<EventHandler> registration = eventDispatcher.getContext().getBundleContext().registerService(EventHandler.class, handler, registerProperties);
								handler.setRegistration(registration);
							}
						}
					}
				}
			}
			
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
				
				if(toDelete.getConsumeEventHandlerList() != null)
				{
					for(ConsumeEventHandler handler : toDelete.getConsumeEventHandlerList() )
					{
						ServiceRegistration<EventHandler> registration = handler.getRegistration();
						handler.setRegistration(null);
						try
						{
							if(registration != null)
							{
								registration.unregister();
							}
						}
						catch (Exception e) 
						{
							log(LogService.LOG_ERROR,"Unregistration Consuming Event for queue " + queueId , e);
						}
					}
				}
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
	
	public void addService(IQueueService queueService,Map<String, ?> properties)
	{
		boolean reschedule = false;
		serviceListWriteLock.lock();
		try
		{
			for(ServiceContainer serviceContainer : this.serviceList)
			{
				if(serviceContainer.getQueueService() == queueService)
				{
					serviceContainer.setProperties(properties);
					reschedule = true;
					break;
				}
			}
			
			if(! reschedule)
			{
				ServiceContainer serviceContainer = new ServiceContainer();
				serviceContainer.setProperties(properties);
				serviceContainer.setQueueService(queueService);
				
				this.serviceList.add(serviceContainer);
				this.serviceListCopy = null;
			}
		}
		finally 
		{
			serviceListWriteLock.unlock();
		}
		
		this.scheduleService(queueService, properties, reschedule);
		
	}
	
	private void scheduleService(IQueueService queueService,Map<String, ?> properties, boolean reschedule)
	{
		String serviceId = null;
		long delay = 0;
		long timeout = -1;
		long hbtimeout = -1;
		
		
		try
		{
			if(properties.get(IQueueService.PROPERTY_SERVICE_ID) != null)
			{
				serviceId = (String)properties.get(IQueueService.PROPERTY_SERVICE_ID);
			}		
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"problems with serviceid",e);
		}
		
		try
		{
			if(properties.get(IQueueService.PROPERTY_START_DELAY_MS) != null)
			{
				Object delayObject = properties.get(IQueueService.PROPERTY_START_DELAY_MS);
				if(delayObject instanceof String)
				{
					delay = Long.parseLong((String)delayObject);
				}
				else if(delayObject instanceof Integer)
				{
					delay = ((Integer)delayObject);
				}
				else
				{
					delay = ((Long)delayObject);
				}
			}		
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"problems with startdelayvalue",e);
		}
		
		try
		{
			if(properties.get(IQueueService.PROPERTY_TIMEOUT_MS) != null)
			{
				Object timeoutObject = properties.get(IQueueService.PROPERTY_TIMEOUT_MS);
				if(timeoutObject instanceof String)
				{
					timeout = Long.parseLong((String)timeoutObject);
				}
				else if(timeoutObject instanceof Integer)
				{
					timeout = ((Integer)timeoutObject);
				}
				else
				{
					timeout = ((Long)timeoutObject);
				}
			}		
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"problems with timeoutvalue",e);
		}
		
		try
		{
			if(properties.get(IQueueService.PROPERTY_HB_TIMEOUT_MS) != null)
			{
				Object hbtimeoutObject = properties.get(IQueueService.PROPERTY_HB_TIMEOUT_MS);
				if(hbtimeoutObject instanceof String)
				{
					hbtimeout = Long.parseLong((String)hbtimeoutObject);
				}
				else if(hbtimeoutObject instanceof Integer)
				{
					hbtimeout = ((Integer)hbtimeoutObject);
				}
				else
				{
					hbtimeout = ((Long)hbtimeoutObject);
				}
			}		
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"problems with heartbeattimeoutvalue",e);
		}
		
		
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
			this.scheduleJob(serviceId, queueService, servicePropertyBlock, System.currentTimeMillis() + delay, timeout, hbtimeout);
		}
		catch (Exception e) 
		{
			log(LogService.LOG_ERROR,"problems scheduling service with id " + serviceId,e);
		}
	}
	
	public boolean removeService(IQueueService eventQueueService)
	{
		serviceListWriteLock.lock();
		try
		{
			List<ServiceContainer> toDeleteList = new ArrayList<ServiceContainer>();
			for(ServiceContainer serviceContainer : this.serviceList)
			{
				if(serviceContainer.getQueueService() == eventQueueService)
				{
					toDeleteList.add(serviceContainer);
				}
			}
			for(ServiceContainer toDelete : toDeleteList)
			{
				this.serviceList.remove(toDelete);
			}
			this.serviceListCopy = null;
			return toDeleteList.size() > 0;
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
			
			PropertyBlockImpl qualityValues = new PropertyBlockImpl();
			qualityValues.setProperty(IMetrics.QUALITY_VALUE_CREATED, System.currentTimeMillis());
			
			
			MetricImpl metric = new MetricImpl(this,qualityValues, id);
			
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
			
			qualityValues.setProperty(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, -1L);
			
			if(jobContainer.isNamedJob())
			{
				metric.registerGauge(new IGauge<Long>()																		// TODO test
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_CREATED);
					}
				}, IMetrics.GAUGE_JOB_CREATED);
				
				metric.registerGauge(new IGauge<Long>()																		// TODO test
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_FINISHED_TIMESTAMP);
					}
				}, IMetrics.GAUGE_JOB_FINISHED);
				
				metric.registerGauge(new IGauge<Long>()																		// TODO test
				{
	
					@Override
					public Long getValue()
					{
						return (Long)qualityValues.getProperty(IMetrics.QUALITY_VALUE_STARTED_TIMESTAMP);
					}
				}, IMetrics.GAUGE_JOB_STARTED);
				
				metric.registerGauge(new IGauge<Long>()																		// TODO test
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
	
	public void dispose()
	{
		this.disposed = true;
	}
	
	public void stopQueueWorker()
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
		// TODO test metrics
		this.metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_SEND_EVENT, System.currentTimeMillis());
		
		try
		{
			eventDispatcher.getMetrics().counter(Event.class.getName(), IMetrics.METRICS_SEND_EVENT).inc();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "increment metric counter", e);
		}
		
		try
		{
			eventDispatcher.getMetrics().meter(Event.class.getName(), IMetrics.METRICS_SEND_EVENT).mark();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "mark metric counter", e);
		}
		
		ITimer.Context timerContext = null;
		try
		{
			timerContext = eventDispatcher.getMetrics().timer(Event.class.getName(), IMetrics.METRICS_SEND_EVENT).time();
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
				this.firedEventList.add(event);
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
		// TODO test metrics
		this.metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_POST_EVENT, System.currentTimeMillis());
		
		try
		{
			eventDispatcher.getMetrics().counter(Event.class.getName(), IMetrics.METRICS_POST_EVENT).inc();
		}
		catch(Exception e)
		{
			log(LogService.LOG_ERROR, "increment metric counter", e);
		}
		
		try
		{
			eventDispatcher.getMetrics().meter(Event.class.getName(), IMetrics.METRICS_POST_EVENT).mark();
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
				if(this.disposed)
				{
					return;
				}
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
				if((worker == null) && (! this.disposed))
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
	
	protected void log(int logServiceLevel,String logMessage, Exception e)
	{
		try
		{
			LogService logService = null;
			ComponentContext context = null;
			
			if(eventDispatcher != null){logService = eventDispatcher.logService;}
			if(eventDispatcher != null){context = eventDispatcher.getContext();}
			
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
	
	protected void log(int logServiceLevel,String logMessage, Error e)
	{
		
		try
		{
			LogService logService = null;
			ComponentContext context = null;
			
			if(eventDispatcher != null){logService = eventDispatcher.logService;}
			if(eventDispatcher != null){context = eventDispatcher.getContext();}
			
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
}
