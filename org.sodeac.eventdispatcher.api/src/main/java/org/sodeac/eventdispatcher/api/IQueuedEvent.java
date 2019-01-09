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

import java.util.Map;
import java.util.Set;

import org.osgi.service.event.Event;

/**
 * wrapper object for queued {@link org.osgi.service.event.Event}s
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueuedEvent
{
	/**
	 * 
	 * @return wrapped {@link org.osgi.service.event.Event}
	 */
	public Event getEvent();
	
	/**
	 * 
	 * @return unique id generated for this event
	 */
	public String getUUID();
	
	/**
	 * 
	 * @return parent queue
	 */
	public IQueue getQueue();
	
	/**
	 * insert or update property for {@link IQueuedEvent}
	 * 
	 * @param key property key
	 * @param value property value
	 * @return overwritten property or null
	 */
	public Object setProperty(String key,Object value);
	
	/**
	 * get property for {@link IQueuedEvent} registered with {@code key}
	 * 
	 * @param key property key
	 * 
	 * @return property for {@link IQueuedEvent} registered with {@code key} or null, if absent
	 */
	public Object getProperty(String key);
	
	/**
	 * get aset of all property-keys for {@link IQueuedEvent}
	 * 
	 * @return set of all property-keys for {@link IQueuedEvent}
	 */
	public Set<String> getPropertyKeySet();
	
	/**
	 * get immutable deep copy of property-keys {@link IQueuedEvent}
	 * 
	 * @return immutable deep copy of property-keys {@link IQueuedEvent}
	 */
	public Map<String,Object> getProperties();
	
	/**
	 * get properties of wrapped {@link org.osgi.service.event.Event}
	 * 
	 * @return properties of wrapped {@link org.osgi.service.event.Event}
	 */
	public Map<String,Object> getNativeEventProperties();
	
	/**
	 * getter for {@link IQueueEventResult} to inform schedule invoker about result
	 * 
	 * @return schedule result object
	 */
	public IQueueEventResult getScheduleResultObject();
	
	/**
	 * get registered adapter
	 * 
	 * @param adapterClass type of adapter
	 * 
	 * @return registered adapter with specified adapterClass
	 */
	@SuppressWarnings("unchecked")
	public default <T> T getAdapter(Class<T> adapterClass)
	{
		return (T)getProperty(adapterClass.getCanonicalName());
	}
	
	/**
	 * remove event from parent queue
	 */
	public default void removeFromQueue()
	{
		getQueue().removeEvent(getUUID());
	}
	
	/**
	 * get create-timestamp
	 * 
	 * @return create-timestamp
	 */
	public long getCreateTimeStamp();
	
}
