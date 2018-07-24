/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.extension.jmx;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.extension.api.IExtensibleHistogram;

public class Histogram implements HistogramMBean
{
	protected ObjectName histogramObjectName = null;
	private IExtensibleHistogram histogram = null;
	public Histogram(ObjectName histogramObjectName, IExtensibleHistogram histogram)
	{
		super();
		this.histogramObjectName = histogramObjectName;
		this.histogram = histogram;
	}
	
	
	@Override
	public String getName()
	{
		return this.histogram.getName();
	}
	
	@Override
	public String getKey()
	{
		return this.histogram.getKey();
	}


	@Override
	public long getCount()
	{
		return this.histogram.getCount();
	}

	@Override
	public double getValue(double quantile)
	{
		return this.histogram.getSnapshot().getValue(quantile);
	}

	@Override
	public int size()
	{
		return this.histogram.getSnapshot().size();
	}

	@Override
	public double getMedian()
	{
		return this.histogram.getSnapshot().getMedian();
	}

	@Override
	public double get75thPercentile()
	{
		return this.histogram.getSnapshot().get75thPercentile();
	}

	@Override
	public double get95thPercentile()
	{
		return this.histogram.getSnapshot().get95thPercentile();
	}

	@Override
	public double get98thPercentile()
	{
		return this.histogram.getSnapshot().get98thPercentile();
	}

	@Override
	public double get99thPercentile()
	{
		return this.histogram.getSnapshot().get99thPercentile();
	}

	@Override
	public double get999thPercentile()
	{
		return this.histogram.getSnapshot().get999thPercentile();
	}

	@Override
	public long getMax()
	{
		return this.histogram.getSnapshot().getMax();
	}

	@Override
	public double getMean()
	{
		return this.histogram.getSnapshot().getMean();
	}

	@Override
	public long getMin()
	{
		return this.histogram.getSnapshot().getMin();
	}	
}
