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

import org.osgi.service.event.Event;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IPeriodicQueueJob;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnEventScheduled;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.ITimer;

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
	private long wakeUpTimeStamp = -1;
	
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
			this.wakeUpTimeStamp = -2; // rescheduling from now results in skipping next sleep
			try
			{
				synchronized (this.waitMonitor)
				{
					if((this.wakeUpTimeStamp > 0) || (this.wakeUpTimeStamp == -3))
					{
						this.wakeUpTimeStamp = -2;
					}
				}
			}
			catch (Exception e) {} 
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
							ITimer.Context timerContextJob = null;
							ITimer.Context timerContextQueue = null;
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
								if(dueJob.getJob() instanceof IPeriodicQueueJob)
								{
									Long periodicRepetitionInterval = ((IPeriodicQueueJob) dueJob.getJob()).getPeriodicRepetitionInterval();
									if((periodicRepetitionInterval ==  null) || (periodicRepetitionInterval.longValue() < 1))
									{
										periodicRepetitionInterval = 1000L * 60L * 60L * 24L * 365L * 108L;
									}
									dueJob.getJobControl().setExecutionTimeStamp(System.currentTimeMillis() + periodicRepetitionInterval);
									dueJob.getJobControl().preRunPeriodicJob();
								}
								else if(dueJob.getJob() instanceof IQueueService)
								{
									long periodicRepetitionInterval = -1L;
									
									try
									{
										if(dueJob.getPropertyBlock().getProperty(IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL) != null)
										{
											Object pri = dueJob.getPropertyBlock().getProperty(IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL);
											if(pri instanceof String)
											{
												periodicRepetitionInterval = Long.parseLong((String)pri);
											}
											else if(pri instanceof Integer)
											{
												periodicRepetitionInterval = ((Integer)pri);
											}
											else
											{
												periodicRepetitionInterval = ((Long)pri);
											}
										}		
									}
									catch (Exception e) {}
									
									if(periodicRepetitionInterval < 1)
									{
										periodicRepetitionInterval = 1000L * 60L * 60L * 24L * 365L * 108L;
									}
									dueJob.getJobControl().setExecutionTimeStamp(System.currentTimeMillis() + periodicRepetitionInterval);
									dueJob.getJobControl().preRunPeriodicJob();
								}
								else
								{
									dueJob.getJobControl().preRun();
								}
								
								this.currentRunningJob.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_STARTED_TIMESTAMP, System.currentTimeMillis());
								
								dueJob.getMetrics().meter(IMetrics.METRICS_RUN_JOB).mark();
								dueJob.getMetrics().counter(IMetrics.METRICS_RUN_JOB).inc();
								eventQueue.getMetrics().meter(IMetrics.METRICS_RUN_JOB).mark();
								eventQueue.getMetrics().counter(IMetrics.METRICS_RUN_JOB).inc();
								timerContextJob = dueJob.getMetrics().timer(IMetrics.METRICS_RUN_JOB).time();
								timerContextQueue = eventQueue.getMetrics().timer(IMetrics.METRICS_RUN_JOB).time();
								
								dueJob.getJob().run(eventQueue, dueJob.getMetrics(), dueJob.getPropertyBlock(), dueJob.getJobControl() ,currentProcessedJobList);
								
								this.currentRunningJob.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_FINISHED_TIMESTAMP, System.currentTimeMillis());
								this.currentRunningJob.getPropertyBlock().setProperty(IQueueJob.PROPERTY_KEY_THROWED_EXCEPTION, null);
								
								timerContextJob.stop();
								timerContextQueue.stop();
								
								dueJob.getJobControl().postRun();
								
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								if(jobTimeOut)
								{
									try
									{
										this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
									}
									catch (Exception e) 
									{
										this.log(LogService.LOG_ERROR, "eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob)", e);
									}
								}
							}
							catch (Exception e) 
							{
								this.currentRunningJob.getPropertyBlock().setProperty(IQueueJob.PROPERTY_KEY_THROWED_EXCEPTION, e);
								log(LogService.LOG_ERROR,"Exception while process job " + dueJob,e);
								if(timerContextJob != null)
								{
									try
									{
										timerContextJob.stop();
									}
									catch (Exception e2) {}
								}
								if(timerContextQueue != null)
								{
									try
									{
										timerContextQueue.stop();
									}
									catch (Exception e2) {}
								}
								
								try
								{
									dueJob.getMetrics().counter(IMetrics.METRICS_RUN_JOB_ERROR).inc();
									eventQueue.getMetrics().counter(IMetrics.METRICS_RUN_JOB_ERROR).inc();
								}
								catch (Exception e2) {}
								
								dueJob.getJobControl().postRun();
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
								}
								
								CountDownLatch countDownLatch = new CountDownLatch(1);
								
								try
								{
									if(! (dueJob.getJob() instanceof IQueueService))
									{
										dueJob.getJobControl().setDone();
									}
									new Thread()
									{
										public void run()
										{
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
								Exception exc = new Exception(e.getMessage(),e);
								this.currentRunningJob.getPropertyBlock().setProperty(IQueueJob.PROPERTY_KEY_THROWED_EXCEPTION, exc);
								log(LogService.LOG_ERROR,"Error while process job " + dueJob,e);
								if(timerContextJob != null)
								{
									try
									{
										timerContextJob.stop();
									}
									catch (Exception e2) {}
								}
								if(timerContextQueue != null)
								{
									try
									{
										timerContextQueue.stop();
									}
									catch (Exception e2) {}
								}
								
								try
								{
									dueJob.getMetrics().counter(IMetrics.METRICS_RUN_JOB_ERROR).inc();
									eventQueue.getMetrics().counter(IMetrics.METRICS_RUN_JOB_ERROR).inc();
								}
								catch (Exception e2) {}
								
								dueJob.getJobControl().postRun();
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
								}
								CountDownLatch countDownLatch = new CountDownLatch(1);
								
								try
								{
									if(! (dueJob.getJob() instanceof IQueueService))
									{
										dueJob.getJobControl().setDone();
									}
									new Thread()
									{
										public void run()
										{
											for(ControllerContainer conf : eventQueue.getConfigurationList())
											{
												if(conf.getEventController() instanceof IOnJobError)
												{
													try
													{
														((IOnJobError)conf.getEventController()).onJobError(dueJob.getJob(),  new Exception(exc));
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
							
							if(dueJob.getJobControl().isDone())
							{
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
					}
					catch (Exception e) 
					{
						try
						{
							if(! (dueJob.getJob() instanceof IQueueService))
							{
								dueJob.getJobControl().setDone();
							}
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
						if((this.isUpdateNotified) || (this.wakeUpTimeStamp == -3))
						{
							this.wakeUpTimeStamp = -1;
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
								this.wakeUpTimeStamp = System.currentTimeMillis() + waitTime;
								waitMonitor.wait(waitTime);
							}
						}
					}
					this.isUpdateNotified = false;
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
			this.wakeUpTimeStamp = -1;
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
			if(timeOutJob.getJob() instanceof IQueueService)
			{
				timeOutJob.getJobControl().timeOutService();
			}
			else
			{
				timeOutJob.getJobControl().timeOut();
			}
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
	
	public void notifyUpdate(long newRuntimeStamp)
	{
		try
		{
			synchronized (this.waitMonitor)
			{
				this.isUpdateNotified = true;
				if(this.wakeUpTimeStamp == -2)
				{
					this.wakeUpTimeStamp = -3;
				}
				else if(this.wakeUpTimeStamp > 0)
				{
					if(this.wakeUpTimeStamp > newRuntimeStamp)
					{
						waitMonitor.notify();
					}
				}
			}	
		}
		catch (Exception e) {}
		catch (Error e) {}
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
		if(eventQueue != null)
		{
			eventQueue.log(logServiceLevel, logMessage, e);
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
	
	private void log(int logServiceLevel,String logMessage, Error e)
	{
		if(eventQueue != null)
		{
			eventQueue.log(logServiceLevel, logMessage, e);
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

	public JobContainer getCurrentRunningJob()
	{
		return currentRunningJob;
	}
	
}
