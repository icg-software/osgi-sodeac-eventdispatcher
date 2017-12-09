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
 * A {@link IQueueJob} acts as processor for queued {@link IQueuedEvent}s or as service.
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
	public static final String PROPERTY_KEY_THROWED_EXCEPTION		= "THROWED_EXCEPTION"	;
	
	
	public static final long DEFAULT_TIMEOUT = 1080 * 108;
	
	/**
	 * invoked onetime at initialization of this job
	 * 
	 * @param id registration-id of this job
	 * @param metrics metric-handler for this job
	 * @param propertyBlock properties for this job
	 * @param jobControl state-handler for this job
	 */
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl);
	
	/**
	 * run this job, invoked by queue-worker.
	 * 
	 * @param queue parent-{@link IQueue} 
	 * @param metrics metric-handler for this job
	 * @param propertyBlock properties for this job
	 * @param jobControl state-handler for this job
	 * @param currentProcessedJobList all jobs run in the same task phase
	 */
	public void run(IQueue queue,IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl, List<IQueueJob> currentProcessedJobList);
}
