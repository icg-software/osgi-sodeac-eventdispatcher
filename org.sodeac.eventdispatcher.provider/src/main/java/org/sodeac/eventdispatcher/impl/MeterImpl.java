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

import org.sodeac.eventdispatcher.api.IMeter;

import com.codahale.metrics.Meter;

public class MeterImpl implements IMeter
{

	private Meter meter = null;
	
	public MeterImpl(Meter meter)
	{
		super();
		this.meter = meter;
	}
	
	@Override
	public void mark()
	{
		if(this.meter != null)
		{
			this.meter.mark();
		}
	}

	@Override
	public void mark(long n)
	{
		if(this.meter != null)
		{
			this.meter.mark(n);
		}
	}

	@Override
	public long getCount()
	{
		if(this.meter == null)
		{
			return 0L;
		}
		return this.meter.getCount();
	}

	@Override
	public double getMeanRate()
	{
		if(this.meter == null)
		{
			return 0.0;
		}
		return this.meter.getMeanRate();
	}

	@Override
	public double getOneMinuteRate()
	{
		if(this.meter == null)
		{
			return 0.0;
		}
		return this.meter.getOneMinuteRate();
	}

	@Override
	public double getFiveMinuteRate()
	{
		if(this.meter == null)
		{
			return 0.0;
		}
		return this.meter.getFiveMinuteRate();
	}

	@Override
	public double getFifteenMinuteRate()
	{
		if(this.meter == null)
		{
			return 0.0;
		}
		return this.meter.getFifteenMinuteRate();
	}

}
