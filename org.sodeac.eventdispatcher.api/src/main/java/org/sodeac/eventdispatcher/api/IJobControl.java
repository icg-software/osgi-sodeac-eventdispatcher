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
package org.sodeac.eventdispatcher.api;

/**
 * acts as state container for jobexecutions
 * 
 * @author Sebastian Palarus
 *
 */
public interface IJobControl
{
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
	 * setter for timestamp of execution plan for next run
	 * 
	 * <br>
	 * 
	 * additionally job is marked done == false
	 * 
	 * @param executionTimeStamp timestamp for next run
	 * 
	 * @return overwritten execution timestamp
	 */
	public long setExecutionTimeStamp(long executionTimeStamp);
	
	
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
	
	// TODO
	/*
	 * in moment, ignore this methode
	 * 
	 * @return false
	 */
	/*public boolean stopOnTimeOut();*/


}
