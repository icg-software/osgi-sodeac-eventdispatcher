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
 * OSGi-service interface to register {@link IQueueJob} as OSGi-service / component
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueueService extends IQueueJob, IQueueComponent
{
	public static final String PROPERTY_QUEUE_ID 						= IEventDispatcher.PROPERTY_QUEUE_ID;
	
	public static final String PROPERTY_SERVICE_ID 						= "serviceid";
	public static final String PROPERTY_TIMEOUT_MS 						= "servicetimeout";
	public static final String PROPERTY_HB_TIMEOUT_MS 					= "serviceheartbeattimeout";
	public static final String PROPERTY_START_DELAY_MS 					= "servicestartdelay";
	public static final String PROPERTY_PERIODIC_REPETITION_INTERVAL 	= "serviceperiodicrepetitioninterval"; 
}
