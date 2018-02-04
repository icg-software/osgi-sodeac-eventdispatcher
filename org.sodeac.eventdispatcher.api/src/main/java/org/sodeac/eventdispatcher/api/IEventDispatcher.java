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
	 * @param event osgi-event to schedule to {@link IQueue} 
	 * @param queueId id of {@link IQueue} 
	 * 
	 * @return Future of {@link IScheduleResult}
	 */
	public Future<IScheduleResult> schedule(Event event, String queueId);
	
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
}
