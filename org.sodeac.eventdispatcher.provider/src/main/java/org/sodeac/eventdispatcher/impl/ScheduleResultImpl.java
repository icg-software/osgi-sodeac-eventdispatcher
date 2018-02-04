/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import org.sodeac.eventdispatcher.api.IScheduleResult;

public class ScheduleResultImpl implements IScheduleResult
{
	private				ReentrantLock		lock						= null;
	private 			CountDownLatch 		countDownLatch 				= null;
	private volatile 	boolean 			scheduledIsFinished 		= false; 
	
	private volatile 	boolean 			scheduled 					= false;
	private volatile 	List<Throwable> 	errorList 					= null;
	private volatile 	Object 				detailResultObject 			= null;
	private volatile 	List<Object> 		detailResultObjectList 		= null;
	
	public ScheduleResultImpl()
	{
		super();
		this.countDownLatch = new CountDownLatch(1);
		this.lock = new ReentrantLock(true);
	}
	
	protected void waitForScheduledIsFinished()
	{
		while(! scheduledIsFinished)
		{
			try
			{
				this.countDownLatch.await();
			}
			catch (Exception e) {}
		}
	}
	
	protected void schedulePhaseIsFinished()
	{
		this.scheduledIsFinished = true;
		this.countDownLatch.countDown();
	}
	
	@Override
	public void addError(Throwable throwable)
	{
		if(this.errorList == null)
		{
			this.errorList = new ArrayList<Throwable>();
		}
		this.errorList.add(throwable);
	}
	
	@Override
	public List<Throwable> getErrorList()
	{
		this.lock.lock();
		try
		{
			if(this.errorList == null)
			{
				return null;
			}
			return Collections.unmodifiableList(new ArrayList<Throwable>(this.errorList));
		}
		finally 
		{
			this.lock.unlock();
		}
	}
	
	@Override
	public boolean hasErrors()
	{
		if(this.errorList == null)
		{
			return false;
		}
		return ! this.errorList.isEmpty();
	}

	@Override
	public boolean isScheduled()
	{
		return scheduled;
	}

	@Override
	public void setScheduled()
	{
		this.scheduled = true;
	}

	@Override
	public Object getDetailResultObject()
	{
		return detailResultObject;
	}

	@Override
	public void setDetailResultObject(Object detailResultObject)
	{
		this.detailResultObject = detailResultObject;
	}

	@Override
	public List<Object> getDetailResultObjectList()
	{
		this.lock.lock();
		try
		{
			if(this.detailResultObjectList == null)
			{
				return null;
			}
			return Collections.unmodifiableList(new ArrayList<Object>(this.detailResultObjectList));
		}
		finally 
		{
			this.lock.unlock();
		}
	}

	@Override
	public void addDetailResultObjectList(Object detailResultObject)
	{
		this.lock.lock();
		try
		{
			if(this.detailResultObjectList == null)
			{
				this.detailResultObjectList = new ArrayList<Object>();
			}
			this.detailResultObjectList.add(detailResultObject);
		}
		finally 
		{
			this.lock.unlock();
		}
		this.detailResultObjectList = detailResultObjectList;
	}
	
	
}
