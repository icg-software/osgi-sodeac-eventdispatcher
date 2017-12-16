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
		if(this.timer != null)
		{
			this.timer.update(duration, unit);
		}
	}

	@Override
	public Context time()
	{
		if(this.timer == null)
		{
			return new Context(null);
		}
		return new Context(this.timer.time());
	}

	@Override
	public long getCount()
	{
		if(this.timer == null)
		{
			return 0L;
		}
		return this.timer.getCount();
	}

	@Override
	public double getMeanRate()
	{
		if(this.timer == null)
		{
			return 0.0;
		}
		return this.timer.getMeanRate();
	}

	@Override
	public double getOneMinuteRate()
	{
		if(this.timer == null)
		{
			return 0.0;
		}
		return this.timer.getOneMinuteRate();
	}

	@Override
	public double getFiveMinuteRate()
	{
		if(this.timer == null)
		{
			return 0.0;
		}
		return this.timer.getFiveMinuteRate();
	}

	@Override
	public double getFifteenMinuteRate()
	{
		if(this.timer == null)
		{
			return 0.0;
		}
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
			if(ctx != null)
			{
				ctx.close();
			}
		}

		@Override
		public long stop()
		{
			if(ctx != null)
			{
				return ctx.stop();
			}
			return 0L;
		}
	}
	
	@Override
	public IMetricSnapshot getSnapshot()
	{
		if(this.timer ==  null)
		{
			return new MetricSnapshotImpl(null);
		}
		return new MetricSnapshotImpl(this.timer.getSnapshot());
	}

}
