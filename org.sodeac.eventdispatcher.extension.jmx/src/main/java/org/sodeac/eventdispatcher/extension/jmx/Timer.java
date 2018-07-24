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

import org.sodeac.eventdispatcher.extension.api.IExtensibleTimer;

public class Timer implements TimerMBean
{
	protected ObjectName timerObjectName = null;
	private IExtensibleTimer timer = null;
	public Timer(ObjectName timerObjectName, IExtensibleTimer timer)
	{
		super();
		this.timerObjectName = timerObjectName;
		this.timer = timer;
	}
	
	@Override
	public String getName()
	{
		return this.timer.getName();
	}
	
	@Override
	public String getKey()
	{
		return this.timer.getKey();
	}

	
	@Override
	public double getMeanRate()
	{
		return this.timer.getMeanRate();
	}


	@Override
	public double getOneMinuteRate()
	{
		return this.timer.getOneMinuteRate();
	}


	@Override
	public double getFiveMinuteRate()
	{
		return this.timer.getFiveMinuteRate();
	}


	@Override
	public double getFifteenMinuteRate()
	{
		return this.timer.getFifteenMinuteRate();
	}

	@Override
	public long getCount()
	{
		return this.timer.getCount();
	}

	@Override
	public double getValue(double quantile)
	{
		return this.timer.getSnapshot().getValue(quantile);
	}

	@Override
	public int size()
	{
		return this.timer.getSnapshot().size();
	}

	@Override
	public double getMedian()
	{
		return this.timer.getSnapshot().getMedian();
	}

	@Override
	public double get75thPercentile()
	{
		return this.timer.getSnapshot().get75thPercentile();
	}

	@Override
	public double get95thPercentile()
	{
		return this.timer.getSnapshot().get95thPercentile();
	}

	@Override
	public double get98thPercentile()
	{
		return this.timer.getSnapshot().get98thPercentile();
	}

	@Override
	public double get99thPercentile()
	{
		return this.timer.getSnapshot().get99thPercentile();
	}

	@Override
	public double get999thPercentile()
	{
		return this.timer.getSnapshot().get999thPercentile();
	}

	@Override
	public long getMax()
	{
		return this.timer.getSnapshot().getMax();
	}

	@Override
	public double getMean()
	{
		return this.timer.getSnapshot().getMean();
	}

	@Override
	public long getMin()
	{
		return this.timer.getSnapshot().getMin();
	}	
}
