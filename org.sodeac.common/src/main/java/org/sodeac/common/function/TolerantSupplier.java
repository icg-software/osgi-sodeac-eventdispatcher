/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.common.function;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TolerantSupplier<T> implements Supplier<T>
{
	private int attemptCount = 1;
	private int timeToSleepValue = 1; 
	private TimeUnit timeToSleepUnit = TimeUnit.MILLISECONDS;
	private T defaultValue = null;
	private Supplier<T> internSupplier = null;
	
	private TolerantSupplier()
	{
		super();
	}
	
	public static <T> TolerantSupplier<T> forSupplier(Supplier<T> internSupplier)
	{
		TolerantSupplier<T> tolerantSupply = new TolerantSupplier<>();
		tolerantSupply.internSupplier = internSupplier == null ? () -> null : internSupplier;
		return tolerantSupply;
	}
	
	public TolerantSupplier<T> withAttemptCount(int attemptCount)
	{
		this.attemptCount = attemptCount;
		return this;
	}

	public TolerantSupplier<T> withTimeToSleepValue(int timeToSleepValue,TimeUnit timeToSleepUnit)
	{
		this.timeToSleepValue = timeToSleepValue;
		this.timeToSleepUnit = timeToSleepUnit;
		return this;
	}

	public TolerantSupplier<T> withDefaultValue(T defaultValue)
	{
		this.defaultValue = defaultValue;
		return this;
	}
	
	@Override
	public T get()
	{
		return TolerantSupplier.supplyPeriodicallyOrDefault(this.internSupplier, this.attemptCount, this.timeToSleepValue, this.timeToSleepUnit, this.defaultValue);
	}

	public static <T> T supplyPeriodicallyOrDefault(Supplier<T> supplier, int attemptCount, int timeToSleepValue, TimeUnit timeToSleepUnit, T defaultValue)
	{
		T suppliedValue = supplyPeriodically(supplier, attemptCount, timeToSleepValue, timeToSleepUnit);
		return suppliedValue == null ? defaultValue : suppliedValue;
	}
	
	public static <T> T supplyPeriodically(Supplier<T> supplier, int attemptCount, int timeToSleepValue, TimeUnit timeToSleepUnit)
	{
		if(supplier == null)
		{
			return null;
		}
		if(timeToSleepValue < 1)
		{
			timeToSleepValue = 1;
		}
		if(timeToSleepUnit == null)
		{
			timeToSleepUnit = TimeUnit.MILLISECONDS;
		}
		
		T suppliedValue = null;
		while((suppliedValue == null) && (attemptCount > 0))
		{
			attemptCount--;
			suppliedValue = supplier.get();
			if((suppliedValue == null) && (attemptCount > 0))
			{
				try
				{
					Thread.sleep(TimeUnit.MILLISECONDS.convert(timeToSleepValue,timeToSleepUnit));
				}
				catch (Exception e) {}
			}
		}
		return suppliedValue;
	}
}
