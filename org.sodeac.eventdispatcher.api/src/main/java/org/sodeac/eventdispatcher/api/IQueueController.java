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

/**
 * 
 * An eventcontroller reacts to a wide variety of queue happenings, if it implements appropriate extension interfaces.
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueueController extends IQueueComponent
{
	/**
	 * configuration property key to declare an {@link IQueueController} as consumer of {@link org.osgi.service.event.Event} with given topics (value of property)
	 */
	public static final String PROPERTY_CONSUME_EVENT_TOPIC 	= "consume_event_topic";
	
	/**
	 * configuration property key to declare an {@link IQueueController} for disabling metrics on attach {@link IQueue}
	 */
	public static final String PROPERTY_DISABLE_METRICS			= "queue_disable_metrics";
	
	/**
	 * configuration property key to declare the name of {@link IQueueController} in jmx tools
	 */
	public static final String PROPERTY_JMX_NAME				= "jmxname";
	
	/**
	 * configuration property key to declare category subfolder of {@link IQueueController} in jmx tools
	 */
	public static final String PROPERTY_JMX_CATEGORY			= "jmxcategory";
	
}
