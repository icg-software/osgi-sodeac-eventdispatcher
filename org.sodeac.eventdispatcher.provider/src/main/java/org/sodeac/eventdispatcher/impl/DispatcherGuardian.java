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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IMetrics;

public class DispatcherGuardian extends Thread
{
	public static final long DEFAULT_WAIT_TIME = 108 * 108 * 13;
	
	public DispatcherGuardian(EventDispatcherImpl dispatcher)
	{
		super();
		this.eventDispatcher = eventDispatcher;
		this.jobTimeOutIndex = new HashMap<QueueImpl,JobObservable>();
		
		this.jobTimeOutIndexLock = new ReentrantReadWriteLock(true);
		this.jobTimeOutIndexReadLock = this.jobTimeOutIndexLock.readLock();
		this.jobTimeOutIndexWriteLock = this.jobTimeOutIndexLock.writeLock();
		super.setDaemon(true);
		super.setName(DispatcherGuardian.class.getSimpleName() + " " + dispatcher.getId());
	}
	
	private volatile EventDispatcherImpl eventDispatcher = null;
	private volatile boolean go = true;
	private volatile boolean isUpdateNotified = false;
	private volatile Object waitMonitor = new Object();
	private volatile Map<QueueImpl,JobObservable> jobTimeOutIndex = null;
	private volatile ReentrantReadWriteLock jobTimeOutIndexLock;
	private volatile ReadLock jobTimeOutIndexReadLock;
	private volatile WriteLock jobTimeOutIndexWriteLock;
	
	private volatile long currentWait = -1;
	
