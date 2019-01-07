/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

public class EventDispatcherConstants
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
	 * configuration property key to declare category subfolder of {@link IQueueController} in jmx tools
	 */
	public static final String PROPERTY_JMX_CATEGORY			= "jmxcategory";
	/**
	 * configuration property key to declare the name of {@link IQueueController} in jmx tools
	 */
	public static final String PROPERTY_JMX_NAME				= "jmxname";
	
	// Service
	
	public static final String PROPERTY_HB_TIMEOUT_MS 					= "serviceheartbeattimeout";
	public static final String PROPERTY_PERIODIC_REPETITION_INTERVAL 	= "serviceperiodicrepetitioninterval";
	public static final String PROPERTY_SERVICE_ID 						= "serviceid";
	public static final String PROPERTY_START_DELAY_MS 					= "servicestartdelay";
	public static final String PROPERTY_TIMEOUT_MS 						= "servicetimeout";
	
	// Dispatcher
	
	public static final String PROPERTY_DISPATCHER_ID = "dispatcherid";
	public static final String PROPERTY_ID = "id";
	public static final String PROPERTY_QUEUE_ID = "queueid";
	public static final String PROPERTY_QUEUE_MATCH_FILTER = "queueconfigurationmatchfilter";
	public static final String PROPERTY_QUEUE_TYPE = "queuetype";
	
	// TASK
	
	public static final String DEFAULT_DISPATCHER_ID = "default";
	public static final long DEFAULT_TIMEOUT = 1080 * 1080;
	public static final String PROPERTY_KEY_EXECUTION_TIMESTAMP 	= "EXECUTION_TIMESTAMP"	;
	public static final String PROPERTY_KEY_HEARTBEAT_TIMEOUT 		= "HEARTBEAT_TIMEOUT "	;
	public static final String PROPERTY_KEY_TASK_ID 				= "TASK_ID"				;
	public static final String PROPERTY_KEY_THROWED_EXCEPTION		= "THROWED_EXCEPTION"	;
	public static final String PROPERTY_KEY_TIMEOUT_VALUE 			= "TIMEOUT_VALUE"		;

}
