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
package org.sodeac.eventdispatcher.impl;

import java.util.concurrent.locks.ReentrantLock;

import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.IPropertyBlock;

public class TaskControlImpl implements ITaskControl
{
	private volatile boolean done = false;
	private volatile boolean inTimeOut = false;
	private volatile long executionTimeStamp = 0L;
	private volatile long timeOutValue = EventDispatcherConstants.DEFAULT_TIMEOUT;
	private volatile long heartBeatTimeOut = -1;
	
	private volatile boolean stopTaskOnTimeout = false;
	private volatile boolean inRun = false;
	private volatile ExecutionTimeStampSource executionTimeStampSource = ITaskControl.ExecutionTimeStampSource.SCHEDULE;
	
	
	private IPropertyBlock taskPropertyBlock = null;
	
	private ReentrantLock executionTimestampLock = null;
	
	public TaskControlImpl(IPropertyBlock taskPropertyBlock)
	{
		super();
		this.executionTimeStamp = System.currentTimeMillis();
		this.taskPropertyBlock = taskPropertyBlock;
		
		this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_EXECUTION_TIMESTAMP, this.executionTimeStamp);
		this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_TIMEOUT_VALUE, this.timeOutValue);
		this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_HEARTBEAT_TIMEOUT, this.heartBeatTimeOut);
		
		this.executionTimestampLock = new ReentrantLock();
	}
	
	public void preRun()
	{
		this.inRun = true;
		this.done = true;
	}
	
	public void preRunPeriodicTask()
	{
		this.inRun = true;
	}
	
	public void postRun()
	{
		this.inRun = false;
	}
	
	
	
	@Override
	public boolean setDone()
	{
		boolean old = this.done;
		this.done = true;
		return old;
	}

	@Override
	public void timeOut()
	{
		this.inTimeOut = true;
		this.done = true;
	}
	
	public void timeOutService()
	{
		this.inTimeOut = true;
	}

	@Override
	public boolean isInTimeOut()
	{
		return inTimeOut;
	}

	@Override
	public long getExecutionTimeStamp()
	{
		return this.executionTimeStamp;
	}
	
	@Override
	public ExecutionTimeStampSource getExecutionTimeStampSource()
	{
		return this.executionTimeStampSource;
	}
	
	public void setExecutionTimeStampSource(ExecutionTimeStampSource executionTimeStampSource)
	{
		this.executionTimeStampSource = executionTimeStampSource;
	}

	public long getExecutionTimeStampIntern()
	{
		return this.executionTimeStamp;
	}
	
	@Override
	public boolean setExecutionTimeStamp(long executionTimeStamp, boolean force)
	{
		executionTimestampLock.lock();
		try
		{
			long old = this.executionTimeStamp;
			if
			( 
				(executionTimeStamp < old) || 
				force || 
				(old < System.currentTimeMillis()) || 
				(this.executionTimeStampSource == ITaskControl.ExecutionTimeStampSource.SCHEDULE) ||
				(this.executionTimeStampSource == ITaskControl.ExecutionTimeStampSource.WORKER) ||
				(this.executionTimeStampSource == ITaskControl.ExecutionTimeStampSource.PERODIC)
			) 
			{
				this.executionTimeStamp = executionTimeStamp;
				this.executionTimeStampSource = ITaskControl.ExecutionTimeStampSource.WORKER;
				this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_EXECUTION_TIMESTAMP, this.executionTimeStamp);
				
				if(inRun)
				{
					this.done = false;
				}
				
				return true;
			}
			
			return false;
		}
		finally 
		{
			executionTimestampLock.unlock();
		}
	}
	
	public void setExecutionTimeStampPeriodic(long executionTimeStamp)
	{
		executionTimestampLock.lock();
		try
		{
			long old = this.executionTimeStamp;
			if
			(
				(old > System.currentTimeMillis()) &&
				(this.executionTimeStampSource == ITaskControl.ExecutionTimeStampSource.RESCHEDULE) &&
				(executionTimeStamp > 0)
			)
			{
				return;
			}
			
			this.executionTimeStamp = executionTimeStamp;
			this.executionTimeStampSource = ITaskControl.ExecutionTimeStampSource.PERODIC;
			this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_EXECUTION_TIMESTAMP, this.executionTimeStamp);
		}
		finally 
		{
			executionTimestampLock.unlock();
		}
	}
	
	public void setExecutionTimeStampSchedule(long executionTimeStamp)
	{
		executionTimestampLock.lock();
		try
		{
			this.executionTimeStamp = executionTimeStamp;
			this.executionTimeStampSource = ITaskControl.ExecutionTimeStampSource.SCHEDULE;
			this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_EXECUTION_TIMESTAMP, this.executionTimeStamp);
		}
		finally 
		{
			executionTimestampLock.unlock();
		}
		
	}
	
	public void setExecutionTimeStampReschedule(long executionTimeStamp)
	{
		executionTimestampLock.lock();
		try
		{
			long old = this.executionTimeStamp;
			if
			( 
				(executionTimeStamp < old) || 
				(old < System.currentTimeMillis()) || 
				(this.executionTimeStampSource == ITaskControl.ExecutionTimeStampSource.SCHEDULE) ||
				(this.executionTimeStampSource == ITaskControl.ExecutionTimeStampSource.RESCHEDULE) 
			) 
			{
				this.executionTimeStamp = executionTimeStamp;
				this.executionTimeStampSource = ITaskControl.ExecutionTimeStampSource.RESCHEDULE;
				this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_EXECUTION_TIMESTAMP, this.executionTimeStamp);	
			}
		}
		finally 
		{
			executionTimestampLock.unlock();
		}
	}

	@Override
	public long getTimeOut()
	{
		return this.timeOutValue;
	}

	@Override
	public long setTimeOut(long timeOut)
	{
		long old = this.timeOutValue;
		this.timeOutValue = timeOut;
		this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_TIMEOUT_VALUE, this.timeOutValue);
		return old;
	}
	
	@Override
	public long getHeartBeatTimeOut()
	{
		return this.heartBeatTimeOut;
	}
	
	public long setHeartBeatTimeOut(long heartBeatTimeOut)
	{
		long old =  this.heartBeatTimeOut;
		this.heartBeatTimeOut = heartBeatTimeOut;
		this.taskPropertyBlock.setProperty(EventDispatcherConstants.PROPERTY_KEY_HEARTBEAT_TIMEOUT, this.heartBeatTimeOut);
		return old;
	}

	@Override
	public boolean isDone()
	{
		return this.done;
	}

	@Override
	public boolean setStopOnTimeOutFlag(boolean value)
	{
		boolean oldValue = this.stopTaskOnTimeout;
		this.stopTaskOnTimeout = value;
		return oldValue;
	}
	
	public boolean getStopOnTimeOutFlag()
	{
		return this.stopTaskOnTimeout;
	}
}
