/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import org.osgi.framework.Filter;
import org.osgi.service.event.Event;

/**
 * API for event-queues. {@link IQueue}s are configured by one or more {@link IEventController}s. 
 * All collected osgi-{@link org.osgi.service.event.Event}s are wrapped by {@link IQueuedEvent}. 
 * {@link IQueuedEvent}s can be processed by {@link IQueueJob}s.
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueue
{
	/**
	 * getter for queue id
	 * 
	 * @return id of queue
	 */
	public String getQueueId();
	
	
	/**
	 * schedule an osgi event to queue
	 * 
	 * @param event osgi-event to schedule 
	 * 
	 * @return Future of {@link IScheduleResult}
	 * @throws QueueIsFullException
	 */
	public Future<IScheduleResult> scheduleEvent(Event event) throws QueueIsFullException;
	
	/**
	 * schedule a list of osgi events to queue
	 * 
	 * @param eventList list of osgi-events to schedule 
	 * 
	 * @return Future of {@link IScheduleResult}
	 * @throws QueueIsFullException
	 */
	public Future<IScheduleResult> scheduleEventList(List<Event> eventList) throws QueueIsFullException;
	
	/**
	 * getter for configuration propertyblock of queue
	 * 
	 * @return {@link IPropertyBlock} of queue for configuration details
	 */
	public IPropertyBlock getConfigurationPropertyBlock();

	/**
	 * getter for state propertyblock of queue
	 * 
	 * @return {@link IPropertyBlock} of queue  for work state
	 */
	public IPropertyBlock getStatePropertyBlock();
	
	/**
	 * getter of metric-handler of queue
	 * @return {@link IMetrics} of queue
	 */
	public IMetrics getMetrics();
	
	/**
	 * getter for global dispatchter service
	 * 
	 * @return {@link IEventDispatcher}
	 */
	public IEventDispatcher getDispatcher();
	
	/**
	 * returns {@link IQueuedEvent} queued  with {@code uuid} 
	 * 
	 * @param uuid searchfilter 
	 * 
	 * @return IQueuedEvent queued with {@code uuid} or null if not present
	 */
	public IQueuedEvent getEvent(String uuid);
	
	/**
	 * returns list of {@link IQueuedEvent}s matched by filter parameter
	 * 
	 * @param topics topic-filter or null for irrelevant
	 * @param queuedEventFilter osgi-filter for {@link IQueuedEvent}-properties
	 * @param nativeEventFilter osgi-filter for {@link org.osgi.service.event.Event}-properties
	 * 
	 * @return list of {@link IQueuedEvent}s matched by filter parameter
	 */
	public List<IQueuedEvent> getEventList(String[] topics, Filter queuedEventFilter, Filter nativeEventFilter);
	
	/**
	 * remove {@link IQueuedEvent} queued  with {@code uuid} 
	 * 
	 * @param uuid identifier for {@link IQueuedEvent} to remove
	 * 
	 * @return true if {@link IQueuedEvent} was found and remove, otherwise false
	 */
	public boolean removeEvent(String uuid);
	
	/**
	 * remove list of {@link IQueuedEvent}s queued  with one of {@code uuid}s
	 * 
	 * @param uuidList list of identifiers for {@link IQueuedEvent} to remove
	 * @return  true if one of {@link IQueuedEvent} was found and remove, otherwise false
	 */
	public boolean removeEventList(List<String> uuidList);
	
	/**
	 * returns list of scheduled {@link IQueueJob} matched by filter parameter
	 * 
	 * @param filter osgi-filter for {@link IQueueJob}-properties
	 * @return list of {@link IQueueJob}s matched by filter parameter
	 */
	public List<IQueueJob> getJobList(Filter filter);
	
	/**
	 * returns map with jobid-job-pairs matched by filter parameter
	 * 
	 * @param filter osgi-filter for {@link IQueueJob}-properties
	 * @return map with jobid-job-pairs matched by filter parameter
	 */
	public Map<String,IQueueJob> getJobIndex(Filter filter);
	
	/**
	 * schedule a anonymous {@link IQueueJob} to {@link IQueue}
	 * 
	 * equivalent to scheduleJob(null,job, null, -1, -1, -1);
	 * 
	 * @param job {@link IQueueJob} to schedule
	 * 
	 * @return generated jobid
	 */
	public String scheduleJob(IQueueJob job);
	
	/**
	 * schedule a {@link IQueueJob} to {@link IQueue}.
	 * 
	 * @param id registration-id for {@link IQueueJob} to schedule
	 * @param job {@link IQueueJob} to schedule
	 * 
	 * @return jobid (generated, if parameter id is null)
	 */
	public String scheduleJob(String id,IQueueJob job);
	
	/**
	 * schedule a {@link IQueueJob} to {@link IQueue}.
	 * 
	 * @param id registration-id for {@link IQueueJob} to schedule
	 * @param job {@link IQueueJob} to schedule
	 * @param propertyBlock {@link IQueueJob}-properties (factory in {@link IEventDispatcher})
	 * @param executionTimeStamp execution time millis
	 * @param timeOutValue timeout value in ms, before notify for timeout
	 * @param heartBeatTimeOut heartbeat-timeout value in ms, before notify for timeout
	 * 
	 * @return jobid (generated, in parameter id is null)
	 */
	public String scheduleJob(String id, IQueueJob job, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut );
	
	/**
	 * schedule a {@link IQueueJob} to {@link IQueue}.
	 * 
	 * @param id registration-id for {@link IQueueJob} to schedule
	 * @param job {@link IQueueJob} to schedule
	 * @param propertyBlock {@link IQueueJob}-properties (factory in {@link IEventDispatcher})
	 * @param executionTimeStamp execution time millis
	 * @param timeOutValue timeout value in ms, before notify for timeout
	 * @param heartBeatTimeOut heartbeat-timeout value in ms, before notify for timeout
	 * @param stopOnTimeOut stop unlinked worker-thread on timeout. This option is NOT necessary to create new worker running other jobs. Attention: can be dangerous  
	 * 
	 * @return jobid (generated, in parameter id is null)
	 */
	public String scheduleJob(String id, IQueueJob job, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut, boolean stopOnTimeOut );
	
	/**
	 * reset execution plan for an existing {@link IQueueJob}
	 * 
	 * @param id registration-id of {@link IQueueJob} in which reset execution plan 
	 * @param executionTimeStamp new execution time millis
	 * @param timeOutValue new timeout value in ms, before notify for timeout
	 * @param heartBeatTimeOut heartbeat-timeout value in ms, before notify for timeout
	 * @return affected {@link IQueueJob} or null if not found
	 */
	public IQueueJob rescheduleJob(String id, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut );
	
	/**
	 * returns {@link IQueueJob} scheduled under registration {@code id}
	 * 
	 * @param id registration-id for {@link IQueueJob}
	 * @return {@link IQueueJob} scheduled under registration {@code id}
	 */
	public IQueueJob getJob(String id);
	
	/**
	 * remove{@link IQueueJob} scheduled under registration {@code id}
	 * 
	 * @param id registration-id for {@link IQueueJob} to remove
	 * @return removed {@link IQueueJob} or null if no scheduled with {@code id} found
	 */
	public IQueueJob removeJob(String id);
	
	/**
	 * returns properties of {@link IQueueJob} scheduled under registration {@code id}
	 * 
	 * @param id registration-id for {@link IQueueJob}
	 * @return properties of {@link IQueueJob} scheduled under registration {@code id}
	 */
	public IPropertyBlock getJobPropertyBlock(String id);
	
	/**
	 * Sends a signal. All {@link IEventController} manage this {@link IQueue} and implements {@link IOnQueueSignal} will notify asynchronously by queueworker.
	 * 
	 * @param signal
	 */
	public void signal(String signal);
	
	/**
	 * Sends an osgi-{@link org.osgi.service.event.Event} synchronously. 
	 * Following this all {@link IEventController} manage this {@link IQueue} and implements {@link IOnFireEvent} will notify asynchronously by queueworker.
	 * 
	 * @param topic event topic
	 * @param properties event properties
	 */
	public void sendEvent(String topic, Map<String, ?> properties);
	
	/**
	 * Post an osgi-{@link org.osgi.service.event.Event} asynchronously. 
	 * Following this all {@link IEventController} manage this {@link IQueue} and implements {@link IOnFireEvent} will notify asynchronously by queueworker.
	 * 
	 * @param topic event topic
	 * @param properties event properties
	 */
	public void postEvent(String topic, Map<String, ?> properties);
	
	/**
	 * setter to enable or disable metrics capabilities
	 * 
	 * @param enabled enable metrics capabilities if true, otherwise disable metrics capabilities
	 */
	public void  setMetricsEnabled(boolean enabled);
	
	/**
	 * 
	 * @return true if metrics capabilities is enabled , otherwise false
	 */
	public boolean isMetricsEnabled();
	
	/**
	 * create {@link IQueueSessionScope} for {@link IQueue}. Does not work, if this queue is already a session scope.
	 * 
	 * @param scopeId unique id of scope (unique by queue) or null for auto-generation
	 * @param scopeName human readable name of scope (nullable)
	 * @param configurationProperties blue print for configuration propertyblock of new scope (nullable)
	 * @param stateProperties blue print for state propertyblock of new scope (nullable)
	 * @param adoptContoller keep controller of parent queue
	 * @param adoptServices keep services of parent queue
	 * 
	 * @return new scope, or null, if scope already exists
	 */
	public IQueueSessionScope createSessionScope(UUID scopeId,String scopeName, Map<String,Object> configurationProperties, Map<String,Object> stateProperties, boolean adoptContoller, boolean adoptServices);
	
	/**
	 * returns scopelist of queue
	 * 
	 * @return scopelist of queue
	 */
	public List<IQueueSessionScope> getSessionScopes();
	
	/**
	 * returns scopelist of queue with positiv match result for {@code filter}
	 * 
	 * @param filter match condition for configuration propertyblock
	 * 
	 * @return scopelist of queue with positiv match result for {@code filter}
	 */
	public List<IQueueSessionScope> getSessionScopes(Filter filter);
	
	
	/**
	 * returns scope with given {@code scopeId}
	 * 
	 * @param scopeId id of scope to return
	 * 
	 * @return  scope with given {@code scopeId} or null, if scope not found
	 */
	public IQueueSessionScope getSessionScope(UUID scopeId);
}
