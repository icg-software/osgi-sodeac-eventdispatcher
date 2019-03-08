package org.sodeac.common.function;

import static org.junit.Assert.assertEquals;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class TolerantSupplierTest
{
	
	@Test
	public void test01()
	{
		int i = 1;
		int x = TolerantSupplier.supplyPeriodically(() -> i , 1, 1,TimeUnit.MILLISECONDS);
		assertEquals("x should be corrext",i, x);
	}
	
	
	@Test
	public void test02()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		Integer x = TolerantSupplier.supplyPeriodically(i , 1, 1,TimeUnit.MILLISECONDS);
		assertEquals("x should be corrext",i.get(), x);
	}
	
	@Test
	public void test03()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		Integer x = TolerantSupplier.supplyPeriodicallyOrDefault(i , 3, 1,TimeUnit.SECONDS,5);
		assertEquals("x should be corrext",Integer.valueOf(5), x);
	}
	
	@Test
	public void test04()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		
		new Thread(() -> 
		{ 
			try{Thread.sleep(1000);}catch (Exception e) {}
			i.setValue(5);
		}).start();
		
		Integer x = TolerantSupplier.supplyPeriodicallyOrDefault(i , 3, 1,TimeUnit.SECONDS,3);
		assertEquals("x should be corrext",Integer.valueOf(5), x);
	}
	
	@Test
	public void test05()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		
		new Thread(() -> 
		{ 
			try{Thread.sleep(3000);}catch (Exception e) {}
			i.setValue(5);
		}).start();
		
		Integer x = TolerantSupplier.supplyPeriodicallyOrDefault(i , 3, 1,TimeUnit.SECONDS,3);
		assertEquals("x should be corrext",Integer.valueOf(3), x);
	}
	
	@Test
	public void test11()
	{
		int i = 1;
		int x = TolerantSupplier.forSupplier(() -> i).get();
		assertEquals("x should be corrext",i, x);
	}
	
	
	@Test
	public void test12()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		Integer x =  TolerantSupplier.forSupplier(i).get();
		assertEquals("x should be corrext",i.get(), x);
	}
	
	@Test
	public void test13()
	{
		ConplierBean<Integer> i = new ConplierBean<Integer>(null);
		Integer x = TolerantSupplier.forSupplier(i)
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
		
		Integer x = TolerantSupplier.forSupplier(i)
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
			try{Thread.sleep(3000);}catch (Exception e) {}
			i.setValue(5);
		}).start();
		
		Integer x = TolerantSupplier.forSupplier(i)
					.withAttemptCount(3)
					.withWaitTimeForNextAttempt(1, TimeUnit.SECONDS)
					.withDefaultValue(3)
					.get();
		assertEquals("x should be corrext",Integer.valueOf(3), x);
	}
	
	
}
