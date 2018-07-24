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

import java.util.List;

/**
 * 
 * This object inform invoker of {@link IQueue#scheduleEvent(org.osgi.service.event.Event)} (or related) about state of scheduling
 * The schedule implementation {@link IOnScheduleEvent} / {@link IOnScheduleEventList} has confirm scheduled state, if it is in charge of this event type
 * 
 * @author Sebastian Palarus
 *
 */
public interface IScheduleResult
{
	/**
	 * marks event(s) as scheduled
	 */
	public void setScheduled();
	
	/**
	 * getter for scheduled-state
	 * @return
	 */
	public boolean isScheduled();
	
	/**
	 * publish an error
	 * 
	 * @param throwable 
	 */
	public void addError(Throwable throwable);
	
	/**
	 * return schedule result contains errors
	 * 
	 * @return true if scheduler or worker published an error, otherwise false,
	 */
	public boolean hasErrors();
	
	/**
	 * returns all published errors
	 * 
	 * @return all published errors
	 */
	public List<Throwable> getErrorList();
	
	/**
	 * getter for detailResultObject
	 * 
	 * @return detailResultObject
	 */
	public Object getDetailResultObject();
	
	/**
	 * setter for detailResultObject
	 * 
	 * @param detailResultObject notify object for invoker
	 */
	public void setDetailResultObject(Object detailResultObject);
	
	/**
	 * return all published detail result objects
	 * 
	 * @return a list of published detail result object
	 */
	public List<Object> getDetailResultObjectList();
	
	/**
	 * publish a new detail result object
	 * 
	 * @param detailResultObject detail result object (appends to detailResultObjectList)
	 */
	public void addDetailResultObjectList(Object detailResultObject);
	
}
