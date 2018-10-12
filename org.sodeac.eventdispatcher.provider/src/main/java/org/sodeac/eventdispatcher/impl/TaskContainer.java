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
package org.sodeac.eventdispatcher.impl;

import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueueTask;

public class TaskContainer
{
	private IQueueTask task;
	private String id;
	private IPropertyBlock properties;
	private IMetrics metrics;
	private TaskControlImpl taskControl;
	private boolean namedTask = false;
	
	public IQueueTask getTask()
	{
		return task;
	}
	public void setTask(IQueueTask task)
	{
		this.task = task;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public IPropertyBlock getPropertyBlock()
	{
		return properties;
	}
	public void setPropertyBlock(IPropertyBlock properties)
	{
		this.properties = properties;
	}
	public IMetrics getMetrics()
	{
		return metrics;
	}
	public void setMetrics(IMetrics metrics)
	{
		this.metrics = metrics;
	}
	public TaskControlImpl getTaskControl()
	{
		return taskControl;
	}
	public void setTaskControl(TaskControlImpl taskControl)
	{
		this.taskControl = taskControl;
	}
	public boolean isNamedTask()
	{
		return namedTask;
	}
	public void setNamedTask(boolean namedJob)
	{
		this.namedTask = namedJob;
	}
}
