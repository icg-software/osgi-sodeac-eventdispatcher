package org.sodeac.common.function;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;

public class SmartSupplierTest
{
	
	@Test
	public void test11()
	{
		int i = 1;
		int x = SmartSupplier.forSupplier(() -> i).get();
		assertEquals("x should be corrext",i, x);
	}
	
	
	@Test
	public void test12()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		Integer x =  SmartSupplier.forSupplier(i).get();
		assertEquals("x should be corrext",i.get(), x);
	}
	
	@Test
	public void test13()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		Integer x = SmartSupplier.forSupplier(i)
					.withAttemptCount(3)
					.withWaitTimeForNextAttempt(1, TimeUnit.SECONDS)
					.withDefaultValue(5)
					.get();
		assertEquals("x should be corrext",Integer.valueOf(5), x);
	}
	
	@Test
	public void test14()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		
		new Thread(() -> 
		{ 
			try{Thread.sleep(1000);}catch (Exception e) {}
			i.setValue(5);
		}).start();
		
		Integer x = SmartSupplier.forSupplier(i)
					.withAttemptCount(3)
					.withWaitTimeForNextAttempt(1, TimeUnit.SECONDS)
					.withDefaultValue(3)
					.get();
		assertEquals("x should be corrext",Integer.valueOf(5), x);
	}
	
	@Test
	public void test15()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		
		new Thread(() -> 
		{ 
			try{Thread.sleep(4000);}catch (Exception e) {}
			i.setValue(5);
		}).start();
		
		Integer x = SmartSupplier.forSupplier(i)
					.withAttemptCount(3)
					.withWaitTimeForNextAttempt(1, TimeUnit.SECONDS)
					.withDefaultValue(3)
					.get();
		assertEquals("x should be corrext",Integer.valueOf(3), x);
	}
	
	@Test
	public void test20()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(10);
		
		Supplier<Integer> tolerantSupplier = SmartSupplier.forSupplier(i)
				.withAttemptCount(3)
				.withWaitTimeForNextAttempt(1, TimeUnit.SECONDS)
				.withDefaultValue(3)
				.useCacheAfterFailedAttemptsCount(1)
				.withCacheExpireTime(5, TimeUnit.SECONDS);
		
		assertEquals("supplier should supply corrext",Integer.valueOf(10), tolerantSupplier.get());
		
		i.setValue(null);
		assertEquals("supplier should supply corrext",Integer.valueOf(10), tolerantSupplier.get());
		
		try{Thread.sleep(2000);}catch(Exception e){}
		
		assertEquals("supplier should supply corrext",Integer.valueOf(10), tolerantSupplier.get());
		
		try{Thread.sleep(5000);}catch(Exception e){}
		
		assertEquals("supplier should supply corrext",Integer.valueOf(3), tolerantSupplier.get());
		
	}
	
}
