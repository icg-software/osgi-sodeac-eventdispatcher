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
package org.sodeac.eventdispatcher.impl;

import org.sodeac.eventdispatcher.api.IGauge;

import com.codahale.metrics.Gauge;

public class SodeacGaugeWrapper<T> implements Gauge<T>,IGauge<T>
{
	private IGauge<T> gauge = null;
	
	public SodeacGaugeWrapper(IGauge<T> gauge)
	{
		super();
		this.gauge = gauge;
	}
	@Override
	public T getValue()
	{
		return this.gauge.getValue();
	}

}
