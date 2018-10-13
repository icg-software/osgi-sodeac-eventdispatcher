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
		this.taskTimeOutIndex = new HashMap<QueueImpl,TaskObservable>();
		
		this.taskTimeOutIndexLock = new ReentrantReadWriteLock(true);
		this.taskTimeOutIndexReadLock = this.taskTimeOutIndexLock.readLock();
		this.taskTimeOutIndexWriteLock = this.taskTimeOutIndexLock.writeLock();
		super.setDaemon(true);
		super.setName(DispatcherGuardian.class.getSimpleName() + " " + dispatcher.getId());
	}
	
	private volatile EventDispatcherImpl eventDispatcher = null;
	private volatile boolean go = true;
	private volatile boolean isUpdateNotified = false;
	private volatile Object waitMonitor = new Object();
	private volatile Map<QueueImpl,TaskObservable> taskTimeOutIndex = null;
	private volatile ReentrantReadWriteLock taskTimeOutIndexLock;
	private volatile ReadLock taskTimeOutIndexReadLock;
	private volatile WriteLock taskTimeOutIndexWriteLock;
	
	private volatile long currentWait = -1;
	
	@Override
	public void run()
	{
		long nextTimeOutTimeStamp = -1;
		List<QueueImpl> timeOutList = null;
		List<TaskObservable> removeTaskObservableList = null;
		boolean inTimeOut = false;
		
		while(go)
		{
			nextTimeOutTimeStamp = -1;
			if(timeOutList != null)
			{
				timeOutList.clear();
			}
			if(removeTaskObservableList != null)
			{
				removeTaskObservableList.clear();
			}
			
			taskTimeOutIndexReadLock.lock();
			
			try
			{
				long currentTimeStamp = System.currentTimeMillis();
				long heartBeatTimeOut = -1;
				long lastHeartBeat = -1;
				long heartBeatTimeOutStamp = -1;
				TaskContainer task = null;
				
				
				// Task TimeOut
				
				for(Entry<QueueImpl,TaskObservable> entry : this.taskTimeOutIndex.entrySet())
				{
					inTimeOut = false;
					task = entry.getKey().getCurrentRunningTask();
					
					if((task == null) || (task != entry.getValue().task))
					{
						if(removeTaskObservableList == null)
						{
							removeTaskObservableList = new ArrayList<TaskObservable>();
						}
						removeTaskObservableList.add(entry.getValue());
					}
					
					// Task Timeout
					
					Long taskTimeOut = entry.getValue().taskTimeOut;
					
					if((taskTimeOut != null) && (taskTimeOut.longValue() > 0))
					{
						if(taskTimeOut.longValue() <= currentTimeStamp)
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
								nextTimeOutTimeStamp = taskTimeOut.longValue();
							}
							else if(nextTimeOutTimeStamp > taskTimeOut.longValue())
							{
								nextTimeOutTimeStamp = taskTimeOut.longValue();
							}
						}
					}
					
					if(inTimeOut)
					{
						continue;
					}
					
					// HeartBeat Timeout
					
					heartBeatTimeOutStamp = -1;
					
					if(task !=  null)
					{
						heartBeatTimeOut = task.getTaskControl().getHeartBeatTimeOut();
						if(heartBeatTimeOut > 0)
						{
							try
							{
								lastHeartBeat = (Long)task.getMetrics().getQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
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
			
			taskTimeOutIndexReadLock.unlock();
			
			
			if(timeOutList != null)
			{
				for(QueueImpl queue : timeOutList)
				{
					queue.checkTimeOut();
				}
			}
			
			if((removeTaskObservableList != null) && (! removeTaskObservableList.isEmpty()))
			{
				taskTimeOutIndexWriteLock.lock();
				
				try
				{
					for(TaskObservable taskObservable : removeTaskObservableList)
					{
						TaskObservable toRemoveObservable = this.taskTimeOutIndex.get(taskObservable.queue);
						if(toRemoveObservable == null)
						{
							continue;
						}
						if(toRemoveObservable != taskObservable)
						{
							continue;
						}
						
						TaskContainer task = taskObservable.queue.getCurrentRunningTask();
						if((task == null) || (task != taskObservable.task))
						{
							this.taskTimeOutIndex.remove(taskObservable.queue);
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
			
				taskTimeOutIndexWriteLock.unlock();
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
	
	public void registerTimeOut(QueueImpl queue, TaskContainer task)
	{
		TaskControlImpl taskControl = task.getTaskControl();
		if(taskControl == null)
		{
			return;
		}
		
		long timeOutTimeStamp = taskControl.getTimeOut() + System.currentTimeMillis();
		
		taskTimeOutIndexWriteLock.lock();
		try
		{
			TaskObservable taskObservable = this.taskTimeOutIndex.get(queue);
			if(taskObservable ==  null)
			{
				taskObservable =  new TaskObservable();
				taskObservable.queue = queue;
				this.taskTimeOutIndex.put(queue,taskObservable);
			}
			if(taskControl.getTimeOut() > 0)
			{
				taskObservable.task = task;
				taskObservable.taskTimeOut = timeOutTimeStamp;
			}
			else if(taskControl.getHeartBeatTimeOut() > 0)
			{
				taskObservable.task = task;
			}
			else
			{
				return;
			}
		}
		finally 
		{
			taskTimeOutIndexWriteLock.unlock();
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
					long heartBeatTimeOut = task.getTaskControl().getHeartBeatTimeOut();
					if(heartBeatTimeOut > 0)
					{
						try
						{
							long lastHeartBeat = (Long)task.getMetrics().getQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
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
	
	public void unregisterTimeOut(QueueImpl queue, TaskContainer task)
	{
		taskTimeOutIndexWriteLock.lock();
		try
		{
			TaskObservable taskObservable = this.taskTimeOutIndex.get(queue);
			if(taskObservable !=  null)
			{
				if((taskObservable.task != null) && (taskObservable.task == task))
				{
					this.taskTimeOutIndex.remove(queue);
				}
			}
		}
		finally 
		{
			taskTimeOutIndexWriteLock.unlock();
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
	
	private class TaskObservable
	{
		public Long taskTimeOut = null;
		public TaskContainer task = null;
		public QueueImpl queue = null;
	}
	
}
