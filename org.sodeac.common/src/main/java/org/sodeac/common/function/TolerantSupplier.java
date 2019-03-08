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

/**
 * wrapper for {@link Supplier} with additional features
 * 
 * @author Sebastian Palarus
 *
 * @param <T> the type of results supplied by wrapped supplier
 */
public class TolerantSupplier<T> implements Supplier<T>
{
	private int attemptCount = 1;
	private int waitTimeValue = 1; 
	private TimeUnit waitTimeUnit = TimeUnit.MILLISECONDS;
	private T defaultValue = null;
	private Supplier<T> internSupplier = null;
	
	private TolerantSupplier()
	{
		super();
	}
	
	/**
	 * create tolerant supplier
	 * 
	 * @param wrappedSupplier wrapped supplier
	 * 
	 * @return
	 */
	public static <T> TolerantSupplier<T> forSupplier(Supplier<T> wrappedSupplier)
	{
		TolerantSupplier<T> tolerantSupply = new TolerantSupplier<>();
		tolerantSupply.internSupplier = wrappedSupplier == null ? () -> null : wrappedSupplier;
		return tolerantSupply;
	}
	
	/**
	 * instruct tolerant supplier to retry {@link Supplier#get()} multiple time, if wrapped supplier return no not-null-value 
	 * 
	 * @param attemptCount counts to attempt
	 * 
	 * @return tolerant supplier
	 */
	public TolerantSupplier<T> withAttemptCount(int attemptCount)
	{
		this.attemptCount = attemptCount;
		return this;
	}

	/**
	 * define wait time for next retry => @see {@link #withAttemptCount}
	 * 
	 * @param waitTimeValue wait time until next attempt to get next value
	 * @param waitTimeUnit wait time unit until next attempt to get next value
	 * 
	 * @return tolerant supplier
	 */
	public TolerantSupplier<T> withWaitTimeForNextAttempt(int waitTimeValue,TimeUnit waitTimeUnit)
	{
		this.waitTimeValue = waitTimeValue;
		this.waitTimeUnit = waitTimeUnit;
		return this;
	}

	/**
	 * define default value, if supplier return no not-null-value 
	 * 
	 * @param defaultValue value to supply, if wrapped supplier return no not-null-value
	 * @return
	 */
	public TolerantSupplier<T> withDefaultValue(T defaultValue)
	{
		this.defaultValue = defaultValue;
		return this;
	}
	
	@Override
	public T get()
	{
		return TolerantSupplier.supplyPeriodicallyOrDefault(this.internSupplier, this.attemptCount, this.waitTimeValue, this.waitTimeUnit, this.defaultValue);
	}

	/**
	 * get value from {@link Supplier} with multiple attempts
	 * 
	 * @param supplier wrapped supplier
	 * @param attemptCount count of attempts
	 * @param waitTimeValue wait time until next attempt to get next value
	 * @param waitTimeUnit wait time unit until next attempt to get next value
	 * @param defaultValue value to return, if supplier supplies no value
	 * 
	 * @return supplied value, or default value, if supplier supplied no value
	 */
	public static <T> T supplyPeriodicallyOrDefault(Supplier<T> supplier, int attemptCount, int waitTimeValue, TimeUnit waitTimeUnit, T defaultValue)
	{
		T suppliedValue = supplyPeriodically(supplier, attemptCount, waitTimeValue, waitTimeUnit);
		return suppliedValue == null ? defaultValue : suppliedValue;
	}
	
	/**
	 * get value from {@link Supplier} with multiple attempts
	 * 
	 * @param supplier wrapped supplier
	 * @param attemptCount count of attempts
	 * @param waitTimeValue wait time until next attempt to get next value
	 * @param waitTimeUnit wait time unit until next attempt to get next value
	 * 
	 * @return supplied value, or null, if supplier supplied no value
	 */
	public static <T> T supplyPeriodically(Supplier<T> supplier, int attemptCount, int waitTimeValue, TimeUnit waitTimeUnit)
	{
		if(supplier == null)
		{
			return null;
		}
		if(waitTimeValue < 1)
		{
			waitTimeValue = 1;
		}
		if(waitTimeUnit == null)
		{
			waitTimeUnit = TimeUnit.MILLISECONDS;
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
					Thread.sleep(TimeUnit.MILLISECONDS.convert(waitTimeValue,waitTimeUnit));
				}
				catch (Exception e) {}
			}
		}
		return suppliedValue;
	}
}
