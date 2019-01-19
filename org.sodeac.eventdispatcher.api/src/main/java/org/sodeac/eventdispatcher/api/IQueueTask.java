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


/**
 * A {@link IQueueTask} acts as processor for queued {@link IQueuedEvent}s or as service.
 * 
 * @author Sebastian Palarus
 *
 */
@FunctionalInterface
public interface IQueueTask
{
	
	/**
	 * invoked one time at initialization of this task
	 * 
	 * @param queue parent-{@link IQueue} 
	 * @param id registration-id of this task
	 * @param metrics metric-handler for this task
	 * @param propertyBlock properties for this task
	 * @param taskControl state-handler for this task
	 */
	public default void configure(IQueue queue, String id, IMetrics metrics, IPropertyBlock propertyBlock, ITaskControl taskControl) {};
	
	/**
	 * run this task, invoked by queue-worker.
	 * 
	 * @param context of task running
	 * @throws Exception
	 */
	public void run(IQueueTaskContext taskContext) throws Exception;
}
