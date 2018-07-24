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
package org.sodeac.eventdispatcher.impl;

import org.sodeac.eventdispatcher.api.ICounter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMetrics;

import com.codahale.metrics.Counter;

public class CounterImpl implements ICounter, IExtensibleCounter
{
	private Counter counter;
	private String key;
	private String name;
	private MetricImpl metrics;
	
	public CounterImpl(Counter counter, String key, String name, MetricImpl metric)
	{
		super();
		this.counter = counter;
		this.key = key;
		this.name = name;
		this.metrics = metric;
	}

	@Override
	public void inc()
	{
		if(this.counter != null)
		{
			this.counter.inc();
			metrics.updateCounter(this);
		}
	}

	@Override
	public void inc(long n)
	{
		if(this.counter != null)
		{
			this.counter.inc(n);
			metrics.updateCounter(this);
		}
	}

	@Override
	public void dec()
	{
		if(this.counter != null)
		{
			this.counter.dec();
			metrics.updateCounter(this);
		}
	}

	@Override
	public void dec(long n)
	{
		if(this.counter != null)
		{
			this.counter.dec(n);
			metrics.updateCounter(this);
		}
	}

	@Override
	public long getCount()
	{
		if(this.counter != null)
		{
			return this.counter.getCount();
		}
		return 0L;
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
