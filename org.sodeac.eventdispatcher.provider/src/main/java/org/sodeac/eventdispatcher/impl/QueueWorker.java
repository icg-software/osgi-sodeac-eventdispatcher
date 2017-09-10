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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IOnEventScheduled;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
import org.sodeac.eventdispatcher.api.IQueueJob;

public class QueueWorker extends Thread
{
	public static final long DEFAULT_WAIT_TIME = 108 * 108;
	public static final long DEFAULT_SHUTDOWN_TIME = 1080 * 54;
	
	private QueueImpl eventQueue = null;
	private volatile boolean go = true;
	private volatile boolean isUpdateNotified = false;
	private volatile Object waitMonitor = new Object();
	
	private List<JobContainer> dueJobList = null;
	private List<QueuedEventImpl> newScheduledList = null;
	private List<QueuedEventImpl> removedEventList = null;
	private List<Event> firedEventList = null;
	private List<String> signalList = null;
	
	private volatile Long currentTimeOutTimeStamp = null;
	private volatile JobContainer currentRunningJob = null;
	
	public QueueWorker(QueueImpl impl)
	{
		super();
		this.eventQueue = impl;
		this.dueJobList = new ArrayList<JobContainer>();
		this.newScheduledList = new ArrayList<QueuedEventImpl>();
		this.removedEventList = new ArrayList<QueuedEventImpl>();
		this.firedEventList = new ArrayList<Event>();
		this.signalList = new ArrayList<String>();
		
	}

	@Override
	public void run()
	{
		long lastAction = System.currentTimeMillis();
		while(go)
		{
			try
			{
				this.newScheduledList.clear();
				eventQueue.fetchNewScheduledList(this.newScheduledList);
				for(QueuedEventImpl event : this.newScheduledList)
				{
					lastAction = System.currentTimeMillis();
					for(ControllerContainer conf : eventQueue.getConfigurationList())
					{
						try
						{
							if(go)
							{
								if(conf.getEventController() instanceof IOnEventScheduled)
								{
									((IOnEventScheduled)conf.getEventController()).onEventScheduled(event);
								}
							}
						}
						catch (Exception e) {}
					}
				}
				this.newScheduledList.clear();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while process newScheduledList",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while process newScheduledList",e);
			}
			
			try
			{
				this.firedEventList.clear();
				eventQueue.fetchFiredEventList(this.firedEventList);
				for(Event event : this.firedEventList)
				{
					lastAction = System.currentTimeMillis();
					for(ControllerContainer conf : eventQueue.getConfigurationList())
					{
						try
						{
							if(go)
							{
								if(conf.getEventController() instanceof IOnFireEvent)
								{
									((IOnFireEvent)conf.getEventController()).onFireEvent(event,this.eventQueue);
								}
							}
						}
						catch (Exception e) {}
					}
				}
				this.firedEventList.clear();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while process firedEventList",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while process firedEventList",e);
			}
			
