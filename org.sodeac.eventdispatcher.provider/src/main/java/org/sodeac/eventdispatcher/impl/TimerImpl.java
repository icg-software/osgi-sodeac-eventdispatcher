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
import org.sodeac.eventdispatcher.extension.api.IExtensibleMetrics;
import org.sodeac.eventdispatcher.extension.api.IExtensibleTimer;

import com.codahale.metrics.Timer;

public class TimerImpl implements ITimer,IExtensibleTimer
{
	private Timer timer;
	
	private String key;
	private String name;
	private MetricImpl metrics;
	
	public TimerImpl(Timer timer,String key, String name, MetricImpl metric)
	{
		super();
		this.timer = timer;
		
		this.key = key;
		this.name = name;
		this.metrics = metric;
	}
	
	@Override
	public void update(long duration, TimeUnit unit)
	{
		if(this.timer != null)
		{
			this.timer.update(duration, unit);
			this.metrics.updateTimer(this);
		}
	}

	@Override
	public Context time()
	{
		if(this.timer == null)
		{
			return new Context(null,null);
		}
		return new Context(this.timer.time(),this);
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
		private TimerImpl timer;
		
		private Context(Timer.Context ctx,TimerImpl timer)
		{
			super();
			this.ctx = ctx;
			this.timer = timer;
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
				long time = ctx.stop();
				this.timer.metrics.updateTimer(this.timer);
				return time;
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
