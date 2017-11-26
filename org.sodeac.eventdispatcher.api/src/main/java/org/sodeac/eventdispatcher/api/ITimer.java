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

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper for io.dropwizard.metrics.Timer
 * 
 *  @see <a href="http://metrics.dropwizard.io/3.2.3/manual/core.html#timers">Coda Hale Metrics</a>
 * 
 * @author Sebastian Palarus
 *
 */
public interface ITimer
{
	/**
	 * A time context (a running stop watch)
	 * 
	 * @author Sebastian Palarus
	 *
	 */
	public interface Context extends Closeable
	{
		
		/**
		 * Updates the timer with the difference between current and start time. Call to this method will
		 * not reset the start time. Multiple calls result in multiple updates.
		 * @return the elapsed time in nanoseconds
		 */
		public long stop();
	}
	
	/**
	 *  Adds a recorded duration.
	 *
	 * @param duration the length of the duration
	 * @param unit     the scale unit of {@code duration}
	 */
	public void update(long duration, TimeUnit unit);

	/**
	 * Returns a new {@link Context}.
	 *
	 * @return a new {@link Context}
	 * @see Context
	 */
	public Context time();
	
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
	
	/**
	 * Returns a snapshot of values.
	 *
	 * @return a snapshot of values
	 */
	public IMetricSnapshot getSnapshot();
}
