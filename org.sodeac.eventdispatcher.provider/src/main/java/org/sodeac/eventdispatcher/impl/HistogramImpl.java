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
	public Snapshot getSnapshot()
	{
		return new Snapshot(this.histogram.getSnapshot());
	}
	
	private static final class Snapshot implements IHistogram.Snapshot
	{
		private com.codahale.metrics.Snapshot snapshot = null;
		
		private Snapshot(com.codahale.metrics.Snapshot snapshot)
		{
			super();
			this.snapshot = snapshot;
		}

		@Override
		public double getValue(double quantile)
		{
			return this.snapshot.getValue(quantile);
		}

		@Override
		public long[] getValues()
		{
			return this.snapshot.getValues();
		}

		@Override
		public int size()
		{
			return this.snapshot.size();
		}

		@Override
		public double getMedian()
		{
			return this.snapshot.getMedian();
		}

		@Override
		public double get75thPercentile()
		{
			return this.snapshot.get75thPercentile();
		}

		@Override
		public double get95thPercentile()
		{
			return this.snapshot.get95thPercentile();
		}

		@Override
		public double get98thPercentile()
		{
			return this.snapshot.get98thPercentile();
		}

		@Override
		public double get99thPercentile()
		{
			return this.snapshot.get99thPercentile();
		}

		@Override
		public double get999thPercentile()
		{
			return this.snapshot.get999thPercentile();
		}

		@Override
		public long getMax()
		{
			return this.snapshot.getMax();
		}

		@Override
		public double getMean()
		{
			return this.snapshot.getMean();
		}

		@Override
		public long getMin()
		{
			return this.snapshot.getMin();
		}
		
	}

}
