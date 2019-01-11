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
package org.sodeac.eventdispatcher.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.osgi.framework.Filter;
import org.osgi.service.event.Event;
import org.sodeac.multichainlist.Snapshot;

/**
 * API for event-queues. {@link IQueue}s are configured by one or more {@link IQueueController}s. 
 * All collected osgi-{@link org.osgi.service.event.Event}s are wrapped by {@link IQueuedEvent}. 
 * {@link IQueuedEvent}s can be processed by {@link IQueueTask}s.
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
	public String getId();
	
	/**
	 * queue an osgi event
	 * 
	 * @param event osgi-event to queue
	 * 
	 * @throws QueueIsFullException
	 */
	public void queueEvent(Event event) throws QueueIsFullException;
	
	/**
	 * queue a collection of osgi events
	 * 
	 * @param eventList list of osgi-events to queue
	 * 
	 * @throws QueueIsFullException
	 */
	public void queueEvents(Collection<Event> events) throws QueueIsFullException;
	
	/**
	 * queue an osgi event
	 * 
	 * @param event osgi-event to queue
	 * 
	 * @return Future of {@link IQueueEventResult}
	 * @throws QueueIsFullException
	 */
	public Future<IQueueEventResult> queueEventWithResult(Event event) throws QueueIsFullException;
	
	/**
	 * queue a list of osgi events
	 * 
	 * @param eventList list of osgi-events to queue
	 * 
	 * @return Future of {@link IQueueEventResult}
	 * @throws QueueIsFullException
	 */
	public Future<IQueueEventResult> queueEventsWithResult(Collection<Event> events) throws QueueIsFullException;
	
	// TODO queueEvents without result
	
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
	 * return event chain-snapshot  
	 * 
	 * @param chainName define chain
	 * 
	 * @return snapshot for chain
	 */
	public Snapshot<IQueuedEvent> getEventSnapshot(String chainName);
	
	/**
	 * return event chain-snapshot-poll (clears returned elements)
	 * 
	 * @param chainName define chain
	 * 
	 * @return snapshot for chain
	 */
	public Snapshot<IQueuedEvent> getEventSnapshotPoll(String chainName);
	
	/**
	 * register an adapter 
	 * 
	 * @param adapterClass type of adapter
	 * @param adapter implementation of adapter
	 * @throws PropertyIsLockedException
	 */
	public default <T> void setAdapter(Class<T> adapterClass, T adapter) throws PropertyIsLockedException
	{
		getConfigurationPropertyBlock().setAdapter(adapterClass, adapter);
	}
	
	/**
	 * get registered adapter
	 * 
	 * @param adapterClass type of adapter
	 * 
	 * @return registered adapter with specified adapterClass
	 */
	public default <T> T getAdapter(Class<T> adapterClass)
	{
		return getConfigurationPropertyBlock().getAdapter(adapterClass);
	}
	
	/**
	 * get registered adapter
	 * 
	 * @param adapterClass type of adapter
	 * @param adapterFactoryIfNotExists factory to create adapter if not exists , and store with specified key 
	 * 
	 * @return registered adapter with specified adapterClass
	 */
	public default <T> T getAdapter(Class<T> adapterClass, Supplier<T> adapterFactoryIfNotExists)
	{
		return getConfigurationPropertyBlock().getAdapter(adapterClass, adapterFactoryIfNotExists);
	}
	
	/**
	 * remove registered adapter
	 * 
	 * @param adapterClass type of adapter
	 * @throws PropertyIsLockedException
	 */
	public default <T> void removeAdapter(Class<T> adapterClass) throws PropertyIsLockedException
	{
		getConfigurationPropertyBlock().removeAdapter(adapterClass);
	}
	
	/**
	 * remove list of {@link IQueuedEvent}s queued  with one of {@code uuid}s
	 * 
	 * @param uuidList list of identifiers for {@link IQueuedEvent} to remove
	 * @return  true if one of {@link IQueuedEvent} was found and remove, otherwise false
	 */
	public boolean removeEventList(List<String> uuidList);
	
	/**
	 * returns list of scheduled {@link IQueueTask} matched by filter parameter
	 * 
	 * @param filter osgi-filter for {@link IQueueTask}-properties
	 * @return list of {@link IQueueTask}s matched by filter parameter
	 */
	public List<IQueueTask> getTaskList(Filter filter);
	
	/**
	 * returns map with taskid-task-pairs matched by filter parameter
	 * 
	 * @param filter osgi-filter for {@link IQueueTask}-properties
	 * @return map with taskid-task-pairs matched by filter parameter
	 */
	public Map<String,IQueueTask> getTaskIndex(Filter filter);
	
	/**
	 * schedule a anonymous {@link IQueueTask} to {@link IQueue}
	 * 
	 * equivalent to scheduleTask(null,task, null, -1, -1, -1);
	 * 
	 * @param task {@link IQueueTask} to schedule
	 * 
	 * @return generated taskid
	 */
	public String scheduleTask(IQueueTask task);
	
	/**
	 * schedule a {@link IQueueTask} to {@link IQueue}.
	 * 
	 * @param id registration-id for {@link IQueueTask} to schedule
	 * @param task {@link IQueueTask} to schedule
	 * 
	 * @return taskid (generated, if parameter id is null)
	 */
	public String scheduleTask(String id,IQueueTask task);
	
	/**
	 * schedule a {@link IQueueTask} to {@link IQueue}.
	 * 
	 * @param id registration-id for {@link IQueueTask} to schedule
	 * @param task {@link IQueueTask} to schedule
	 * @param propertyBlock {@link IQueueTask}-properties (factory in {@link IEventDispatcher})
	 * @param executionTimeStamp execution time millis
	 * @param timeOutValue timeout value in ms, before notify for timeout
	 * @param heartBeatTimeOut heartbeat-timeout value in ms, before notify for timeout
	 * 
	 * @return taskid (generated, in parameter id is null)
	 */
	public String scheduleTask(String id, IQueueTask task, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut );
	
	/**
	 * schedule a {@link IQueueTask} to {@link IQueue}.
	 * 
	 * @param id registration-id for {@link IQueueTask} to schedule
	 * @param task {@link IQueueTask} to schedule
	 * @param propertyBlock {@link IQueueTask}-properties (factory in {@link IEventDispatcher})
	 * @param executionTimeStamp execution time millis
	 * @param timeOutValue timeout value in ms, before notify for timeout
	 * @param heartBeatTimeOut heartbeat-timeout value in ms, before notify for timeout
	 * @param stopOnTimeOut stop unlinked worker-thread on timeout. This option is NOT necessary to create new worker running other tasks. Attention: can be dangerous  
	 * 
	 * @return taskid (generated, in parameter id is null)
	 */
	public String scheduleTask(String id, IQueueTask task, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut, boolean stopOnTimeOut );
	
	/**
	 * reset execution plan for an existing {@link IQueueTask}
	 * 
	 * @param id registration-id of {@link IQueueTask} in which reset execution plan 
	 * @param executionTimeStamp new execution time millis
	 * @param timeOutValue new timeout value in ms, before notify for timeout
	 * @param heartBeatTimeOut heartbeat-timeout value in ms, before notify for timeout
	 * @return affected {@link IQueueTask} or null if not found
	 */
	public IQueueTask rescheduleTask(String id, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut );
	
	/**
	 * returns {@link IQueueTask} scheduled under registration {@code id}
	 * 
	 * @param id registration-id for {@link IQueueTask}
	 * @return {@link IQueueTask} scheduled under registration {@code id}
	 */
	public IQueueTask getTask(String id);
	
	/**
	 * remove{@link IQueueTask} scheduled under registration {@code id}
	 * 
	 * @param id registration-id for {@link IQueueTask} to remove
	 * @return removed {@link IQueueTask} or null if no scheduled with {@code id} found
	 */
	public IQueueTask removeTask(String id);
	
	/**
	 * returns properties of {@link IQueueTask} scheduled under registration {@code id}
	 * 
	 * @param id registration-id for {@link IQueueTask}
	 * @return properties of {@link IQueueTask} scheduled under registration {@code id}
	 */
	public IPropertyBlock getTaskPropertyBlock(String id);
	
	/**
	 * Sends a signal. All {@link IQueueController} manage this {@link IQueue} and implements {@link IOnQueueSignal} will notify asynchronously by queueworker.
	 * 
	 * @param signal
	 */
	public void signal(String signal);
	
	/**
	 * Sends an osgi-{@link org.osgi.service.event.Event} synchronously. 
	 * Following this all {@link IQueueController} manage this {@link IQueue} and implements {@link IOnFiredEvent} will notify asynchronously by queueworker.
	 * 
	 * @param topic event topic
	 * @param properties event properties
	 */
	public void sendEvent(String topic, Map<String, ?> properties);
	
	/**
	 * Post an osgi-{@link org.osgi.service.event.Event} asynchronously. 
	 * Following this all {@link IQueueController} manage this {@link IQueue} and implements {@link IOnFiredEvent} will notify asynchronously by queueworker.
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
	public void setQueueMetricsEnabled(boolean enabled);
	
	/**
	 * 
	 * @return true if metrics capabilities is enabled , otherwise false
	 */
	public boolean isMetricsEnabled();
	
	/**
	 * returns global scope. global scope is a session creates this scope
	 *
	 * @return global scope
	 */
	public IQueue getGlobalScope();
	
	/**
	 * create {@link IQueueChildScope} for {@link IQueue}
	 * 
	 * @param scopeId unique id of scope (unique by queue) or null for auto-generation
	 * @param scopeName human readable name of scope (nullable)
	 * @param parentScope parent scope to define tree structure for scopes
	 * @param configurationProperties blue print for configuration propertyblock of new scope (nullable)
	 * @param stateProperties blue print for state propertyblock of new scope (nullable)
	 * 
	 * @return new scope, or null, if scope already exists
	 */
	public default IQueueChildScope createChildSessionScope(UUID scopeId,String scopeName, IQueueChildScope parentScope, Map<String,Object> configurationProperties, Map<String,Object> stateProperties)
	{
		return this.createChildScope(scopeId, scopeName, parentScope, configurationProperties, stateProperties, false, false);
	}
	
	/**
	 * create {@link IQueueChildScope} for {@link IQueue}
	 * 
	 * @param scopeId unique id of scope (unique by queue) or null for auto-generation
	 * @param scopeName human readable name of scope (nullable)
	 * @param parentScope parent scope to define tree structure for scopes
	 * @param configurationProperties blue print for configuration propertyblock of new scope (nullable)
	 * @param stateProperties blue print for state propertyblock of new scope (nullable)
	 * @param adoptContoller keep controller of parent queue
	 * @param adoptServices keep services of parent queue
	 * 
	 * @return new scope, or null, if scope already exists
	 */
	public IQueueChildScope createChildScope(UUID scopeId,String scopeName, IQueueChildScope parentScope, Map<String,Object> configurationProperties, Map<String,Object> stateProperties, boolean adoptContoller, boolean adoptServices);
	
	/*@Deprecated
	public default IQueueSessionScope createSessionScope(UUID scopeId,String scopeName, Map<String,Object> configurationProperties, Map<String,Object> stateProperties, boolean adoptContoller, boolean adoptServices)
	{
		return createSessionScope(scopeId, scopeName, null, configurationProperties, stateProperties, adoptContoller, adoptServices);
	}*/
	
	/**
	 * getter for child scope list. The child scopes list is defined by virtual tree structure.
	 * 
	 * @return immutable list of child scopes
	 */
	public List<IQueueChildScope> getChildScopes();
	
	/**
	 * returns scopelist of queue with positiv match result for {@code filter}
	 * 
	 * @param filter match condition for configuration propertyblock
	 * 
	 * @return scopelist of queue with positiv match result for {@code filter}
	 */
	public List<IQueueChildScope> getChildScopes(Filter filter);
	
	
	/**
	 * returns scope with given {@code scopeId}
	 * 
	 * @param scopeId id of scope to return
	 * 
	 * @return  scope with given {@code scopeId} or null, if scope not found
	 */
	public IQueueChildScope getChildScope(UUID scopeId);
	
	
	/**
	 * Enables rule with specified ruleId
	 * @param ruleId id of rule to enable
	 */
	public void enableRule(String ruleId);
	
	/**
	 * Disable rule with specified ruleId
	 * @param ruleId id of rule to disable
	 */
	public void disableRule(String ruleId);
}
