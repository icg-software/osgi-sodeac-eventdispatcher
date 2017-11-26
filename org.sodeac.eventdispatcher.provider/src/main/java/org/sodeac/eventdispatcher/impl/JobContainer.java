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
package org.sodeac.eventdispatcher.impl;

import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueueJob;

public class JobContainer
{
	private IQueueJob job;
	private String id;
	private IPropertyBlock properties;
	private IMetrics metrics;
	private JobControlImpl jobControl;
	private boolean namedJob = false;
	
	public IQueueJob getJob()
	{
		return job;
	}
	public void setJob(IQueueJob job)
	{
		this.job = job;
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
	public JobControlImpl getJobControl()
	{
		return jobControl;
	}
	public void setJobControl(JobControlImpl jobControl)
	{
		this.jobControl = jobControl;
	}
	public boolean isNamedJob()
	{
		return namedJob;
	}
	public void setNamedJob(boolean namedJob)
	{
		this.namedJob = namedJob;
	}
}
