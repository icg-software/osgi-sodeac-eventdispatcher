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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.event.Event;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IPeriodicQueueJob;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnScheduleEventList;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueueWorker;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IScheduleResult;
import org.sodeac.eventdispatcher.api.ITimer;

public class QueueWorker extends Thread
{
	public static final long DEFAULT_WAIT_TIME = 108 * 108 * 108 * 7;
	public static final long FREE_TIME = 108 + 27;
	public static final long RESCHEDULE_BUFFER_TIME = 27;
	public static final long DEFAULT_SHUTDOWN_TIME = 1080 * 54;
	
	private long spoolTimeStamp = 0;
	
	private QueueImpl eventQueue = null;
	private IQueueWorker workerWrapper = null;
	private volatile boolean go = true;
	protected volatile boolean isUpdateNotified = false;
	protected volatile boolean isSoftUpdated = false;
	private volatile Object waitMonitor = new Object();
	
	private List<JobContainer> dueJobList = null;
	//private List<QueuedEventImpl> newScheduledList = null;
	//private List<QueuedEventImpl> removedEventList = null;
	private List<Event> firedEventList = null;
	private List<String> signalList = null;
	private List<IOnQueueObserve> onQueueObserveList = null;
	
	private volatile Long currentTimeOutTimeStamp = null;
	private volatile JobContainer currentRunningJob = null;
	private volatile long wakeUpTimeStamp = -1;
	private volatile boolean inFreeingArea = false;
	
	public QueueWorker(QueueImpl impl)
	{
		super();
		this.eventQueue = impl;
		this.workerWrapper = new QueueWorkerWrapper(this);
		this.dueJobList = new ArrayList<JobContainer>();
		//this.newScheduledList = new ArrayList<QueuedEventImpl>();
		//this.removedEventList = new ArrayList<QueuedEventImpl>();
		this.firedEventList = new ArrayList<Event>();
		this.signalList = new ArrayList<String>();
		this.onQueueObserveList = new ArrayList<IOnQueueObserve>();
		super.setDaemon(true);
		super.setName(QueueWorker.class.getSimpleName() + " " + this.eventQueue.getId());
	}

