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
package org.sodeac.eventdispatcher.api;

/**
 * acts as state container for taskexecutions
 * 
 * @author Sebastian Palarus
 *
 */
public interface ITaskControl
{
	
	public enum ExecutionTimestampSource {SCHEDULE,RESCHEDULE,WORKER,PERODIC,TRIGGER}
	
	/**
	 * 
	 * @return true, of task is done, otherwise false
	 */
	public boolean isDone();
	
	/**
	 * mark task as done
	 * 
	 * @return true, if task is already marked as done, otherwise false
	 */
	public boolean setDone();
	
	
	/**
	 * 
	 * @return timestamp of execution plan for next run
	 */
	public long getExecutionTimestamp();
	
	/**
	 * 
	 * @return SourceType of executionTimestamp
	 */
	public ExecutionTimestampSource getExecutionTimestampSource();

	/**
	 * setter for timestamp of execution plan for next run
	 * 
	 * <br>
	 * 
	 * additionally task is marked done == false
	 * 
	 * @param executionTimestamp timestamp for next run
	 * @param force force to set new execution timestamp
	 * 
	 * @return success state
	 */
	public boolean setExecutionTimestamp(long executionTimestamp, boolean force);
	
	
	/**
	 * getter for timout value
	 * 
	 * @return timeout value
	 */
	public long getTimeout();
	
	/**
	 * setter for timeout value
	 * 
	 * @param timeout new timeout value
	 * @return overwritten timeout value
	 */
	public long setTimeout(long timeout);
	
	
	/**
	 * getter for heartbeat timeout value
	 * 
	 * @return heartbeat timeout value
	 */
	public long getHeartbeatTimeout();
	
	/**
	 * setter for heartbeat timeout value
	 * 
	 * @param heartbeatTimeout new heartbeat timeout value
	 * 
	 * @return overwritten heartbeat timeout value 
	 */
	public long setHeartbeatTimeout(long heartbeatTimeout);
	
	/**
	 * notify, that this task runs to long
	 */
	public void timeout(); 

	/**
	 * 
	 * @return true if task runs to long (timeout or heartbeat timeout), otherwise false
	 */
	public boolean isInTimeout();
	
	/**
	 * setter for stopOnTimeout flag
	 * 
	 * If a task runs in a timeout, the worker is no longer in use, but normally continuing running to finish the runMethode. 
	 * A long running task should ask for timeout by invoking isInTimeOut() periodically to clean up and skip continuing. 
	 * But, if worker is blocked by network operation for example, the guardian can stop worker thread by setting  stopOnTimeoutFlag true. 
	 * 
	 * Attention !!! You should be familiar with all things can happens by stopping a thread. You can handle some issues if {@link IQueueTask} implements {@link IOnTaskStop} 
	 * 
	 * 
	 * @param stopOnTimeoutFlag new stopOnTimeoutFlag value
	 * 
	 * @return old stopOnTimeout value
	 */
	public boolean setStopOnTimeoutFlag(boolean stopOnTimeoutFlag);

	/**
	 * getter for stopOnTimeout flag
	 * 
	 * @return stopOnTimeout flag
	 */
	public boolean getStopOnTimeoutFlag();

}
