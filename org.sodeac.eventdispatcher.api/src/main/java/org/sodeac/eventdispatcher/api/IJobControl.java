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
	 * @return timestamp, the worker has to invoke run-methode
	 */
	public long getExecutionTimeStamp();
	
	public long setExecutionTimeStamp(long executionTimeStamp);
	
	
	/**
	 * 
	 * @return time in ms, the run-methode should be finished
	 */
	public long getTimeOut(); // Rule
	
	public long setTimeOut(long timeOut);
	
	
	public long getHeartBeatTimeOut();
	
	public long setHeartBeatTimeOut(long heartBeatTimeOut);
	
	public void timeOut(); // Command
	
	public boolean stopOnTimeOut();

	public boolean isInTimeOut();
}
