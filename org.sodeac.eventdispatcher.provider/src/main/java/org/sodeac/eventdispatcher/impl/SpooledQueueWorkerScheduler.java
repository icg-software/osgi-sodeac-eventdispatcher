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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.service.log.LogService;

public class SpooledQueueWorkerScheduler extends Thread
{
	public static final long DEFAULT_WAIT_TIME = 108 * 108 * 7;
	
	public SpooledQueueWorkerScheduler(EventDispatcherImpl eventDispatcher)
	{
		super();
		this.eventDispatcher = eventDispatcher;
		
		this.lock = new ReentrantReadWriteLock(true);
		this.readLock = this.lock.readLock();
		this.writeLock = this.lock.writeLock();
		
		this.scheduledList = new ArrayList<SpooledQueueWorker>();
		this.removeList = new ArrayList<SpooledQueueWorker>();
		
		super.setDaemon(true);
		super.setName(SpooledQueueWorkerScheduler.class.getSimpleName() + " " + eventDispatcher.getId());
	}
	
	private EventDispatcherImpl eventDispatcher = null;
	private volatile boolean go = true;
	private volatile boolean isUpdateNotified = false;
	private volatile long currentWait = -1;
	
	private Object waitMonitor = new Object();
	private List<SpooledQueueWorker> removeList = null;
	private ReentrantReadWriteLock lock;
	private ReadLock readLock;
	private WriteLock writeLock;
	
	private List<SpooledQueueWorker> scheduledList = null;
	
	protected SpooledQueueWorker scheduleQueueWorker(QueueImpl queue, long wakeUpTime)
	{
		SpooledQueueWorker spooledQueueWorker = new SpooledQueueWorker(queue, wakeUpTime);
		this.writeLock.lock();
		try
		{
			scheduledList.add(spooledQueueWorker);
		}
		finally 
		{
			this.writeLock.unlock();
		}
		
		synchronized (this.waitMonitor)
		{
			this.isUpdateNotified = true;
			if(this.currentWait > 0)
			{
				if(wakeUpTime < this.currentWait)
				{
					waitMonitor.notify();
				}
			}
		}
	
		return spooledQueueWorker;
	}
	
	@Override
	public void run()
	{
		long spoolCleanRun = 0;
		while(go)
		{
			removeList.clear();
			long minWakeUpTimestamp = -1;
			
			long now = System.currentTimeMillis();
			
			if(spoolCleanRun < now - DEFAULT_WAIT_TIME)
			{
				try
				{
					spoolCleanRun = now;
					eventDispatcher.checkTimeoutWorker();
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"clean worker spooler", e);
				}
				catch (Error e) 
				{
					log(LogService.LOG_ERROR,"clean worker spooler", e);
				}
			}
			
			
			try
			{
				readLock.lock();
				try
				{
					for(SpooledQueueWorker worker : this.scheduledList)
					{
						if(! worker.isValid())
						{
							removeList.add(worker);
							continue;
						}
						if(now >= worker.getWakeupTime())
						{
							removeList.add(worker);
							continue;
						}
						if(minWakeUpTimestamp < 0)
						{
							minWakeUpTimestamp = worker.getWakeupTime();
						}
						else if(minWakeUpTimestamp > worker.getWakeupTime())
						{
							minWakeUpTimestamp = worker.getWakeupTime();
						}
					}
				}
				finally 
				{
					readLock.unlock();
				}
				
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while run SpooledQueueWorkerScheduler",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while run SpooledQueueWorkerScheduler",e);
			}
			
			
			if(! removeList.isEmpty())
			{
				writeLock.lock();
				
				try
				{
					for(SpooledQueueWorker toRemove : removeList)
					{
						this.scheduledList.remove(toRemove);
					}
				}
				finally 
				{
					writeLock.unlock();
				}
				
				for(SpooledQueueWorker toRemove : removeList)
				{
					if(! toRemove.isValid())
					{
						continue;
					}
					toRemove.getQueue().notifyOrCreateWorker(toRemove.getWakeupTime());
				}
			}
			
			
			try
			{
				synchronized (this.waitMonitor)
				{
					if(go)
					{
						if(isUpdateNotified)
						{
							isUpdateNotified = false;
						}
						else
						{
							long wait = DEFAULT_WAIT_TIME;
							if(minWakeUpTimestamp > 0)
							{
								wait = minWakeUpTimestamp - System.currentTimeMillis();
							}
							if(wait > DEFAULT_WAIT_TIME)
							{
								wait = DEFAULT_WAIT_TIME;
							}
							if(wait > 0)
							{
								this.currentWait = wait + System.currentTimeMillis();
								waitMonitor.wait(wait);
								this.currentWait = -1;
							}
						}
					}
				}
			}
			catch (InterruptedException e) {}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while run SpooledQueueWorkerScheduler",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while run SpooledQueueWorkerScheduler",e);
			}
		}
	}
	public void stopScheduler()
	{
		this.go = false;
		synchronized (this.waitMonitor)
		{
			try
			{
				this.waitMonitor.notify();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while stop Spooled Queue Worker Scheduler",e);
			}
		}
	}
	
	private void log(int logServiceLevel,String logMessage, Throwable e)
	{
		this.eventDispatcher.log(logServiceLevel, logMessage, e);
	}
	
}