	private void checkQueueObserve()
	{
		try
		{
			if(! this.onQueueObserveList.isEmpty())
			{
				this.onQueueObserveList.clear();
			}
			eventQueue.fetchOnQueueObserveList(this.onQueueObserveList);
			if(this.onQueueObserveList.isEmpty())
			{
				return;
			}
			for(IOnQueueObserve onQueueObserve : this.onQueueObserveList)
			{
				try
				{
					onQueueObserve.onQueueObserve(this.eventQueue);
				}
				catch (Exception e) 
				{
					this.onQueueObserveList.clear();
					log(LogService.LOG_ERROR,"Exception on on-create() event controller",e);
				}
				catch (Error e) 
				{
					this.onQueueObserveList.clear();
					log(LogService.LOG_ERROR,"Exception on on-create() event controller",e);
				}
			}
			this.onQueueObserveList.clear();
		}
		catch (Exception e) 
		{
			this.onQueueObserveList.clear();
			log(LogService.LOG_ERROR,"Exception while check queueObserve",e);
		}
		catch (Error e) 
		{
			this.onQueueObserveList.clear();
			log(LogService.LOG_ERROR,"Exception while check queueObserve",e);
		}
	}
	@Override
	public void run()
	{
		//List<IQueuedEvent> processScheduleList = Collections.unmodifiableList(this.newScheduledList);
		Map<IScheduleResult,IScheduleResult> scheduledResultIndex = new HashMap<IScheduleResult,IScheduleResult>();
		org.sodeac.multichainlist.Snapshot<? extends IQueuedEvent> newEventsSnapshot;
		org.sodeac.multichainlist.Snapshot<? extends IQueuedEvent> removedEventsSnapshot;
		while(go)
		{
			
			try
			{
				checkQueueObserve();
			}
			catch(Exception ex) {}
			catch(Error ex) {}
			
			try
			{
				synchronized (this.waitMonitor)
				{
					this.isUpdateNotified = false;
					this.isSoftUpdated = false;
				}
				
				//this.newScheduledList.clear();
				//eventQueue.fetchNewScheduledList(this.newScheduledList);
				newEventsSnapshot = eventQueue.getNewScheduledEventsSnaphot();
				try
				{
					if((newEventsSnapshot != null) && (! newEventsSnapshot.isEmpty()))
					{
						try
						{
							checkQueueObserve();
						}
						catch(Exception ex) {}
						catch(Error ex) {}
						
						boolean singleProcess = false;
						boolean listProcess = false;
							
						scheduledResultIndex.clear();
						for(IQueuedEvent event : newEventsSnapshot)
						{
							try
							{
								scheduledResultIndex.put(event.getScheduleResultObject(), event.getScheduleResultObject());
							}
							catch (Exception ie) {}
						}
							
						for(ControllerContainer conf : eventQueue.getConfigurationList())
						{
							if(conf.isImplementingIOnScheduleEvent())
							{
								singleProcess = true;
							}
							if(conf.isImplementingIOnScheduleEventList())
							{
								listProcess = true;
							}
						}
						if(listProcess)
						{
								
							eventQueue.touchLastWorkerAction();
							for(ControllerContainer conf : eventQueue.getConfigurationList())
							{
								try
								{
									if(go)
									{
										if(conf.isImplementingIOnScheduleEventList())
										{
											((IOnScheduleEventList)conf.getQueueController()).onScheduleEventList(this.eventQueue, (org.sodeac.multichainlist.Snapshot<IQueuedEvent>)newEventsSnapshot);
										}
									}
								}
								catch (Exception e) 
								{
									for(IScheduleResult scheduleResult : scheduledResultIndex.keySet())
									{
										try
										{
											scheduleResult.addError(e);
										}
										catch (Exception ie) {}										
									}
								}
							}
							
						}
						if(singleProcess)
						{
							for(QueuedEventImpl event : (org.sodeac.multichainlist.Snapshot<QueuedEventImpl>)newEventsSnapshot)
							{
								eventQueue.touchLastWorkerAction();
								for(ControllerContainer conf : eventQueue.getConfigurationList())
								{
									try
									{
										if(go)
										{
											if(conf.isImplementingIOnScheduleEvent())
											{
												((IOnScheduleEvent)conf.getQueueController()).onScheduleEvent(event);
											}
										}
									}
									catch (Exception e) 
									{
										try
										{
											event.getScheduleResultObject().addError(e);
										}
										catch (Exception ie) {}		
									}
								}
							}
						}
						for(IScheduleResult scheduleResult : scheduledResultIndex.keySet())
						{
							try
							{
								((ScheduleResultImpl)scheduleResult).schedulePhaseIsFinished();
							}
							catch (Exception ie) {}										
						}
						for(QueuedEventImpl event : (org.sodeac.multichainlist.Snapshot<QueuedEventImpl>)newEventsSnapshot)
						{
							try
							{
								event.setScheduleResultObject(null);
							}
							catch(Exception e) {}
						}
					}
				}
				finally
				{
					if(newEventsSnapshot != null)
					{
						try
						{
							newEventsSnapshot.close();
						}
						finally 
						{
							newEventsSnapshot = null;
						}
						scheduledResultIndex.clear();
					}
				}
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
				if(! this.firedEventList.isEmpty())
				{

					try
					{
						checkQueueObserve();
					}
					catch(Exception ex) {}
					catch(Error ex) {}
					
					for(Event event : this.firedEventList)
					{
						eventQueue.touchLastWorkerAction();
						for(ControllerContainer conf : eventQueue.getConfigurationList())
						{
							try
							{
								if(go)
								{
									if(conf.isImplementingIOnFireEvent())
									{
										((IOnFireEvent)conf.getQueueController()).onFireEvent(event,this.eventQueue);
									}
								}
							}
							catch (Exception e) {}
						}
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
				//this.removedEventList.clear();
				//eventQueue.fetchRemovedEventList(this.removedEventList);
				removedEventsSnapshot = eventQueue.getRemovedEventsSnapshot();
				try
				{
					if((removedEventsSnapshot != null) && (! removedEventsSnapshot.isEmpty()))
					{
	
						try
						{
							checkQueueObserve();
						}
						catch(Exception ex) {}
						catch(Error ex) {}
						
						for(QueuedEventImpl event : (org.sodeac.multichainlist.Snapshot<QueuedEventImpl>)removedEventsSnapshot)
						{
							eventQueue.touchLastWorkerAction();
							for(ControllerContainer conf : eventQueue.getConfigurationList())
							{
								try
								{
									if(go)
									{
										if(conf.isImplementingIOnRemoveEvent())
										{
											((IOnRemoveEvent)conf.getQueueController()).onRemoveEvent(event);
										}
									}
								}
								catch (Exception e) {}
							}
						}
					}
				}
				finally 
				{
					if(removedEventsSnapshot != null)
					{
						try
						{
							removedEventsSnapshot.close();
						}
						finally 
						{
							removedEventsSnapshot = null;
						}
					}
				}
				//this.removedEventList.clear();
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
				if(! this.signalList.isEmpty())
				{

					try
					{
						checkQueueObserve();
					}
					catch(Exception ex) {}
					catch(Error ex) {}
					
					for(String signal : this.signalList)
					{
						eventQueue.touchLastWorkerAction();
						for(ControllerContainer conf : eventQueue.getConfigurationList())
						{
							try
							{
								if(go)
								{
									if(conf.isImplementingIOnQueueSignal())
									((IOnQueueSignal)conf.getQueueController()).onQueueSignal(eventQueue, signal);
								}
							}
							catch (Exception e) {}
						}
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
			eventQueue.getDueJobs(this.dueJobList);
			
			if(! dueJobList.isEmpty())
			{

				try
				{
					checkQueueObserve();
				}
				catch(Exception ex) {}
				catch(Error ex) {}
				
				eventQueue.touchLastWorkerAction();
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
								dueJob.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
								
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
									dueJob.getJobControl().setExecutionTimeStampPeriodic(System.currentTimeMillis() + periodicRepetitionInterval);
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
												periodicRepetitionInterval = Long.parseLong(((String)pri).trim());
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
									dueJob.getJobControl().setExecutionTimeStampPeriodic(System.currentTimeMillis() + periodicRepetitionInterval);
									dueJob.getJobControl().preRunPeriodicJob();
								}
								else
								{
									dueJob.getJobControl().preRun();
								}
								
								dueJob.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_STARTED_TIMESTAMP, System.currentTimeMillis());
								
								if(dueJob.isNamedJob())
								{
									timerContextJob = dueJob.getMetrics().timer(IMetrics.METRICS_RUN_JOB).time();
								}
								
								timerContextQueue = eventQueue.getMetrics().timer(IMetrics.METRICS_RUN_JOB).time();
								
								dueJob.getJob().run(eventQueue, dueJob.getMetrics(), dueJob.getPropertyBlock(), dueJob.getJobControl() ,currentProcessedJobList);
								
								dueJob.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_FINISHED_TIMESTAMP, System.currentTimeMillis());
								dueJob.getPropertyBlock().setProperty(IQueueJob.PROPERTY_KEY_THROWED_EXCEPTION, null);
								
								if(timerContextJob != null)
								{
									timerContextJob.stop();
								}
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
								if(! go)
								{
									return;
								}
							}
							catch (Exception e) 
							{
								JobContainer runningJob = this.currentRunningJob;
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								
								runningJob.getPropertyBlock().setProperty(IQueueJob.PROPERTY_KEY_THROWED_EXCEPTION, e);
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
									if(dueJob.isNamedJob())
									{
										dueJob.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
									}
									eventQueue.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
								}
								catch (Exception e2) {}
								
								dueJob.getJobControl().postRun();
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
								}
								
								if(! (dueJob.getJob() instanceof IQueueService))
								{
									dueJob.getJobControl().setDone();
								}
								
								if(! go)
								{
									return;
								}
								
								try
								{
									for(ControllerContainer conf : eventQueue.getConfigurationList())
									{
										if(conf.isImplementingIOnJobError())
										{
											try
											{
												((IOnJobError)conf.getQueueController()).onJobError(dueJob.getJob(),  e);
											}
											catch (Exception ie) 
											{
												log(LogService.LOG_ERROR,"Error while process onJobError " + dueJob,ie);
											}
										}
									}
								}
								catch (Exception ie) 
								{
									log(LogService.LOG_ERROR,"Error while process onJobError " + dueJob,ie);
								}
				
							}
							catch (Error e) 
							{
								JobContainer runningJob = this.currentRunningJob;
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								
								Exception exc = new Exception(e.getMessage(),e);
								runningJob.getPropertyBlock().setProperty(IQueueJob.PROPERTY_KEY_THROWED_EXCEPTION, exc);
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
									if(dueJob.isNamedJob())
									{
										dueJob.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
									}
									eventQueue.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
								}
								catch (Exception e2) {}
								
								dueJob.getJobControl().postRun();
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob);
								}
								