			try
			{
				this.removedEventList.clear();
				eventQueue.fetchRemovedEventList(this.removedEventList);
				for(QueuedEventImpl event : this.removedEventList)
				{
					lastAction = System.currentTimeMillis();
					for(ControllerContainer conf : eventQueue.getConfigurationList())
					{
						try
						{
							if(go)
							{
								if(conf.getEventController() instanceof IOnRemoveEvent)
								{
									((IOnRemoveEvent)conf.getEventController()).onRemoveEvent(event);
								}
							}
						}
						catch (Exception e) {}
					}
				}
				this.removedEventList.clear();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while process removedEventList",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while process removedEventList",e);
			}
			
			try
			{
				this.signalList.clear();
				eventQueue.fetchSignalList(this.signalList);
				for(String signal : this.signalList)
				{
					lastAction = System.currentTimeMillis();
					for(ControllerContainer conf : eventQueue.getConfigurationList())
					{
						try
						{
							if(go)
							{
								if(conf.getEventController() instanceof IOnQueueSignal)
								((IOnQueueSignal)conf.getEventController()).onQueueSignal(eventQueue, signal);
							}
						}
						catch (Exception e) {}
					}
				}
				this.signalList.clear();
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while process signalList",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while process signalList",e);
			}
			
			this.dueJobList.clear();
			long nextRunTimeStamp = eventQueue.getDueJobs(this.dueJobList);
			
			if(! dueJobList.isEmpty())
			{
				lastAction = System.currentTimeMillis();
				boolean jobTimeOut  = false;
				List<IQueueJob> currentProcessedJobList = null;
				for(JobContainer dueJob : this.dueJobList)
				{
					try
					{
						if(dueJob.getJobControl().isDone())
						{
							continue;
						}
						if(go)
						{
							try
							{
								jobTimeOut = ((dueJob.getJobControl().getTimeOut() > 0) || (dueJob.getJobControl().getHeartBeatTimeOut() > 0));
								this.currentRunningJob = dueJob;
								this.currentRunningJob.getMetrics().heartBeat();
								
								if(jobTimeOut)
								{
									if(dueJob.getJobControl().getTimeOut() > 0)
									{
										this.currentTimeOutTimeStamp = System.currentTimeMillis() + dueJob.getJobControl().getTimeOut();
									}
									this.eventQueue.getEventDispatcher().registerTimeOut(this.eventQueue,dueJob);
								}
								if(currentProcessedJobList == null)
								{
									currentProcessedJobList = new ArrayList<IQueueJob>();
									for(JobContainer jobContainer : this.dueJobList)
									{
										currentProcessedJobList.add(jobContainer.getJob());
									}
								}
								dueJob.getJobControl().preRun();
								dueJob.getJob().run(eventQueue, dueJob.getMetrics(), dueJob.getPropertyBlock(), dueJob.getJobControl() ,currentProcessedJobList);
								dueJob.getJobControl().postRun();
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
								}
							}
							catch (Exception e) 
							{
								log(LogService.LOG_ERROR,"Exception while process job " + dueJob,e);
								dueJob.getJobControl().preRun();
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
								}
								
								CountDownLatch countDownLatch = new CountDownLatch(1);
								
								try
								{
									new Thread()
									{
										public void run()
										{
											dueJob.getJobControl().setDone();
											for(ControllerContainer conf : eventQueue.getConfigurationList())
											{
												if(conf.getEventController() instanceof IOnJobError)
												{
													try
													{
														((IOnJobError)conf.getEventController()).onJobError(dueJob.getJob(),  e);
													}
													catch (Exception e) {}
												}
											}
											
											countDownLatch.countDown();
										}
									}.start();
								}
								catch (Exception ie) {}
								
								try
								{
									countDownLatch.await(13, TimeUnit.SECONDS);
								}
								catch (Exception ie) {}
							}
							catch (Error e) 
							{
								log(LogService.LOG_ERROR,"Error while process job " + dueJob,e);
								dueJob.getJobControl().preRun();
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
								}
								CountDownLatch countDownLatch = new CountDownLatch(1);
								
								try
								{
									new Thread()
									{
										public void run()
										{
											dueJob.getJobControl().setDone();
											for(ControllerContainer conf : eventQueue.getConfigurationList())
											{
												if(conf.getEventController() instanceof IOnJobError)
												{
													try
													{
														((IOnJobError)conf.getEventController()).onJobError(dueJob.getJob(),  new Exception(e));
													}
													catch (Exception e) {}
												}
											}
											
											countDownLatch.countDown();
										}
									}.start();
								}
								catch (Exception ie) {}
								
								try
								{
									countDownLatch.await(13, TimeUnit.SECONDS);
								}
								catch (Exception ie) {}
							}
							
							this.currentTimeOutTimeStamp = null;
							
							try
							{
								this.firedEventList.clear();
								eventQueue.fetchFiredEventList(this.firedEventList);
								for(Event event : this.firedEventList)
								{
									lastAction = System.currentTimeMillis();
									for(ControllerContainer conf : eventQueue.getConfigurationList())
									{
										try
										{
											if(go)
											{
												if(conf.getEventController() instanceof IOnFireEvent)
												{
													((IOnFireEvent)conf.getEventController()).onFireEvent(event,this.eventQueue);
												}
											}
										}
										catch (Exception e) {}
									}
								}
								this.firedEventList.clear();
							}
							catch (Exception e) 
							{
								log(LogService.LOG_ERROR,"Exception while process firedEventList",e);
							}
							catch (Error e) 
							{
								log(LogService.LOG_ERROR,"Error while process firedEventList",e);
							}
							
							try
							{
								this.removedEventList.clear();
								eventQueue.fetchRemovedEventList(this.removedEventList);
								for(QueuedEventImpl event : this.removedEventList)
								{
									lastAction = System.currentTimeMillis();
									for(ControllerContainer conf : eventQueue.getConfigurationList())
									{
										try
										{
											if(go)
											{
												if(conf.getEventController() instanceof IOnRemoveEvent)
												{
													((IOnRemoveEvent)conf.getEventController()).onRemoveEvent(event);
												}
											}
										}
										catch (Exception e) {}
									}
								}
								this.removedEventList.clear();
							}
							catch (Exception e) 
							{
								log(LogService.LOG_ERROR,"Exception while process removedEventList",e);
							}
							catch (Error e) 
							{
								log(LogService.LOG_ERROR,"Error while process removedEventList",e);
							}
							
							try
							{
								this.signalList.clear();
								eventQueue.fetchSignalList(this.signalList);
								for(String signal : this.signalList)
								{
									lastAction = System.currentTimeMillis();
									for(ControllerContainer conf : eventQueue.getConfigurationList())
									{
										try
										{
											if(go)
											{
												if(conf.getEventController() instanceof IOnQueueSignal)
												((IOnQueueSignal)conf.getEventController()).onQueueSignal(eventQueue, signal);
											}
										}
										catch (Exception e) {}
									}
								}
								this.signalList.clear();
							}
							catch (Exception e) 
							{
								log(LogService.LOG_ERROR,"Exception while process signalList",e);
							}
							catch (Error e) 
							{
								log(LogService.LOG_ERROR,"Error while process signalList",e);
							}
							
							for(ControllerContainer conf : eventQueue.getConfigurationList())
							{
								try
								{
									if(go)
									{
										if(conf.getEventController() instanceof IOnJobDone)
										{
											((IOnJobDone)conf.getEventController()).onJobDone(dueJob.getJob());
										}
									}
								}
								catch (Exception e) {}
							}
						}
					}
					catch (Exception e) 
					{
						try
						{
							dueJob.getJobControl().setDone();
						}
						catch (Exception ie) {}
						log(LogService.LOG_ERROR,"Error while process currentProcessedJobList",e);
					}
					
				}
			}
			
			eventQueue.cleanDoneJobs();
			
			try
			{
				this.dueJobList.clear();
				nextRunTimeStamp = eventQueue.getDueJobs(this.dueJobList);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Error while recalc next runtime again",e);
			}
			
			try
			{
				if(System.currentTimeMillis() > (lastAction + DEFAULT_SHUTDOWN_TIME))
				{
					this.eventQueue.checkWorkerShutdown(this);
					lastAction = System.currentTimeMillis();
				}
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
							long waitTime = nextRunTimeStamp - System.currentTimeMillis();
							if(waitTime > DEFAULT_WAIT_TIME)
							{
								waitTime = DEFAULT_WAIT_TIME;
							}
							if(waitTime > 0)
							{
								waitMonitor.wait(waitTime);
							}
						}
					}
				}
			}
			catch (InterruptedException e) {}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Exception while run QueueWorker",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while run QueueWorker",e);
			}
		}
		
	}
	
	public boolean checkTimeOut()
	{
		JobContainer timeOutJob = this.currentRunningJob;
		if(timeOutJob == null)
		{
			return false;
		}
		
		// HeartBeat TimeOut
		
		boolean heartBeatTimeout = false;
		if(timeOutJob.getJobControl().getHeartBeatTimeOut() > 0)
		{
			if(timeOutJob.getMetrics().getLastHeartBeat() > 0)
			{
				if((timeOutJob.getMetrics().getLastHeartBeat() + timeOutJob.getJobControl().getHeartBeatTimeOut() ) <= System.currentTimeMillis())
				{
					heartBeatTimeout = true;
				}
			}
		}
		
		if(! heartBeatTimeout)
		{
			// Job TimeOut
			
			Long timeOut = this.currentTimeOutTimeStamp;
			if(timeOut == null)
			{
				return false;
			}
			
			
			// check timeOut and timeOutJob again to prevent working with values don't match
			
			if(timeOutJob != this.currentRunningJob)
			{
				return false;
			}
			
			if(timeOut != this.currentTimeOutTimeStamp)
			{
				return false;
			}
			
			if(timeOut.longValue() > System.currentTimeMillis())
			{
				return false;
			}
		}
		
		this.stopWorker();
		
		try
		{
			timeOutJob.getJobControl().timeOut();
		}
		catch (Exception e) {}
		
		for(ControllerContainer conf : eventQueue.getConfigurationList())
		{
			try
			{
				if(conf.getEventController() instanceof IOnJobTimeout)
				{
					new Thread()
					{
						public void run()
						{
							((IOnJobTimeout)conf.getEventController()).onJobTimeout(timeOutJob.getJob());
						}
					}.start();
				}
			}
			catch (Exception e) {}
		}
		
		if(timeOutJob.getJobControl().stopOnTimeOut())
		{
			if(Thread.currentThread() != this)
			{
				new Thread()
				{
					@SuppressWarnings("deprecation")
					public void run()
					{
						if(QueueWorker.this.isAlive())
						{
							try
							{
								Thread.sleep(1080); // TODO give chance API
							}
							catch (Exception e) {}
							catch (Error e) {}
						}
						
						if(QueueWorker.this.isAlive())
						{
							try
							{
								synchronized (QueueWorker.this.waitMonitor)
								{
									QueueWorker.this.stop();
								}
							}
							catch (Exception e) {}
							catch (Error e) {}
						}
					}
				}.start();
			}
		}
		return true;
	}
	
	public void notifyUpdate()
	{
		try
		{
			synchronized (this.waitMonitor)
			{
				this.isUpdateNotified = true;
				waitMonitor.notify();
			}	
		}
		catch (Exception e) {}
		catch (Error e) {}
	}
	
	public void stopWorker()
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
				log(LogService.LOG_ERROR,"Exception while stop QueueWorker",e);
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while stop QueueWorker",e);
			}
		}
	}
	
	private void log(int logServiceLevel,String logMessage, Exception e)
	{
		try
		{
			EventDispatcherImpl dispatcher = null;
			LogService logService = null;
			ComponentContext context = null;
			
			if(eventQueue != null){dispatcher = eventQueue.getEventDispatcher();}
			if(dispatcher != null){logService = dispatcher.logService;}
			if(dispatcher != null){context = dispatcher.getContext();}
			
			if(logService != null)
			{
				logService.log(context == null ? null : context.getServiceReference(), logServiceLevel, logMessage, e);
			}
			else
			{
				if(logServiceLevel == LogService.LOG_ERROR)
				{
					System.err.println(logMessage);
				}
				if(e != null)
				{
					e.printStackTrace();
				}
			}
		}
		catch (Exception ie) 
		{
			ie.printStackTrace();
		}
	}
	
	private void log(int logServiceLevel,String logMessage, Error e)
	{
		try
		{
			EventDispatcherImpl dispatcher = null;
			LogService logService = null;
			ComponentContext context = null;
			
			if(eventQueue != null){dispatcher = eventQueue.getEventDispatcher();}
			if(dispatcher != null){logService = dispatcher.logService;}
			if(dispatcher != null){context = dispatcher.getContext();}
			
			if(logService != null)
			{
				logService.log(context == null ? null : context.getServiceReference(), logServiceLevel, logMessage, e);
			}
			else
			{
				if(logServiceLevel == LogService.LOG_ERROR)
				{
					System.err.println(logMessage);
				}
				if(e != null)
				{
					e.printStackTrace();
				}
			}
		}
		catch (Exception ie) 
		{
			ie.printStackTrace();
		}
	}

	public JobContainer getCurrentRunningJob()
	{
		return currentRunningJob;
	}
	
}
