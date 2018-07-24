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
 * Wrapper for io.dropwizard.metrics.Snapshot
 * 
 * @see <a href="http://metrics.dropwizard.io/3.2.3/manual/core.html#histograms">Coda Hale Metrics</a>
 * 
 * @author Sebastian Palarus
 *
 */
public interface IMetricSnapshot
{
	/**
	 * Returns the value at the given quantile.
	 *
	 * @param quantile    a given quantile, in {@code [0..1]}
	 * @return the value in the distribution at {@code quantile}
	 */
	public double getValue(double quantile);
	
	/**
	 * Returns the entire set of values in the snapshot.
	 *
	 * @return the entire set of values
	 */
	public long[] getValues();
	
	/**
	 * Returns the number of values in the snapshot.
	 *
	 * @return the number of values
	 */
	public int size();
	
	/**
	 * Returns the median value in the distribution.
	 *
	 * @return the median value
	 */
	public double getMedian();
	
	/**
	 * Returns the value at the 75th percentile in the distribution.
	 *
	 * @return the value at the 75th percentile
	 */
	public double get75thPercentile();
	
	/**
	 * Returns the value at the 95th percentile in the distribution.
	 *
	 * @return the value at the 95th percentile
	 */
	public double get95thPercentile();
	
	/**
	 * Returns the value at the 98th percentile in the distribution.
	 *
	 * @return the value at the 98th percentile
	 */
	public double get98thPercentile();
	
	/**
	 * Returns the value at the 99th percentile in the distribution.
	 *
	 * @return the value at the 99th percentile
	 */
	public double get99thPercentile();
	
	/**
	 * Returns the value at the 99.9th percentile in the distribution.
	 *
	 * @return the value at the 99.9th percentile
	 */
	public double get999thPercentile();
	
	/**
	 * Returns the highest value in the snapshot.
	 *
	 * @return the highest value
	 */
	public long getMax();
	
	/**
	 * Returns the arithmetic mean of the values in the snapshot.
	 *
	 * @return the arithmetic mean
	 */
	public double getMean();
	
	/**
	 * Returns the lowest value in the snapshot.
	 *
	 * @return the lowest value
	 */
	public long getMin();
}
