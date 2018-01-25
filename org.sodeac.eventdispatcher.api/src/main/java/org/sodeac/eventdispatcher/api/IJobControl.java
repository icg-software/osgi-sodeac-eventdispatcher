/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

/**
 * acts as state container for jobexecutions
 * 
 * @author Sebastian Palarus
 *
 */
public interface IJobControl
{
	
	public enum ExecutionTimeStampSource {SCHEDULE,RESCHEDULE,WORKER,PERODIC}
	
	/**
	 * 
	 * @return true, of job is done, otherwise false
	 */
	public boolean isDone();
	
	/**
	 * mark job as done
	 * 
	 * @return true, if job is already marked as done, otherwise false
	 */
	public boolean setDone();
	
	
	/**
	 * 
	 * @return timestamp of execution plan for next run
	 */
	public long getExecutionTimeStamp();
	
	/**
	 * 
	 * @return SourceType of executionTimestamp
	 */
	public ExecutionTimeStampSource getExecutionTimeStampSource();

	/**
	 * setter for timestamp of execution plan for next run
	 * 
	 * <br>
	 * 
	 * additionally job is marked done == false
	 * 
	 * @param executionTimeStamp timestamp for next run
	 * @param force force to set new execution timestamp
	 * 
	 * @return success state
	 */
	public boolean setExecutionTimeStamp(long executionTimeStamp, boolean force);
	
	
	/**
	 * getter for timout value
	 * 
	 * @return timeout value
	 */
	public long getTimeOut();
	
	/**
	 * setter for timeout value
	 * 
	 * @param timeOut new timeout value
	 * @return overwritten timeout value
	 */
	public long setTimeOut(long timeOut);
	
	
	/**
	 * getter for heartbeat timeout value
	 * 
	 * @return heartbeat timeout value
	 */
	public long getHeartBeatTimeOut();
	
	/**
	 * setter for heartbeat timeout value
	 * 
	 * @param heartBeatTimeOut new heartbeat timeout value
	 * 
	 * @return overwritten heartbeat timeout value 
	 */
	public long setHeartBeatTimeOut(long heartBeatTimeOut);
	
	/**
	 * notify, that this job runs to long
	 */
	public void timeOut(); 

	/**
	 * 
	 * @return true if job runs to long (timeout or heartbeat timeout), otherwise false
	 */
	public boolean isInTimeOut();
	
	/**
	 * setter for stopOnTimeout flag
	 * 
	 * If a job runs in a timeout, the worker is no longer in use, but normally continuing running to finish the runMethode. 
	 * A long running job should ask for timeout by invoking isInTimeOut() periodically to clean up and skip continuing. 
	 * But, if worker is blocked by network operation for example, the guardian can stop worker thread by setting  stopOnTimeoutFlag true. 
	 * 
	 * Attention !!! You should be familiar with all things can happens by stopping a thread. You can handle some issues if {@link IQueueJob} implements {@link IOnJobStop} 
	 * 
	 * 
	 * @param stopOnTimeoutFlag new stopOnTimeoutFlag value
	 * 
	 * @return old stopOnTimeout value
	 */
	public boolean setStopOnTimeOutFlag(boolean stopOnTimeoutFlag);

	/**
	 * getter for stopOnTimeout flag
	 * 
	 * @return stopOnTimeout flag
	 */
	public boolean getStopOnTimeOutFlag();

}
