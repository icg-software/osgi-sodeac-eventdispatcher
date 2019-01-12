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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.event.Event;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IPeriodicQueueTask;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
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
import org.sodeac.eventdispatcher.api.ITaskControl.ExecutionTimestampSource;
import org.sodeac.eventdispatcher.impl.TaskControlImpl.PeriodicServiceTimestampPredicate;
import org.sodeac.eventdispatcher.impl.TaskControlImpl.ScheduleTimestampPredicate;

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
	
	private List<TaskContainer> dueTaskList = null;
	private List<String> signalList = null;
	private List<IOnQueueAttach> onQueueAttachList = null;
	
	private volatile Long currentTimeOutTimeStamp = null;
	private volatile TaskContainer currentRunningTask = null;
	private volatile long wakeUpTimeStamp = -1;
	private volatile boolean inFreeingArea = false;
	
	public QueueWorker(QueueImpl impl)
	{
		super();
		this.eventQueue = impl;
		this.workerWrapper = new QueueWorkerWrapper(this);
		this.dueTaskList = new ArrayList<TaskContainer>();
		this.signalList = new ArrayList<String>();
		this.onQueueAttachList = new ArrayList<IOnQueueAttach>();
		super.setDaemon(true);
		super.setName(QueueWorker.class.getSimpleName() + " " + this.eventQueue.getId());
	}

	private void checkQueueAttach()
	{
		try
		{
			if(! this.onQueueAttachList.isEmpty())
			{
				this.onQueueAttachList.clear();
			}
			eventQueue.fetchOnQueueAttachList(this.onQueueAttachList); // TODO mcl
			if(this.onQueueAttachList.isEmpty())
			{
				return;
			}
			for(IOnQueueAttach onQueueAttach : this.onQueueAttachList)
			{
				try
				{
					onQueueAttach.onQueueAttach(this.eventQueue);
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
			this.onQueueAttachList.clear();
		}
		catch (Exception e) 
		{
			this.onQueueAttachList.clear();
			log(LogService.LOG_ERROR,"Exception while check queueAttach"
					+ "",e);
		}
		catch (Error e) 
		{
			this.onQueueAttachList.clear();
			log(LogService.LOG_ERROR,"Exception while check queueAttach",e);
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	public void run()
	{
		Set<IQueueEventResult> scheduledResultSet = new HashSet<IQueueEventResult>();
		org.sodeac.multichainlist.Snapshot<? extends IQueuedEvent> newEventsSnapshot;
		org.sodeac.multichainlist.Snapshot<? extends IQueuedEvent> removedEventsSnapshot;
		org.sodeac.multichainlist.Snapshot<Event> firedEventsSnapshot;
		while(go)
		{
			
			try
			{
				checkQueueAttach();
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
							checkQueueAttach();
						}
						catch(Exception ex) {}
						catch(Error ex) {}
						
						boolean singleProcess = false;
						boolean listProcess = false;
							
						scheduledResultSet.clear();
						for(IQueuedEvent event : newEventsSnapshot)
						{
							try
							{
								scheduledResultSet.add(event.getScheduleResultObject());
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
									for(IQueueEventResult scheduleResult : scheduledResultSet)
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
						for(IQueueEventResult scheduleResult : scheduledResultSet)
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
						scheduledResultSet.clear();
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
							checkQueueAttach();
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
							checkQueueAttach();
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
						checkQueueAttach();
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
			
			this.dueTaskList.clear();
			eventQueue.getDueTasks(this.dueTaskList); // TODO mcl
			
			if(! dueTaskList.isEmpty())
			{

				try
				{
					checkQueueAttach();
				}
				catch(Exception ex) {}
				catch(Error ex) {}
				
				eventQueue.touchLastWorkerAction();
				boolean taskTimeOut  = false;
				List<IQueueTask> currentProcessedTaskList = null;
				for(TaskContainer dueTask : this.dueTaskList)
				{
					try
					{
						if(dueTask.getTaskControl().isDone())
						{
							continue;
						}
						if(go)
						{
							ITimer.Context timerContextTask = null;
							ITimer.Context timerContextQueue = null;
							try
							{
								taskTimeOut = ((dueTask.getTaskControl().getTimeout() > 0) || (dueTask.getTaskControl().getHeartbeatTimeout() > 0));
								this.currentRunningTask = dueTask;
								dueTask.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
								
								if(taskTimeOut)
								{
									if(dueTask.getTaskControl().getTimeout() > 0)
									{
										this.currentTimeOutTimeStamp = System.currentTimeMillis() + dueTask.getTaskControl().getTimeout();
									}
									this.eventQueue.getEventDispatcher().registerTimeOut(this.eventQueue,dueTask);
								}
								if(currentProcessedTaskList == null)
								{
									currentProcessedTaskList = new ArrayList<IQueueTask>();
									for(TaskContainer taskContainer : this.dueTaskList)
									{
										currentProcessedTaskList.add(taskContainer.getTask());
									}
								}
								if(dueTask.getTask() instanceof IPeriodicQueueTask)
								{
									Long periodicRepetitionInterval = ((IPeriodicQueueTask) dueTask.getTask()).getPeriodicRepetitionInterval();
									if((periodicRepetitionInterval ==  null) || (periodicRepetitionInterval.longValue() < 1))
									{
										periodicRepetitionInterval = 1000L * 60L * 60L * 24L * 365L * 108L;
									}
									dueTask.getTaskControl().setExecutionTimeStamp
									(
										System.currentTimeMillis() + periodicRepetitionInterval, 
										ExecutionTimestampSource.PERODIC, 
										PeriodicServiceTimestampPredicate.getInstance()
									);
									dueTask.getTaskControl().preRunPeriodicTask();
								}
								else if(dueTask.getTask() instanceof IQueueService)
								{
									long periodicRepetitionInterval = -1L;
									
									try
									{
										if(dueTask.getPropertyBlock().getProperty(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL) != null)
										{
											Object pri = dueTask.getPropertyBlock().getProperty(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL);
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
									dueTask.getTaskControl().setExecutionTimeStamp
									(
										System.currentTimeMillis() + periodicRepetitionInterval, 
										ExecutionTimestampSource.PERODIC, 
										PeriodicServiceTimestampPredicate.getInstance()
									);
									dueTask.getTaskControl().preRunPeriodicTask();
								}
								else
								{
									dueTask.getTaskControl().preRun();
								}
								
								dueTask.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_STARTED_TIMESTAMP, System.currentTimeMillis());
								
								if(dueTask.isNamedTask())
								{
									timerContextTask = dueTask.getMetrics().timer(IMetrics.METRICS_RUN_TASK).time();
								}
								
								timerContextQueue = eventQueue.getMetrics().timer(IMetrics.METRICS_RUN_TASK).time();
								
								dueTask.getTask().run(eventQueue, dueTask.getMetrics(), dueTask.getPropertyBlock(), dueTask.getTaskControl() ,currentProcessedTaskList);
								
								dueTask.getMetrics().setQualityValue(IMetrics.QUALITY_VALUE_FINISHED_TIMESTAMP, System.currentTimeMillis());
								dueTask.getPropertyBlock().setProperty(EventDispatcherConstants.PROPERTY_KEY_THROWED_EXCEPTION, null);
								
								if(timerContextTask != null)
								{
									timerContextTask.stop();
								}
								timerContextQueue.stop();
								
								dueTask.getTaskControl().postRun();
								
								this.currentTimeOutTimeStamp = null;
								this.currentRunningTask = null;
								if(taskTimeOut)
								{
									try
									{
										this.eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask);
									}
									catch (Exception e) 
									{
										this.log(LogService.LOG_ERROR, "eventQueue.getEventDispatcher().unregisterTimeOut(this.eventQueue,dueTask)", e);
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
								TaskContainer runningTask = this.currentRunningTask;
								this.currentTimeOutTimeStamp = null;
								this.currentRunningTask = null;
								
								runningTask.getPropertyBlock().setProperty(EventDispatcherConstants.PROPERTY_KEY_THROWED_EXCEPTION, e);
								log(LogService.LOG_ERROR,"Exception while process task " + dueTask,e);
								if(timerContextTask != null)
								{
									try
									{
										timerContextTask.stop();
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
										dueTask.getMetrics().meter(IMetrics.METRICS_RUN_TASK_ERROR).mark();
									}
									eventQueue.getMetrics().meter(IMetrics.METRICS_RUN_TASK_ERROR).mark();
								}
								catch (Exception e2) {}
								
								dueTask.getTaskControl().postRun();
								if(taskTimeOut)
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
										if(conf.isImplementingIOnTaskError())
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
								TaskContainer runningTask = this.currentRunningTask;
								this.currentTimeOutTimeStamp = null;
								this.currentRunningTask = null;
								
								Exception exc = new Exception(e.getMessage(),e);
								runningTask.getPropertyBlock().setProperty(EventDispatcherConstants.PROPERTY_KEY_THROWED_EXCEPTION, exc);
								log(LogService.LOG_ERROR,"Error while process task " + dueTask,e);
								if(timerContextTask != null)
								{
									try
									{
										timerContextTask.stop();
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
										dueTask.getMetrics().meter(IMetrics.METRICS_RUN_TASK_ERROR).mark();
									}
									eventQueue.getMetrics().meter(IMetrics.METRICS_RUN_TASK_ERROR).mark();
								}
								catch (Exception e2) {}
								
								dueTask.getTaskControl().postRun();
								if(taskTimeOut)
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
										if(conf.isImplementingIOnTaskError())
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
							this.currentRunningTask = null;
							
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
											checkQueueAttach();
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
											checkQueueAttach();
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
										checkQueueAttach();
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
											if(conf.isImplementingIOnTaskDone())
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
						log(LogService.LOG_ERROR,"Error while process currentProcessedTaskList",e);
					}
					
				}
			}
			
			eventQueue.cleanDoneTasks();
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
					checkQueueAttach();
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
		TaskContainer timeOutTask = this.currentRunningTask;
		if(timeOutTask == null)
		{
			return false;
		}
		
		// HeartBeat TimeOut
		
		boolean heartBeatTimeout = false;
		if(timeOutTask.getTaskControl().getHeartbeatTimeout() > 0)
		{
			try
			{
				long lastHeartBeat = (Long)timeOutTask.getMetrics().getQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT);
				if(lastHeartBeat > 0)
				{
					if((lastHeartBeat + timeOutTask.getTaskControl().getHeartbeatTimeout() ) <= System.currentTimeMillis())
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
			// Task TimeOut
			
			Long timeOut = this.currentTimeOutTimeStamp;
			if(timeOut == null)
			{
				return false;
			}
			
			// check timeOut and timeOutTask again to prevent working with values don't match
			
			if(timeOutTask != this.currentRunningTask)
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
			if(timeOutTask.getTask() instanceof IQueueService)
			{
				timeOutTask.getTaskControl().timeOutService();
			}
			else
			{
				timeOutTask.getTaskControl().timeout();
			}
		}
		catch (Exception e) {}
		
		for(ControllerContainer conf : eventQueue.getConfigurationList())
		{
			try
			{
				if(conf.getQueueController() instanceof IOnTaskTimeout)
				{
					((EventDispatcherImpl)eventQueue.getDispatcher()).executeOnTaskTimeOut((IOnTaskTimeout)conf.getQueueController(), this.eventQueue, timeOutTask.getTask());
				}
			}
			catch (Exception e) {}
		}
		
		if(timeOutTask.getTaskControl().getStopOnTimeoutFlag())
		{
			if(Thread.currentThread() != this)
			{
				try
				{
					stop.set(true);
					((EventDispatcherImpl)eventQueue.getDispatcher()).executeOnTaskStopExecuter(this, timeOutTask.getTask());
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

	public TaskContainer getCurrentRunningTask()
	{
		return currentRunningTask;
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
