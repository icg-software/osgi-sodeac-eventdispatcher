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
 * Wrapper for io.dropwizard.metrics.Meter
 * 
 * @see <a href="http://metrics.dropwizard.io/3.2.3/manual/core.html#meters">Coda Hale Metrics</a>
 * 
 * @author Sebastian Palarus
 *
 */
public interface IMeter
{
	/**
	 * Mark the occurrence of an event.
	 */
	public void mark();
	
	/**
	 * Mark the occurrence of a given number of events.
	 *
	 * @param n the number of events
	 */
	public void mark(long n);
	
	/**
	 * Returns the number of events which have been marked.
	 *
	 * @return the number of events which have been marked
	 */
	public long getCount();
	
	/**
	 * Returns the mean rate at which events have occurred since the meter was created.
	 *
	 * @return the mean rate at which events have occurred since the meter was created
	 */
	public double getMeanRate();
	
	/**
	 * Returns the one-minute exponentially-weighted moving average rate at which events have
	 * occurred since the meter was created.
	 * 
	 * This rate has the same exponential decay factor as the one-minute load average in the {@code
	 * top} Unix command.
	 *
	 * @return the one-minute exponentially-weighted moving average rate at which events have
	 *         occurred since the meter was created
	 */
	public double getOneMinuteRate();
	
	/**
	 * Returns the five-minute exponentially-weighted moving average rate at which events have
	 * occurred since the meter was created.
	 * 
	 * This rate has the same exponential decay factor as the five-minute load average in the {@code
	 * top} Unix command.
	 *
	 * @return the five-minute exponentially-weighted moving average rate at which events have
	 *         occurred since the meter was created
	 */
	public double getFiveMinuteRate();
	
	/**
	 * Returns the fifteen-minute exponentially-weighted moving average rate at which events have
	 * occurred since the meter was created.
	 * 
	 * This rate has the same exponential decay factor as the fifteen-minute load average in the
	 * {@code top} Unix command.
	 *
	 * @return the fifteen-minute exponentially-weighted moving average rate at which events have
	 *         occurred since the meter was created
	 */
	public double getFifteenMinuteRate();
}
