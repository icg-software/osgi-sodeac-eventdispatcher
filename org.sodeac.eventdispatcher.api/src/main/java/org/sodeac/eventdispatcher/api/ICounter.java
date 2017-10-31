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
 * Wrapper for io.dropwizard.metrics.Gauge
 * 
 * @see <a href="http://metrics.dropwizard.io/3.2.3/manual/core.html#counters">Coda Hale Metrics</a>
 * 
 * @author Sebastian Palarus
 *
 */
public interface ICounter
{
	/**
	 * Increment the counter by one.
	 */
	public void inc();
	
	/**
	 * Increment the counter by {@code n}.
	 *
	 * @param n the amount by which the counter will be increased
	 */
	public void inc(long n);
	
	/**
	 * Decrement the counter by one.
	 */
	public void dec();
	
	/**
	 * Decrement the counter by {@code n}.
	 *
	 * @param n the amount by which the counter will be decreased
	 */
	public void dec(long n);
	
	/**
	 * Returns the counter's current value.
	 *
	 * @return the counter's current value
	 */
	public long getCount();
}