								if(! (dueJob.getJob() instanceof IQueueService))
								{
									dueJob.getJobControl().setDone();
								}
								
								if(e instanceof ThreadDeath)
								{
									go = false;
								}
								
								if(! go)
								{
									return;
								}
								
								try
								{
									for(ControllerContainer conf : eventQueue.getConfigurationList())
									{
										if(conf.isImplementingIOnJobError())
										{
											try
											{
												((IOnJobError)conf.getQueueController()).onJobError(dueJob.getJob(), exc);
											}
											catch (Exception ie) 
											{
												log(LogService.LOG_ERROR,"Error while process onJobError " + dueJob,ie);
											}
										}
									}
								}
								catch (Exception ie) 
								{
									log(LogService.LOG_ERROR,"Error while process onJobError " + dueJob,ie);
								}
								
							}
							
							this.currentTimeOutTimeStamp = null;
							this.currentRunningJob = null;
							
							if(! go)
							{
								return;
							}
							
							try
							{
								this.firedEventList.clear();
								eventQueue.fetchFiredEventList(this.firedEventList);
								for(Event event : this.firedEventList)
								{
									eventQueue.touchLastWorkerAction();
									for(ControllerContainer conf : eventQueue.getConfigurationList())
									{
										try
										{
											if(go)
											{
												if(conf.isImplementingIOnFireEvent())
												{
													((IOnFireEvent)conf.getQueueController()).onFireEvent(event,this.eventQueue);
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
								// TODO remove this ??
								removedEventsSnapshot = eventQueue.getRemovedEventsSnapshot();
								try
								{
									if((removedEventsSnapshot != null) && (! removedEventsSnapshot.isEmpty()))
									{
					
										try
										{
											checkQueueObserve();
										}
										catch(Exception ex) {}
										catch(Error ex) {}
										
										for(QueuedEventImpl event : (org.sodeac.multichainlist.Snapshot<QueuedEventImpl>)removedEventsSnapshot)
										{
											eventQueue.touchLastWorkerAction();
											for(ControllerContainer conf : eventQueue.getConfigurationList())
											{
												try
												{
													if(go)
													{
														if(conf.isImplementingIOnRemoveEvent())
														{
															((IOnRemoveEvent)conf.getQueueController()).onRemoveEvent(event);
														}
													}
												}
												catch (Exception e) {}
											}
										}
									}
								}
								finally 
								{
									if(removedEventsSnapshot != null)
									{
										try
										{
											removedEventsSnapshot.close();
										}
										finally 
										{
											removedEventsSnapshot = null;
										}
									}
								}
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
								if(! this.signalList.isEmpty())
								{

									try
									{
										checkQueueObserve();
									}
									catch(Exception ex) {}
									catch(Error ex) {}
									
									for(String signal : this.signalList)
									{
										eventQueue.touchLastWorkerAction();
										for(ControllerContainer conf : eventQueue.getConfigurationList())
										{
											try
											{
												if(go)
												{
													if(conf.isImplementingIOnQueueSignal())
													{
														((IOnQueueSignal)conf.getQueueController()).onQueueSignal(eventQueue, signal);
													}
												}
											}
											catch (Exception e) {}
										}
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
											if(conf.isImplementingIOnJobDone())
											{
												((IOnJobDone)conf.getQueueController()).onJobDone(dueJob.getJob());
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
				boolean shutdownWorker = false;
				if(System.currentTimeMillis() > (eventQueue.getLastWorkerAction() + DEFAULT_SHUTDOWN_TIME))
				{
					this.inFreeingArea = true;
					shutdownWorker = this.eventQueue.checkWorkerShutdown(this);
					if(! shutdownWorker)
					{
						this.inFreeingArea = false;
					}
				}
				
				if(shutdownWorker)
				{
					synchronized (this.waitMonitor)
					{
						while((this.eventQueue == null) && (this.go))
						{
							try
							{
								this.wakeUpTimeStamp = System.currentTimeMillis() + DEFAULT_WAIT_TIME;
								waitMonitor.wait(DEFAULT_WAIT_TIME);
								this.wakeUpTimeStamp = -1;
							}
							catch (Exception e) {}
							catch (ThreadDeath e) {this.go = false;}
							catch (Error e) {}
						}
						
						this.inFreeingArea = false;
						
						if(! go)
						{
							return;
						}
						
						eventQueue.touchLastWorkerAction();
						
						continue;
					}
				}
				
				try
				{
					checkQueueObserve();
				}
				catch(Exception ex) {}
				catch(Error ex) {}
				
				synchronized (this.waitMonitor)
				{
					if(go)
					{
						this.wakeUpTimeStamp = -1;
						
						if(this.isUpdateNotified)
						{
							this.isUpdateNotified = false;
							continue;
						}
							
						long nextRunTimeStamp = System.currentTimeMillis() + DEFAULT_WAIT_TIME;
						try
						{
							nextRunTimeStamp = eventQueue.getNextRun();
						}
						catch (Exception e) 
						{
							log(LogService.LOG_ERROR,"Error while recalc next runtime again",e);
						}
						long waitTime = nextRunTimeStamp - System.currentTimeMillis();
						if(waitTime > DEFAULT_WAIT_TIME)
						{
							waitTime = DEFAULT_WAIT_TIME;
						}
						if(waitTime > 0)
						{
							boolean freeWorker = false;
							if(! isSoftUpdated)
							{
								this.inFreeingArea = true;
								if(waitTime >= FREE_TIME)
								{
									freeWorker = this.eventQueue.checkFreeWorker(this, nextRunTimeStamp);
								}
							}
							if(freeWorker)
							{
								while((this.eventQueue == null) && (this.go))
								{
									try
									{
										this.wakeUpTimeStamp = System.currentTimeMillis() + DEFAULT_WAIT_TIME ;
										waitMonitor.wait(DEFAULT_WAIT_TIME);
										this.wakeUpTimeStamp = -1;
									}
									catch (Exception e) {}
									catch (ThreadDeath e) {this.go = false;}
									catch (Error e) {}
								}
								
								this.inFreeingArea = false;
								
							}
							else
							{
								this.inFreeingArea = false;
								this.wakeUpTimeStamp = System.currentTimeMillis() + waitTime ;
								waitMonitor.wait(waitTime);
								this.wakeUpTimeStamp = -1;
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
			catch (ThreadDeath e) 
			{
				this.go = false;
			}
			catch (Error e) 
			{
				log(LogService.LOG_ERROR,"Error while run QueueWorker",e);
			}
		}		
	}
	
	public boolean checkTimeOut(AtomicBoolean stop)
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
			try
			{
				long lastHeartBeat = (Long)timeOutJob.getMetrics().getQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
				if(lastHeartBeat > 0)
				{
					if((lastHeartBeat + timeOutJob.getJobControl().getHeartBeatTimeOut() ) <= System.currentTimeMillis())
					{
						heartBeatTimeout = true;
					}
				}
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"Error while check heartbeat timeout",e);
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
		
		this.go = false;
		
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
				if(conf.getQueueController() instanceof IOnJobTimeout)
				{
					((EventDispatcherImpl)eventQueue.getDispatcher()).executeOnJobTimeOut((IOnJobTimeout)conf.getQueueController(), timeOutJob.getJob());
				}
			}
			catch (Exception e) {}
		}
		
		if(timeOutJob.getJobControl().getStopOnTimeOutFlag())
		{
			if(Thread.currentThread() != this)
			{
				try
				{
					stop.set(true);
					((EventDispatcherImpl)eventQueue.getDispatcher()).executeOnJobStopExecuter(this, timeOutJob.getJob());
				}
				catch (Exception e) {}
				
			}
			else
			{
				log(LogService.LOG_WARNING, "worker not stopped: checkTimeout invoke by self", null);
			}
		}
		
		return true;
	}
	
	public void notifySoftUpdate()
	{
		this.isUpdateNotified = true;
		this.isSoftUpdated = true;
	}
	
	public void notifyUpdate(long newRuntimeStamp)
	{
		try
		{
			synchronized (this.waitMonitor)
			{
				this.isUpdateNotified = true;
				this.isSoftUpdated = false;
				if(this.wakeUpTimeStamp > 0) // waits for new run
				{
					if(newRuntimeStamp <= System.currentTimeMillis())
					{
						waitMonitor.notify();
					}
					else if(this.wakeUpTimeStamp >= newRuntimeStamp)
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
				this.isSoftUpdated = false;
				
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
	
	public QueueImpl getEventQueue()
	{
		return eventQueue;
	}

	protected boolean setEventQueue(QueueImpl eventQueue)
	{
		if(! this.go)
		{
			return false;
		}
		
		if((eventQueue != null) && (this.eventQueue != null))
		{
			return false;
		}
		
		if(! inFreeingArea)
		{
			return false;
		}
		
		this.eventQueue = eventQueue;
		if(this.eventQueue == null)
		{
			super.setName(QueueWorker.class.getSimpleName() + " IDLE");
		}
		else
		{
			super.setName(QueueWorker.class.getSimpleName() + " " + this.eventQueue.getId());
		}
		
		return true;
	}

	private void log(int logServiceLevel,String logMessage, Throwable e)
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

	public boolean isGo()
	{
		return go;
	}

	public long getSpoolTimeStamp()
	{
		return spoolTimeStamp;
	}

	public void setSpoolTimeStamp(long spoolTimeStamp)
	{
		this.spoolTimeStamp = spoolTimeStamp;
	}

	public IQueueWorker getWorkerWrapper()
	{
		return workerWrapper;
	}
}
