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
