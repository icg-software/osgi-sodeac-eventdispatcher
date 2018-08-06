/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Declares the necessity to use same queue worker thread for synchronized queue activities at all times. 
 * Otherwise the used worker can shared with other queues for synchronized queue activities. In both cases the exclusive access to queue resources is guaranteed.
 * 
 * <br>
 * 
 * <table border="1">
 * <tr>
 * <th>
 * Condition
 * </th>
 * <th>
 * Result
 * </th>
 * </tr>
 * <tr>
 * <td>
 * At least one {@link IQueueComponent} claims to use private worker by {@link PrivateQueueWorkerRequirement#RequirePrivateQueueWorker}
 * </td>
 * <td>
 * Use private Worker (always)
 * </td>
 * </tr>
 * <tr>
 * <td>
 * More {@link IQueueComponent}s prefer to use same worker ({@link PrivateQueueWorkerRequirement#PreferPrivateQueueWorker}) then not ({@link PrivateQueueWorkerRequirement#PreferSharedQueueWorker})
 * </td>
 * <td>
 * Use private Worker (if no {@link IQueueComponent} claims to use private worker by {@link PrivateQueueWorkerRequirement#RequirePrivateQueueWorker})
 * </td>
 * </tr>
 * <tr>
 * <td>
 * More {@link IQueueComponent}s prefer to use shared worker ({@link PrivateQueueWorkerRequirement#PreferSharedQueueWorker}) then not ({@link PrivateQueueWorkerRequirement#PreferPrivateQueueWorker})
 * </td>
 * <td>
 * Use shared Worker (if no {@link IQueueComponent} claims to use private worker by {@link PrivateQueueWorkerRequirement#RequirePrivateQueueWorker})
 * </td>
 * </tr>
 * <tr>
 * <td>
 * No {@link IQueueComponent}s has a preference or a requirement / all {@link IQueueComponent}s declare {@link PrivateQueueWorkerRequirement#NoPreferenceOrRequirement}
 * </td>
 * <td>
 * Use shared Worker
 * </td>
 * </tr>
 * </table>
 * <br>
 * Private worker can be useful, if {@link IQueueComponent} use thread sensitive libraries like Mozilla Rhino. 
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public enum PrivateQueueWorkerRequirement 
{
	
	/**
	 * Preference to use shared queue worker.
	 */
	PreferSharedQueueWorker(1),
	
	/**
	 * It does not matter if queue use private or shared queue worker
	 */
	NoPreferenceOrRequirement(2),
	
	/**
	 * Preference to use private queue worker.
	 */
	PreferPrivateQueueWorker(3),
	
	/**
	 * {@link IQueueComponent} requires a private queue worker.
	 */
	RequirePrivateQueueWorker(4);
	
	private PrivateQueueWorkerRequirement(int intValue)
	{
		this.intValue = intValue;
	}
	
	private static volatile Set<PrivateQueueWorkerRequirement> ALL = null;
	
	private int intValue;
	
	/**
	 * getter for all privateQueueWorker
	 * 
	 * @return Set of all privateQueueWorker
	 */
	public static Set<PrivateQueueWorkerRequirement> getAll()
	{
		if(PrivateQueueWorkerRequirement.ALL == null)
		{
			EnumSet<PrivateQueueWorkerRequirement> all = EnumSet.allOf(PrivateQueueWorkerRequirement.class);
			PrivateQueueWorkerRequirement.ALL = Collections.unmodifiableSet(all);
		}
		return PrivateQueueWorkerRequirement.ALL;
	}
	
	/**
	 * search privateQueueWorker enum represents by {@code value}
	 * 
	 * @param value integer value of privateQueueWorker
	 * 
	 * @return privateQueueWorker enum represents by {@code value}
	 */
	public static PrivateQueueWorkerRequirement findByInteger(int value)
	{
		for(PrivateQueueWorkerRequirement privateQueueWorker : getAll())
		{
			if(privateQueueWorker.intValue == value)
			{
				return privateQueueWorker;
			}
		}
		return null;
	}
	
	/**
	 * search privateQueueWorker enum represents by {@code name}
	 * 
	 * @param name of privateQueueWorker
	 * 
	 * @return enum represents by {@code name}
	 */
	public static PrivateQueueWorkerRequirement findByName(String name)
	{
		for(PrivateQueueWorkerRequirement privateQueueWorker : getAll())
		{
			if(privateQueueWorker.name().equalsIgnoreCase(name))
			{
				return privateQueueWorker;
			}
		}
		return null;
	}
}
