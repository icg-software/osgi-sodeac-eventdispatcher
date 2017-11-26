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

import com.codahale.metrics.Histogram;

public class HistogramImpl implements IHistogram
{
	private Histogram histogram;
	
	public HistogramImpl(Histogram histogram)
	{
		super();
		this.histogram = histogram;
	}

	@Override
	public void update(int value)
	{
		this.histogram.update(value);
	}

	@Override
	public void update(long value)
	{
		this.histogram.update(value);
	}

	@Override
	public long getCount()
	{
		return this.histogram.getCount();
	}

	@Override
	public IMetricSnapshot getSnapshot()
	{
		return new MetricSnapshotImpl(this.histogram.getSnapshot());
	}

}
