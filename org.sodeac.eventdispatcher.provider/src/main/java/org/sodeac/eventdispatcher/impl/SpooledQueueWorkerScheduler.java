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

import org.sodeac.multichainlist.MultiChainList;
import org.sodeac.multichainlist.Snapshot;
import org.sodeac.multichainlist.Link;

import org.osgi.service.log.LogService;

public class SpooledQueueWorkerScheduler extends Thread
{
	public static final long DEFAULT_WAIT_TIME = 108 * 108 * 7;
	
	public SpooledQueueWorkerScheduler(EventDispatcherImpl eventDispatcher)
	{
		super();
		this.eventDispatcher = eventDispatcher;
		
		this.scheduledList = new MultiChainList<SpooledQueueWorker>();
		
		super.setDaemon(true);
		super.setName(SpooledQueueWorkerScheduler.class.getSimpleName() + " " + eventDispatcher.getId());
	}
	
	private EventDispatcherImpl eventDispatcher = null;
	private volatile boolean go = true;
	private volatile boolean isUpdateNotified = false;
	private volatile long currentWait = -1;
	
	private Object waitMonitor = new Object();
	private MultiChainList<SpooledQueueWorker> scheduledList = null;
	
	protected SpooledQueueWorker scheduleQueueWorker(QueueImpl queue, long wakeUpTime)
	{
		SpooledQueueWorker spooledQueueWorker = new SpooledQueueWorker(queue, wakeUpTime);
		scheduledList.append(spooledQueueWorker,null);
		
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
		SpooledQueueWorker worker;
		long spoolCleanRun = 0;
		while(go)
		{
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
				Snapshot<SpooledQueueWorker> snapshot = this.scheduledList.createSnapshot(null, null);
				try
				{
					for(Link<SpooledQueueWorker> workerLink : snapshot.linkIterable())
					{
						worker = workerLink.getElement();
						if(worker == null)
						{
							workerLink.unlink();
							continue;
						}
						if(! worker.isValid())
						{
							workerLink.unlink();							
							continue;
						}
						if(now >= worker.getWakeupTime())
						{
							worker.getQueue().notifyOrCreateWorker(worker.getWakeupTime());
							workerLink.unlink();
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
					snapshot.close();
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
		Snapshot<SpooledQueueWorker> snapshot = this.scheduledList.createSnapshot(null, null); // TODO clearAlll
		try
		{
			for(Link<SpooledQueueWorker> workerLink : snapshot.linkIterable())
			{
				workerLink.unlink();
			}
		}
		finally 
		{
			try
			{
				snapshot.close();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Error close snapshot",e);
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
