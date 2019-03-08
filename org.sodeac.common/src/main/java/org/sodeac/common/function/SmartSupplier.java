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
public class SmartSupplier<T> implements Supplier<T>
{
	private volatile int attemptCount = 1;
	private volatile int waitTimeValue = 1; 
	private volatile TimeUnit waitTimeUnit = TimeUnit.MILLISECONDS;
	private volatile T defaultValue = null;
	private Supplier<T> internSupplier = null;
	
	private volatile Integer attemptCountUseCached = null;
	private volatile Integer cacheExpireTimeValue = null; 
	private volatile TimeUnit cacheExpireTimeUnit = null;
	private volatile boolean containsCachedValue = false;
	private volatile T cachedValue = null;
	private volatile long cachedValueExpireTimestamp = 0;
	
	private SmartSupplier()
	{
		super();
	}
	
	/**
	 * create smart supplier
	 * 
	 * @param wrappedSupplier wrapped supplier
	 * 
	 * @return
	 */
	public static <T> SmartSupplier<T> forSupplier(Supplier<T> wrappedSupplier)
	{
		SmartSupplier<T> smartSupply = new SmartSupplier<>();
		smartSupply.internSupplier = wrappedSupplier == null ? () -> null : wrappedSupplier;
		return smartSupply;
	}
	
	/**
	 * instruct smart supplier to retry {@link Supplier#get()} multiple time, if wrapped supplier returns null-value 
	 * 
	 * @param attemptCount counts to attempt
	 * 
	 * @return smart supplier
	 */
	public SmartSupplier<T> withAttemptCount(int attemptCount)
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
	 * @return smart supplier
	 */
	public SmartSupplier<T> withWaitTimeForNextAttempt(int waitTimeValue,TimeUnit waitTimeUnit)
	{
		this.waitTimeValue = waitTimeValue;
		this.waitTimeUnit = waitTimeUnit;
		return this;
	}

	/**
	 * define default value, if supplier return no not-null-value 
	 * 
	 * @param defaultValue value to supply, if wrapped supplier return no not-null-value
	 * @return smart supplier
	 */
	public SmartSupplier<T> withDefaultValue(T defaultValue)
	{
		this.defaultValue = defaultValue;
		return this;
	}
	
	/**
	 * instruct to use cached value (if exists) after x times the wrapped supplier returns null-value 
	 * 
	 * @param failedAttemptCount how often wrapped supplier returns null-value until cached value will returns
	 * @return smart supplier
	 */
	public SmartSupplier<T> useCacheAfterFailedAttemptsCount(int failedAttemptCount)
	{
		this.attemptCountUseCached = failedAttemptCount;
		return this;
	}
	
	/**
	 * define expire time for cached value
	 * @param cacheExpireTimeValue expire time value for cached value
	 * @param cacheExpireTimeUnit expire time unit for cached value
	 * @return smart supplier
	 */
	public SmartSupplier<T> withCacheExpireTime(int cacheExpireTimeValue,TimeUnit cacheExpireTimeUnit)
	{
		if(cacheExpireTimeUnit == null)
		{
			cacheExpireTimeUnit = TimeUnit.SECONDS;
		}
		if(cacheExpireTimeValue < 1)
		{
			cacheExpireTimeValue = 1;
		}
		this.cacheExpireTimeValue = cacheExpireTimeValue;
		this.cacheExpireTimeUnit = cacheExpireTimeUnit;
		
		return this;
	}
	
	@Override
	public T get()
	{
		if(this.internSupplier == null)
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
		
		if((cachedValue != null) && (cachedValueExpireTimestamp <= System.currentTimeMillis()))
		{
			this.cachedValue = null;
			this.containsCachedValue = false;
		}
		
		
		T suppliedValue = null;
		int count = 0;
		while((suppliedValue == null) && (count < attemptCount))
		{
			
			suppliedValue = this.internSupplier.get();
			count++;
			
			if
			(
					(containsCachedValue) 
				&& 	(suppliedValue == null) 
				&& 	(this.attemptCountUseCached != null) 
				&& 	(this.attemptCountUseCached.intValue() <= count) 
			)
			{
				if(cachedValueExpireTimestamp > System.currentTimeMillis())
				{
					return this.cachedValue;
				}
				this.cachedValue = null;
				this.containsCachedValue = false;
			}
			
			if((suppliedValue == null) && (count < attemptCount))
			{
				try
				{
					Thread.sleep(TimeUnit.MILLISECONDS.convert(waitTimeValue,waitTimeUnit));
				}
				catch (Exception e) {}
			}
		}
		if( suppliedValue == null )
		{
			suppliedValue = defaultValue;
		}
		
		if(this.attemptCountUseCached != null)
		{
			this.cachedValue = suppliedValue;
			this.containsCachedValue = true;
			if(this.cacheExpireTimeValue != null)
			{
				this.cachedValue = suppliedValue;
				this.cachedValueExpireTimestamp = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(this.cacheExpireTimeValue,this.cacheExpireTimeUnit);
			}
			else
			{
				this.cachedValueExpireTimestamp = Long.MAX_VALUE;
			}
		}
		
		return suppliedValue;
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
