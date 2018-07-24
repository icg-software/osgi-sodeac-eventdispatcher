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
 * Wrapper for io.dropwizard.metrics.Histogram
 * 
 * @see <a href="http://metrics.dropwizard.io/3.2.3/manual/core.html#histograms">Coda Hale Metrics</a>
 * 
 * @author Sebastian Palarus
 *
 */
public interface IHistogram
{	
	/**
	 * Adds a recorded value.
	 *
	 * @param value the length of the value
	 */
	public void update(int value);
	
	/**
	 * Adds a recorded value.
	 *
	 * @param value the length of the value
	 */
	public void update(long value);
	
	/**
	 * Returns the number of values recorded.
	 *
	 * @return the number of values recorded
	 */
	public long getCount();
	
	/**
	 * Returns a snapshot of values.
	 *
	 * @return a snapshot of values
	 */
	public IMetricSnapshot getSnapshot();
}
