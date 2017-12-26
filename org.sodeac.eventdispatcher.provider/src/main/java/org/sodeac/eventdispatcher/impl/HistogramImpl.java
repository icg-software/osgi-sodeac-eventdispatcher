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

import org.sodeac.eventdispatcher.api.IHistogram;
import org.sodeac.eventdispatcher.api.IMetricSnapshot;
import org.sodeac.eventdispatcher.extension.api.IExtensibleHistogram;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMetrics;

import com.codahale.metrics.Histogram;

public class HistogramImpl implements IHistogram,IExtensibleHistogram
{
	private Histogram histogram;
	private String key;
	private String name;
	private MetricImpl metrics;
	
	public HistogramImpl(Histogram histogram,String key, String name, MetricImpl metric)
	{
		super();
		this.histogram = histogram;
		
		this.key = key;
		this.name = name;
		this.metrics = metric;
	}

	@Override
	public void update(int value)
	{
		if(this.histogram != null)
		{
			this.histogram.update(value);
			this.metrics.updateHistogram(this);
		}
	}

	@Override
	public void update(long value)
	{
		if(this.histogram != null)
		{
			this.histogram.update(value);
			this.metrics.updateHistogram(this);
		}
	}

	@Override
	public long getCount()
	{
		if(this.histogram != null)
		{
			return this.histogram.getCount();
		}
		return 0L;
	}

	@Override
	public IMetricSnapshot getSnapshot()
	{
		if(this.histogram != null)
		{
			return new MetricSnapshotImpl(this.histogram.getSnapshot());
		}
		return new MetricSnapshotImpl(null);
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