	@Override
	public void run()
	{
		long nextTimeOutTimeStamp = -1;
		List<QueueImpl> timeOutList = null;
		List<JobObservable> removeJobObservableList = null;
		boolean inTimeOut = false;
		
		while(go)
		{
			nextTimeOutTimeStamp = -1;
			if(timeOutList != null)
			{
				timeOutList.clear();
			}
			if(removeJobObservableList != null)
			{
				removeJobObservableList.clear();
			}
			
			jobTimeOutIndexReadLock.lock();
			
			try
			{
				long currentTimeStamp = System.currentTimeMillis();
				long heartBeatTimeOut = -1;
				long lastHeartBeat = -1;
				long heartBeatTimeOutStamp = -1;
				TaskContainer job = null;
				
				
				// Job TimeOut
				
				for(Entry<QueueImpl,JobObservable> entry : this.jobTimeOutIndex.entrySet())
				{
					inTimeOut = false;
					job = entry.getKey().getCurrentRunningJob();
					
					if((job == null) || (job != entry.getValue().job))
					{
						if(removeJobObservableList == null)
						{
							removeJobObservableList = new ArrayList<JobObservable>();
						}
						removeJobObservableList.add(entry.getValue());
					}
					
					// Job Timeout
					
					Long jobTimeOut = entry.getValue().jobTimeOut;
					
					if((jobTimeOut != null) && (jobTimeOut.longValue() > 0))
					{
						if(jobTimeOut.longValue() <= currentTimeStamp)
						{
							if(timeOutList == null)
							{
								timeOutList = new ArrayList<QueueImpl>();
							}
							timeOutList.add(entry.getKey());
							inTimeOut = true;
						}
						else
						{
							if(nextTimeOutTimeStamp < 0)
							{
								nextTimeOutTimeStamp = jobTimeOut.longValue();
							}
							else if(nextTimeOutTimeStamp > jobTimeOut.longValue())
							{
								nextTimeOutTimeStamp = jobTimeOut.longValue();
							}
						}
					}
					
					if(inTimeOut)
					{
						continue;
					}
					
					// HeartBeat Timeout
					
					heartBeatTimeOutStamp = -1;
					
					if(job !=  null)
					{
						heartBeatTimeOut = job.getTaskControl().getHeartBeatTimeOut();
						if(heartBeatTimeOut > 0)
						{
							try
							{
								lastHeartBeat = (Long)job.getMetrics().getQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
								heartBeatTimeOutStamp = lastHeartBeat + heartBeatTimeOut;
								if(lastHeartBeat > 0)
								{
									if(heartBeatTimeOutStamp <= currentTimeStamp)
									{
										if(timeOutList == null)
										{
											timeOutList = new ArrayList<QueueImpl>();
										}
										timeOutList.add(entry.getKey());
										inTimeOut = true;
									}
									else
									{
										if(nextTimeOutTimeStamp > heartBeatTimeOutStamp)
										{
											nextTimeOutTimeStamp = heartBeatTimeOutStamp;
										}
									}
								}
							}
							catch (Exception e) 
							{
								log(LogService.LOG_ERROR,"Error while check heartbeat timeout",e);
							}
						}
					}
				}
				
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while run DispatcherGuardian",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while run DispatcherGuardian",e);
			}
			
			jobTimeOutIndexReadLock.unlock();
			
			
			if(timeOutList != null)
			{
				for(QueueImpl queue : timeOutList)
				{
					queue.checkTimeOut();
				}
			}
			
			if((removeJobObservableList != null) && (! removeJobObservableList.isEmpty()))
			{
				jobTimeOutIndexWriteLock.lock();
				
				try
				{
					for(JobObservable jobObservable : removeJobObservableList)
					{
						JobObservable toRemoveObservable = this.jobTimeOutIndex.get(jobObservable.queue);
						if(toRemoveObservable == null)
						{
							continue;
						}
						if(toRemoveObservable != jobObservable)
						{
							continue;
						}
						
						TaskContainer job = jobObservable.queue.getCurrentRunningJob();
						if((job == null) || (job != jobObservable.job))
						{
							this.jobTimeOutIndex.remove(jobObservable.queue);
						}
					}
				}
				catch (Exception e) 
				{
					log(LogService.LOG_ERROR,"Exception while run DispatcherGuardian",e);
				}
				catch (Error e) 
				{
					log(LogService.LOG_ERROR,"Error while run DispatcherGuardian",e);
				}
			
				jobTimeOutIndexWriteLock.unlock();
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
							long waitTime = nextTimeOutTimeStamp - System.currentTimeMillis();
							if(waitTime > DEFAULT_WAIT_TIME)
							{
								waitTime = DEFAULT_WAIT_TIME;
							}
							if(nextTimeOutTimeStamp < 0)
							{
								waitTime = DEFAULT_WAIT_TIME;
							}
							if(waitTime > 0)
							{
								this.currentWait = System.currentTimeMillis() + waitTime;
								waitMonitor.wait(waitTime);
								this.currentWait = -1;
							}
						}
					}
				}
			}
			catch (InterruptedException e) {}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while run Dispatcher DispatcherGuardian",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while run Dispatcher DispatcherGuardian",e);
			}
		}
	}
	
	public void registerTimeOut(QueueImpl queue, TaskContainer job)
	{
		TaskControlImpl jobControl = job.getTaskControl();
		if(jobControl == null)
		{
			return;
		}
		
		long timeOutTimeStamp = jobControl.getTimeOut() + System.currentTimeMillis();
		
		jobTimeOutIndexWriteLock.lock();
		try
		{
			JobObservable jobObservable = this.jobTimeOutIndex.get(queue);
			if(jobObservable ==  null)
			{
				jobObservable =  new JobObservable();
				jobObservable.queue = queue;
				this.jobTimeOutIndex.put(queue,jobObservable);
			}
			if(jobControl.getTimeOut() > 0)
			{
				jobObservable.job = job;
				jobObservable.jobTimeOut = timeOutTimeStamp;
			}
			else if(jobControl.getHeartBeatTimeOut() > 0)
			{
				jobObservable.job = job;
			}
			else
			{
				return;
			}
		}
		finally 
		{
			jobTimeOutIndexWriteLock.unlock();
		}
		
		boolean notify = false;
		synchronized (this.waitMonitor)
		{
			this.isUpdateNotified = true;
			if(this.currentWait > 0)
			{
				if((timeOutTimeStamp > 0) && (timeOutTimeStamp < this.currentWait))
				{
					notify = true;
				}
				else
				{
					long heartBeatTimeOut = job.getTaskControl().getHeartBeatTimeOut();
					if(heartBeatTimeOut > 0)
					{
						try
						{
							long lastHeartBeat = (Long)job.getMetrics().getQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
							if(lastHeartBeat > 0)
							{
								long heartBeatTimeOutStamp = lastHeartBeat + heartBeatTimeOut;
								if(heartBeatTimeOutStamp < this.currentWait)
								{
									notify = true;
								}
							}
						}
						catch (Exception e) 
						{
							log(LogService.LOG_ERROR,"Error while check heartbeat timeout",e);
						}
					}
				}
			}
			
			if(notify)
			{
				try
				{
					waitMonitor.notify();
				}
				catch (Exception e) {}
				catch (Error e) {}
			}
		}
		
	}
	
	public void unregisterTimeOut(QueueImpl queue, TaskContainer job)
	{
		jobTimeOutIndexWriteLock.lock();
		try
		{
			JobObservable jobObservable = this.jobTimeOutIndex.get(queue);
			if(jobObservable !=  null)
			{
				if((jobObservable.job != null) && (jobObservable.job == job))
				{
					this.jobTimeOutIndex.remove(queue);
				}
			}
		}
		finally 
		{
			jobTimeOutIndexWriteLock.unlock();
		}
	}
	
	public void stopGuardian()
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
				log(LogService.LOG_ERROR,"Exception while stop DispatcherGuardian",e);
			}
		}
	}
	
	private void log(int logServiceLevel,String logMessage, Throwable e)
	{
		this.eventDispatcher.log(logServiceLevel, logMessage, e);
	}
	
	private class JobObservable
	{
		public Long jobTimeOut = null;
		public TaskContainer job = null;
		public QueueImpl queue = null;
	}
	
}
