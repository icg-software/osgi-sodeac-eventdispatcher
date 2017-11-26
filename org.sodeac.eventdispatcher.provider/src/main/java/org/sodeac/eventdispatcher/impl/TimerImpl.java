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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.sodeac.eventdispatcher.api.IMetricSnapshot;
import org.sodeac.eventdispatcher.api.ITimer;

import com.codahale.metrics.Timer;

public class TimerImpl implements ITimer
{
	private Timer timer;
	
	public TimerImpl(Timer timer)
	{
		super();
		this.timer = timer;
	}
	
	@Override
	public void update(long duration, TimeUnit unit)
	{
		this.timer.update(duration, unit);
	}

	@Override
	public Context time()
	{
		return new Context(this.timer.time());
	}

	@Override
	public long getCount()
	{
		return this.timer.getCount();
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
	
	private static final class Context implements ITimer.Context
	{
		private Timer.Context ctx;
		
		private Context(Timer.Context ctx)
		{
			super();
			this.ctx = ctx;
		}

		@Override
		public void close() throws IOException
		{
			ctx.close();
		}

		@Override
		public long stop()
		{
			return ctx.stop();
		}
	}
	
	@Override
	public IMetricSnapshot getSnapshot()
	{
		return new MetricSnapshotImpl(this.timer.getSnapshot());
	}

}
