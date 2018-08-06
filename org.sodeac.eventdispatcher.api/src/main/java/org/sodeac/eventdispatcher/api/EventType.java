/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Type of events to consume by {@link IQueueComponent}s.<br>
 * 
 * <br>
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public enum EventType 
{
	
	/**
	 * {@link Event} send or posted by {@link EventAdmin}
	 */
	PublishedByEventAdmin(1),
	
	/**
	 * {@link Event} scheduled by ({@link IEventDispatcher}) : {@link IEventDispatcher#schedule(String, Event)} / {@link IEventDispatcher#schedule(String, java.util.List)}
	 */
	ScheduledByEventDispatcher(2),
	
	/**
	 * {@link Event} published by ({@link IEventDispatcher})
	 */
	PublishedByEventDispatcher(4),
	
	/**
	 * {@link Event} published by ({@link IEventDispatcher}) or send / posted by {@link EventAdmin}
	 */
	PublishedByAny(5),
	
	/**
	 * {@link Event} scheduled or published by ({@link IEventDispatcher})
	 */
	AllByEventDispatcher(6),
	
	/**
	 * {@link Event} scheduled or published by ({@link IEventDispatcher}) or send or posted by {@link EventAdmin}
	 */
	All(7);
	
	private EventType(int intValue)
	{
		this.intValue = intValue;
	}
	
	private static volatile Set<EventType> ALL = null;
	
	private int intValue;
	
	/**
	 * getter for all event types
	 * 
	 * @return Set of all event types
	 */
	public static Set<EventType> getAll()
	{
		if(EventType.ALL == null)
		{
			EnumSet<EventType> all = EnumSet.allOf(EventType.class);
			EventType.ALL = Collections.unmodifiableSet(all);
		}
		return EventType.ALL;
	}
	
	/**
	 * search event type enum represents by {@code value}
	 * 
	 * @param value integer value of event type
	 * 
	 * @return event type enum represents by {@code value}
	 */
	public static EventType findByInteger(int value)
	{
		for(EventType metrics : getAll())
		{
			if(metrics.intValue == value)
			{
				return metrics;
			}
		}
		return null;
	}
	
	/**
	 * search event type enum represents by {@code name}
	 * 
	 * @param name of event type
	 * 
	 * @return event type enum represents by {@code name}
	 */
	public static EventType findByName(String name)
	{
		for(EventType metrics : getAll())
		{
			if(metrics.name().equalsIgnoreCase(name))
			{
				return metrics;
			}
		}
		return null;
	}
}
