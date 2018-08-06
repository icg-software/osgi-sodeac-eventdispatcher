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
 *  Declares the necessity to use queue metrics (for all {@link IQueueComponent}s) or job metrics ( for {@link IQueueService}s only). 
 * <br><br>
 * A {@link IQueueService} can declare the necessity for JobMetrics:<br>
 * <br>

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
 * {@link MetricsRequirement#PreferNoMetrics}
 * </td>
 * <td>
 * JobMetrics Off
 * </td>
 * </tr>
 * <tr>
 * <td>
 * {@link MetricsRequirement#NoPreferenceOrRequirement}/{@link MetricsRequirement#PreferMetrics}/{@link MetricsRequirement#RequireMetrics}
 * </td>
 * <td>
 * JobMetrics ON
 * </td>
 * </tr>
 * </table>
 * <br>
 * Various {@link IQueueComponent}s ({@link IQueueController}s  and {@link IQueueService}s) could have different requirements to use queue metrics or not. Here the final result:<br>
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
 * At least one {@link IQueueComponent} claims metrics with {@link MetricsRequirement#RequireMetrics}
 * </td>
 * <td>
 * QueueMetrics ON (always)
 * </td>
 * </tr>
 * <tr>
 * <td>
 * More {@link IQueueComponent}s prefer to use metrics ({@link MetricsRequirement#PreferMetrics}) then not to use ({@link MetricsRequirement#PreferNoMetrics})
 * </td>
 * <td>
 * QueueMetrics ON (if no {@link IQueueComponent} claims metrics by {@link MetricsRequirement#RequireMetrics})
 * </td>
 * </tr>
 * <tr>
 * <td>
 * No {@link IQueueComponent}s has a preference or a requirement / all {@link IQueueComponent}s declare {@link MetricsRequirement#NoPreferenceOrRequirement}
 * </td>
 * <td>
 * QueueMetrics ON
 * </td>
 * </tr>
 * <tr>
 * <td>
 * More {@link IQueueComponent}s prefer to use metrics ({@link MetricsRequirement#PreferNoMetrics}) then not to use  ({@link MetricsRequirement#PreferMetrics})
 * </td>
 * <td>
 * QueueMetrics OFF (if no {@link IQueueComponent} claims metrics by {@link MetricsRequirement#RequireMetrics})
 * </td>
 * </tr>
 * </table>
 * <br>
 * The tendency is to set metrics on, because of monitoring {@link IQueueController}s and {@link IQueueService}s. For situations in which monitoring makes little sense and the {@link IQueueComponent} 
 * does not require metrics data for it self, the {@link IQueueComponent} can help to safe system resources by declaring {@link MetricsRequirement#PreferNoMetrics}, but this Announcement is no guarantee that {@link IEventDispatcher} switch off queue metrics. 
 * 
 * @author Sebastian Palarus
 * @since 1.0
 * @version 1.0
 *
 */
public enum MetricsRequirement 
{
	
	/**
	 * Metrics should be switch off
	 */
	PreferNoMetrics(1),
	
	/**
	 * It does not matter if the metrics are switch on or switch off.
	 */
	NoPreferenceOrRequirement(2),
	
	/**
	 * Metrics should be switch on.
	 */
	PreferMetrics(3),
	
	/**
	 * Metrics are required
	 */
	RequireMetrics(4);
	
	private MetricsRequirement(int intValue)
	{
		this.intValue = intValue;
	}
	
	private static volatile Set<MetricsRequirement> ALL = null;
	
	private int intValue;
	
	/**
	 * getter for all metrics
	 * 
	 * @return Set of all metrics
	 */
	public static Set<MetricsRequirement> getAll()
	{
		if(MetricsRequirement.ALL == null)
		{
			EnumSet<MetricsRequirement> all = EnumSet.allOf(MetricsRequirement.class);
			MetricsRequirement.ALL = Collections.unmodifiableSet(all);
		}
		return MetricsRequirement.ALL;
	}
	
	/**
	 * search metrics enum represents by {@code value}
	 * 
	 * @param value integer value of metrics
	 * 
	 * @return metrics enum represents by {@code value}
	 */
	public static MetricsRequirement findByInteger(int value)
	{
		for(MetricsRequirement metrics : getAll())
		{
			if(metrics.intValue == value)
			{
				return metrics;
			}
		}
		return null;
	}
	
	/**
	 * search metrics enum represents by {@code name}
	 * 
	 * @param name of metrics
	 * 
	 * @return enum represents by {@code name}
	 */
	public static MetricsRequirement findByName(String name)
	{
		for(MetricsRequirement metrics : getAll())
		{
			if(metrics.name().equalsIgnoreCase(name))
			{
				return metrics;
			}
		}
		return null;
	}
}
