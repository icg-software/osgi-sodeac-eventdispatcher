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

import java.util.List;

/**
 * Context of task running
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueueTaskContext
{
	/**
	 * getter for queue
	 * 
	 * @return queue
	 */
	public IQueue getQueue();
	
	/**
	 * getter for task metrics
	 * 
	 * @return task metrics
	 */
	public IMetrics getTaskMetrics();
	
	/**
	 * getter for task property block
	 * 
	 * @return task property block
	 */
	public IPropertyBlock getTaskPropertyBlock(); 
	
	/**
	 * getter for task control
	 * 
	 * @return task control
	 */
	public ITaskControl getTaskControl();
	
	/**
	 * getter for current processing task list
	 * 
	 * @return current processing task list
	 */
	public List<IQueueTask> currentProcessedTaskList();
}
