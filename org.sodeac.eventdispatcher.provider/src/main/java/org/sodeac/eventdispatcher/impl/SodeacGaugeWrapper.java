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
import org.sodeac.eventdispatcher.extension.api.IExtensibleGauge;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMetrics;

import com.codahale.metrics.Gauge;

public class SodeacGaugeWrapper<T> implements Gauge<T>,IGauge<T>,IExtensibleGauge<T>
{
	private IGauge<T> gauge = null;
	
	private String key;
	private String name;
	private MetricImpl metrics;
	
	
	public SodeacGaugeWrapper(IGauge<T> gauge,String key, String name, MetricImpl metric)
	{
		super();
		this.gauge = gauge;
		
		this.key = key;
		this.name = name;
		this.metrics = metric;
	}
	@Override
	public T getValue()
	{
		return this.gauge.getValue();
	}

	@Override
	public String getKey()
	{
		return this.key;
	}

	@Override
	public IExtensibleMetrics getMetrics()
	{
		return this.metrics;
	}

	@Override
	public String getName()
	{
		return this.name;
	}
}
