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
import java.util.Map;

import org.osgi.service.event.Event;

/**
 * wrapper object for queued events
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
	 * @returnproperty for {@link IQueuedEvent} registered with {@code key} or null, if absent
	 */
	public Object getProperty(String key);
	
	/**
	 * get list of all property-keys for {@link IQueuedEvent}
	 * 
	 * @return list of all property-keys for {@link IQueuedEvent}
	 */
	public List<String> getPropertyKeys();
	
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
}
