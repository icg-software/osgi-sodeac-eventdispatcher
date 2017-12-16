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

import org.sodeac.eventdispatcher.api.ICounter;

import com.codahale.metrics.Counter;

public class CounterImpl implements ICounter
{
	private Counter counter;
	
	public CounterImpl(Counter counter)
	{
		super();
		this.counter = counter;
	}

	@Override
	public void inc()
	{
		if(this.counter != null)
		{
			this.counter.inc();
		}
	}

	@Override
	public void inc(long n)
	{
		if(this.counter != null)
		{
			this.counter.inc(n);
		}
	}

	@Override
	public void dec()
	{
		if(this.counter != null)
		{
			this.counter.dec();
		}
	}

	@Override
	public void dec(long n)
	{
		if(this.counter != null)
		{
			this.counter.dec(n);
		}
	}

	@Override
	public long getCount()
	{
		if(this.counter != null)
		{
			return this.counter.getCount();
		}
		return 0L;
	}

}
