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
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.event.Event;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IPeriodicQueueJob;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnQueuedEventList;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueueWorker;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IQueueEventResult;
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
	
	private List<TaskContainer> dueJobList = null;
	private List<String> signalList = null;
	private List<IOnQueueObserve> onQueueObserveList = null;
	
	private volatile Long currentTimeOutTimeStamp = null;
	private volatile TaskContainer currentRunningJob = null;
	private volatile long wakeUpTimeStamp = -1;
	private volatile boolean inFreeingArea = false;
	
	public QueueWorker(QueueImpl impl)
	{
		super();
		this.eventQueue = impl;
		this.workerWrapper = new QueueWorkerWrapper(this);
		this.dueJobList = new ArrayList<TaskContainer>();
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
			eventQueue.fetchOnQueueObserveList(this.onQueueObserveList); // TODO mcl
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
					log(LogService.LOG_ERROR,"Exception on on-create() event controller",e);
				}
				catch (Error e) 
				{
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
	@SuppressWarnings("unchecked")
	@Override
	public void run()
	{
		Map<IQueueEventResult,IQueueEventResult> scheduledResultIndex = new HashMap<IQueueEventResult,IQueueEventResult>();
		org.sodeac.multichainlist.Snapshot<? extends IQueuedEvent> newEventsSnapshot;
		org.sodeac.multichainlist.Snapshot<? extends IQueuedEvent> removedEventsSnapshot;
		org.sodeac.multichainlist.Snapshot<Event> firedEventsSnapshot;
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
				
				eventQueue.closeWorkerSnapshots();
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
											((IOnQueuedEventList)conf.getQueueController()).onQueuedEventList(this.eventQueue, (org.sodeac.multichainlist.Snapshot<IQueuedEvent>)newEventsSnapshot);
										}
									}
								}
								catch (Exception e) 
								{
									for(IQueueEventResult scheduleResult : scheduledResultIndex.keySet())
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
												((IOnQueuedEvent)conf.getQueueController()).onQueuedEvent(event);
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
						for(IQueueEventResult scheduleResult : scheduledResultIndex.keySet())
						{
							try
							{
								((QueueEventResultImpl)scheduleResult).processPhaseIsFinished();
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
				firedEventsSnapshot = eventQueue.getFiredEventsSnapshot();
				try
				{
					if((firedEventsSnapshot != null) && (! firedEventsSnapshot.isEmpty()))
					{
	
						try
						{
							checkQueueObserve();
						}
						catch(Exception ex) {}
						catch(Error ex) {}
						
						for(Event event : firedEventsSnapshot)
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
											((IOnFiredEvent)conf.getQueueController()).onFiredEvent(event,this.eventQueue);
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
					if(firedEventsSnapshot != null)
					{
						try
						{
							firedEventsSnapshot.close();
						}
						finally 
						{
							firedEventsSnapshot = null;
						}
					}
				}
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
											((IOnRemovedEvent)conf.getQueueController()).onRemovedEvent(event);
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
				eventQueue.fetchSignalList(this.signalList); // TODO mcl
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
			eventQueue.getDueJobs(this.dueJobList); // TODO mcl
			
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
				List<IQueueTask> currentProcessedJobList = null;
				for(TaskContainer dueTask : this.dueJobList)
				{
					try
					{
						if(dueTask.getTaskControl().isDone())
						{
							continue;
						}
						if(go)
						{
							ITimer.Context timerContextJob = null;
							ITimer.Context timerContextQueue = null;
							try
							{
								jobTimeOut = ((dueTask.getTaskControl().getTimeOut() > 0) || (dueTask.getTaskControl().getHeartBeatTimeOut() > 0));
								this.currentRunningJob = dueTask;
								dueTask.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
								
								if(jobTimeOut)
								{
									if(dueTask.getTaskControl().getTimeOut() > 0)
									{
										this.currentTimeOutTimeStamp = System.currentTimeMillis() + dueTask.getTaskControl().getTimeOut();
									}
									this.eventQueue.getEventDispatcher().registerTimeOut(this.eventQueue,dueTask);
								}
								if(currentProcessedJobList == null)
								{
									currentProcessedJobList = new ArrayList<IQueueTask>();
									for(TaskContainer jobContainer : this.dueJobList)
									{
										currentProcessedJobList.add(jobContainer.getTask());
									}
								}
								if(dueTask.getTask() instanceof IPeriodicQueueJob)
								{
									Long periodicRepetitionInterval = ((IPeriodicQueueJob) dueTask.getTask()).getPeriodicRepetitionInterval();
									if((periodicRepetitionInterval ==  null) || (periodicRepetitionInterval.longValue() < 1))
									{
										periodicRepetitionInterval = 1000L * 60L * 60L * 24L * 365L * 108L;
									}
									dueTask.getTaskControl().setExecutionTimeStampPeriodic(System.currentTimeMillis() + periodicRepetitionInterval);
									dueTask.getTaskControl().preRunPeriodicJob();
								}
								else if(dueTask.getTask() instanceof IQueueService)
								{
									long periodicRepetitionInterval = -1L;
									
									try
									{
										if(dueTask.getPropertyBlock().getProperty(IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL) != null)
										{
											Object pri = dueTask.getPropertyBlock().getProperty(IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL);
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
									dueTask.getTaskControl().setExecutionTimeStampPeriodic(System.currentTimeMillis() + periodicRepetitionInterval);
									dueTask.getTaskControl().preRunPeriodicJob();
								}
								else
								{
									dueTask.getTaskControl().preRun();
								}
								
								dueTask.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_STARTED_TIMESTAMP, System.currentTimeMillis());
								
								if(dueTask.isNamedTask())
								{
									timerContextJob = dueTask.getMetrics().timer(IMetrics.METRICS_RUN_JOB).time();
								}
								
								timerContextQueue = eventQueue.getMetrics().timer(IMetrics.METRICS_RUN_JOB).time();
								
								dueTask.getTask().run(eventQueue, dueTask.getMetrics(), dueTask.getPropertyBlock(), dueTask.getTaskControl() ,currentProcessedJobList);
								
								dueTask.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_FINISHED_TIMESTAMP, System.currentTimeMillis());
								dueTask.getPropertyBlock().setProperty(IQueueTask.PROPERTY_KEY_THROWED_EXCEPTION, null);
								
								if(timerContextJob != null)
								{
									timerContextJob.stop();
								}
								timerContextQueue.stop();
								
								dueTask.getTaskControl().postRun();
								
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								if(jobTimeOut)
								{
									try
									{
										this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask);
									}
									catch (Exception e) 
									{
										this.log(LogService.LOG_ERROR, "eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueJob)", e);
									}
								}
								if(! go)
								{
									eventQueue.closeWorkerSnapshots();
									return;
								}
							}
							catch (Exception e) 
							{
								TaskContainer runningJob = this.currentRunningJob;
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								
								runningJob.getPropertyBlock().setProperty(IQueueTask.PROPERTY_KEY_THROWED_EXCEPTION, e);
								log(LogService.LOG_ERROR,"Exception while process job " + dueTask,e);
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
									if(dueTask.isNamedTask())
									{
										dueTask.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
									}
									eventQueue.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
								}
								catch (Exception e2) {}
								
								dueTask.getTaskControl().postRun();
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask);
								}
								
								if(! (dueTask.getTask() instanceof IQueueService))
								{
									dueTask.getTaskControl().setDone();
								}
								
								if(! go)
								{
									this.eventQueue.closeWorkerSnapshots();
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
												((IOnTaskError)conf.getQueueController()).onTaskError(this.eventQueue, dueTask.getTask(),  e);
											}
											catch (Exception ie) 
											{
												log(LogService.LOG_ERROR,"Error while process onTaskError " + dueTask,ie);
											}
										}
									}
								}
								catch (Exception ie) 
								{
									log(LogService.LOG_ERROR,"Error while process onTaskError " + dueTask,ie);
								}
				
							}
							catch (Error e) 
							{
								TaskContainer runningJob = this.currentRunningJob;
								this.currentTimeOutTimeStamp = null;
								this.currentRunningJob = null;
								
								Exception exc = new Exception(e.getMessage(),e);
								runningJob.getPropertyBlock().setProperty(IQueueTask.PROPERTY_KEY_THROWED_EXCEPTION, exc);
								log(LogService.LOG_ERROR,"Error while process task " + dueTask,e);
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
									if(dueTask.isNamedTask())
									{
										dueTask.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
									}
									eventQueue.getMetrics().meter(IMetrics.METRICS_RUN_JOB_ERROR).mark();
								}
								catch (Exception e2) {}
								
								dueTask.getTaskControl().postRun();
								if(jobTimeOut)
								{
									this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask);
								}
								
								if(! (dueTask.getTask() instanceof IQueueService))
								{
									dueTask.getTaskControl().setDone();
								}
								
								if(e instanceof ThreadDeath)
								{
									go = false;
								}
								
								if(! go)
								{
									this.eventQueue.closeWorkerSnapshots();
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
												((IOnTaskError)conf.getQueueController()).onTaskError(this.eventQueue, dueTask.getTask(), exc);
											}
											catch (Exception ie) 
											{
												log(LogService.LOG_ERROR,"Error while process onTaskError " + dueTask,ie);
											}
										}
									}
								}
								catch (Exception ie) 
								{
									log(LogService.LOG_ERROR,"Error while process onTaskError " + dueTask,ie);
								}
								
							}
							
							this.currentTimeOutTimeStamp = null;
							this.currentRunningJob = null;
							
							if(! go)
							{
								this.eventQueue.closeWorkerSnapshots();
								return;
							}
							
							try
							{
								firedEventsSnapshot = eventQueue.getFiredEventsSnapshot();
								try
								{
									if((firedEventsSnapshot != null) && (! firedEventsSnapshot.isEmpty()))
									{
					
										try
										{
											checkQueueObserve();
										}
										catch(Exception ex) {}
										catch(Error ex) {}
										
										for(Event event : firedEventsSnapshot)
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
															((IOnFiredEvent)conf.getQueueController()).onFiredEvent(event,this.eventQueue);
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
									if(firedEventsSnapshot != null)
									{
										try
										{
											firedEventsSnapshot.close();
										}
										finally 
										{
											firedEventsSnapshot = null;
										}
									}
								}
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
															((IOnRemovedEvent)conf.getQueueController()).onRemovedEvent(event);
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
								eventQueue.fetchSignalList(this.signalList); // TODO mcl
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
							
							if(dueTask.getTaskControl().isDone())
							{
								for(ControllerContainer conf : eventQueue.getConfigurationList())
								{
									try
									{
										if(go)
										{
											if(conf.isImplementingIOnJobDone())
											{
												((IOnTaskDone)conf.getQueueController()).onTaskDone(this.eventQueue, dueTask.getTask());
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
							if(! (dueTask.getTask() instanceof IQueueService))
							{
								dueTask.getTaskControl().setDone();
							}
						}
						catch (Exception ie) {}
						log(LogService.LOG_ERROR,"Error while process currentProcessedJobList",e);
					}
					
				}
			}
			
			eventQueue.cleanDoneJobs();
			this.eventQueue.closeWorkerSnapshots();
			
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
		TaskContainer timeOutJob = this.currentRunningJob;
		if(timeOutJob == null)
		{
			return false;
		}
		
		// HeartBeat TimeOut
		
		boolean heartBeatTimeout = false;
		if(timeOutJob.getTaskControl().getHeartBeatTimeOut() > 0)
		{
			try
			{
				long lastHeartBeat = (Long)timeOutJob.getMetrics().getQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
				if(lastHeartBeat > 0)
				{
					if((lastHeartBeat + timeOutJob.getTaskControl().getHeartBeatTimeOut() ) <= System.currentTimeMillis())
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
			if(timeOutJob.getTask() instanceof IQueueService)
			{
				timeOutJob.getTaskControl().timeOutService();
			}
			else
			{
				timeOutJob.getTaskControl().timeOut();
			}
		}
		catch (Exception e) {}
		
		for(ControllerContainer conf : eventQueue.getConfigurationList())
		{
			try
			{
				if(conf.getQueueController() instanceof IOnTaskTimeout)
				{
					((EventDispatcherImpl)eventQueue.getDispatcher()).executeOnJobTimeOut((IOnTaskTimeout)conf.getQueueController(), this.eventQueue, timeOutJob.getTask());
				}
			}
			catch (Exception e) {}
		}
		
		if(timeOutJob.getTaskControl().getStopOnTimeOutFlag())
		{
			if(Thread.currentThread() != this)
			{
				try
				{
					stop.set(true);
					((EventDispatcherImpl)eventQueue.getDispatcher()).executeOnJobStopExecuter(this, timeOutJob.getTask());
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

	public TaskContainer getCurrentRunningJob()
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
