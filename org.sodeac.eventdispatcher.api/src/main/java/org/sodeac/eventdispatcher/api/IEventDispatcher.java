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
package org.sodeac.eventdispatcher.api;

import java.util.List;
import java.util.concurrent.Future;

import org.osgi.service.event.Event;

/**
 * 
 * API for eventdispatcher service
 * 
 *  @author Sebastian Palarus
 *
 */
public interface IEventDispatcher
{
	public static final String PROPERTY_ID = "id";
	public static final String PROPERTY_QUEUE_ID = "queueid";
	public static final String PROPERTY_QUEUE_MATCH_FILTER = "queueconfigurationmatchfilter";
	public static final String PROPERTY_QUEUE_TYPE = "queuetype";
	public static final String PROPERTY_DISPATCHER_ID = "dispatcherid";
	public static final String DEFAULT_DISPATCHER_ID = "default";
	
	/**
	 * schedule an osgi event to eventdispatcher queue
	 * 
	 * @param queueId id of {@link IQueue} 
	 * @param event osgi-event to schedule to {@link IQueue} 
	 * 
	 * 
	 * @return Future of {@link IScheduleResult}
	 * @throws QueueNotFoundException
	 * @throws QueueIsFullException
	 */
	public Future<IScheduleResult> schedule(String queueId,Event event) throws QueueNotFoundException, QueueIsFullException;
	
	/**
	 * schedule a list of osgi events to eventdispatcher queue
	 * 
	 * @param queueId id of {@link IQueue} 
	 * @param eventList list of osgi-events to schedule to {@link IQueue} 
	 * 
	 * 
	 * @return Future of {@link IScheduleResult}
	 * @throws QueueNotFoundException
	 * @throws QueueIsFullException
	 */
	public Future<IScheduleResult> schedule(String queueId,List<Event> eventList) throws QueueNotFoundException, QueueIsFullException;
	
	/**
	 * factory-methode creating instance of {@link IPropertyBlock} 
	 * 
	 * @return instance of {@link IPropertyBlock} 
	 */
	public IPropertyBlock createPropertyBlock();
	
	/**
	 * request for all {@link IQueue}-IDs
	 * 
	 * @return {@link java.util.List} with queueIds
	 */
	public List<String> getQueueIdList();
	
	/**
	 * getter to request for {@link IQueue} with given id
	 * 
	 * @param queueId  id for queue
	 * @return instance of {@link IQueue} registered with {@code queueId}
	 */
	public IQueue getQueue(String queueId);
	
	/**
	 * getter for propertyblock of dispatcher
	 * 
	 * @return {@link IPropertyBlock} of dispatcher
	 */
	public IPropertyBlock getPropertyBlock();
	
	/**
	 * getter for id of dispatcher.
	 * 
	 * @return id of dispatcher
	 */
	public String getId();
}
