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

/**
 * A QueueJob is a job reacts to incomming events. the principal aim of a job is recreate and send events depends on metrics, time or other things 
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueueJob
{
	
	public static final String PROPERTY_KEY_JOB_ID 					= "JOB_ID"				;
	public static final String PROPERTY_KEY_EXECUTION_TIMESTAMP 	= "EXECUTION_TIMESTAMP"	;
	public static final String PROPERTY_KEY_TIMEOUT_VALUE 			= "TIMEOUT_VALUE"		;
	public static final String PROPERTY_KEY_HEARTBEAT_TIMEOUT 		= "HEARTBEAT_TIMEOUT "	;
	
	
	public static final long DEFAULT_TIMEOUT = 1080 * 108;
	
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl);
	public void run(IQueue queue,IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl, List<IQueueJob> currentProcessedJobList);
}
