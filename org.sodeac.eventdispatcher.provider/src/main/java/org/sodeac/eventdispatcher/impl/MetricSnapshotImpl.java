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

import org.sodeac.eventdispatcher.api.IMetricSnapshot;

public class MetricSnapshotImpl implements IMetricSnapshot
{
	private com.codahale.metrics.Snapshot snapshot = null;
	
	public MetricSnapshotImpl(com.codahale.metrics.Snapshot snapshot)
	{
		super();
		this.snapshot = snapshot;
	}

	@Override
	public double getValue(double quantile)
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.getValue(quantile);
	}

	@Override
	public long[] getValues()
	{
		if(this.snapshot == null)
		{
			return new long[0];
		}
		return this.snapshot.getValues();
	}

	@Override
	public int size()
	{
		if(this.snapshot == null)
		{
			return 0;
		}
		return this.snapshot.size();
	}

	@Override
	public double getMedian()
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.getMedian();
	}

	@Override
	public double get75thPercentile()
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.get75thPercentile();
	}

	@Override
	public double get95thPercentile()
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.get95thPercentile();
	}

	@Override
	public double get98thPercentile()
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.get98thPercentile();
	}

	@Override
	public double get99thPercentile()
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.get99thPercentile();
	}

	@Override
	public double get999thPercentile()
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.get999thPercentile();
	}

	@Override
	public long getMax()
	{
		if(this.snapshot == null)
		{
			return 0L;
		}
		return this.snapshot.getMax();
	}

	@Override
	public double getMean()
	{
		if(this.snapshot == null)
		{
			return 0.0;
		}
		return this.snapshot.getMean();
	}

	@Override
	public long getMin()
	{
		if(this.snapshot == null)
		{
			return 0L;
		}
		return this.snapshot.getMin();
	}
}
