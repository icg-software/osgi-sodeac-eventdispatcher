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
		meter.mark();
	}

	@Override
	public void mark(long n)
	{
		meter.mark(n);
	}

	@Override
	public long getCount()
	{
		return this.meter.getCount();
	}

	@Override
	public double getMeanRate()
	{
		return this.meter.getMeanRate();
	}

	@Override
	public double getOneMinuteRate()
	{
		return this.meter.getOneMinuteRate();
	}

	@Override
	public double getFiveMinuteRate()
	{
		return this.meter.getFiveMinuteRate();
	}

	@Override
	public double getFifteenMinuteRate()
	{
		return this.meter.getFifteenMinuteRate();
	}

}
