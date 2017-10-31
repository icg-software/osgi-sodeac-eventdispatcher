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
 * 
 * Wrapper for io.dropwizard.metrics.Gauge
 * 
 * @see <a href="http://metrics.dropwizard.io/3.2.3/manual/core.html#gauges">Coda Hale Metrics</a>
 *  
 * @author Sebastian Palarus
 *
 * @param <T> the type of the metric's value
 */
public interface IGauge<T>
{
	
	/**
	 * Returns the metric's current value.
	 *
	 * @return the metric's current value
 	 */
	public T getValue();
}
