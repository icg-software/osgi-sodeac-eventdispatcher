package org.sodeac.common.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

import org.junit.Test;

public class ConplierBeanTest
{
	@Test
	public void test1()
	{
		ConplierBean<String> conplier = new ConplierBean<String>("val");
		Supplier<String> supplier = () -> conplier.get();
		assertEquals("supplied value should be correct", "val", supplier.get());
	}
	
	@Test
	public void test2()
	{
		ConplierBean<String> conplier = new ConplierBean<String>();
		conplier.accept("val");
		assertEquals("supplied value should be correct", "val", conplier.getValue());		
	}
	
	@Test
	public void test3()
	{
		ConplierBean<String> conplier = new ConplierBean<String>();
		Collections.singletonList("val").stream().forEach(conplier);
		assertEquals("supplied value should be correct", "val", conplier.getValue());
		assertTrue("test should returns correct value", conplier.test(s -> "val".equals(s)));
		assertFalse("test should returns correct value", conplier.test(s -> "xxx".equals(s)));
	}
	
	@Test
	public void test4()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
		UnaryOperator<Integer> increment = i -> ++i;
		conplier.unaryOperate(increment);
		assertEquals("operated value should be correct", new Integer(43), conplier.getValue());
	}
	
	@Test
	public void test5()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
		assertEquals("operated value should be correct", new Integer(43), conplier.unaryOperate(i -> ++i));
		assertEquals("operated value should be correct", new Integer(44), conplier.unaryOperate(i -> ++i));
	}
	
	@Test
	public void test6()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
		BinaryOperator<Integer> add = (i,p) -> i+p;
		conplier.binaryOperate(add, 10);
		assertEquals("value should be correct", new Integer(52), conplier.get());
	}
	
	@Test
	public void test7()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
		assertEquals("value should be correct", new Integer(52), conplier.binaryOperate((i,p) -> i+p, 10));
		assertEquals("value should be correct", new Integer(62), conplier.binaryOperate((i,p) -> i+p, 10));
	}
	
	@Test
	public void test8()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(42);
		ConplierBean<Boolean> consumed = new ConplierBean<Boolean>(false);
		
		conplier.supply((v) ->
		{
			consumed.setValue(true);
			assertEquals("value should be correct", 42, v.intValue());
		});
		
		assertTrue("consumed flag should be correct",consumed.get());
	}
	
	@Test
	public void test9()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(1);
		
		assertEquals("value should be correct", 1, conplier.get().intValue());
		conplier.consume(() -> 42);
		assertEquals("value should be correct", 42, conplier.get().intValue());
	}
	
	@Test
	public void test11()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>();
		assertFalse("optional should be not present", conplier.getOptional().isPresent());
		
		conplier.accept(42);
		assertTrue("optional should be present", conplier.getOptional().isPresent());
		assertEquals("options.get should be correct", 42, conplier.getOptional().get().intValue());
		
		conplier.accept(null);
		assertFalse("optional should be not present", conplier.getOptional().isPresent());
		
		boolean throwsNSEE = false;
		try
		{
			conplier.getOptional().get();
		}
		catch (java.util.NoSuchElementException e) 
		{
			throwsNSEE = true;
		}
		
		assertTrue("optional should throws oSuchElementException", throwsNSEE);
	}
	
	@Test
	public void test100()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(1);
		ConplierBean<Integer> mirror = new ConplierBean<Integer>(-1);
		
		assertEquals("value should be correct", 1, conplier.get().intValue());
		assertEquals("value should be correct", -1, mirror.get().intValue());
		
		PropertyChangeListener pcl = new PropertyChangeListener()
		{
			
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				assertEquals("value should be correct", 1, ((Integer)evt.getOldValue()).intValue());
				assertEquals("value should be correct", 42, ((Integer)evt.getNewValue()).intValue());
				
				mirror.setValue((Integer)evt.getNewValue());
			}
		};
		conplier.addPropertyChangeListener(pcl);
		
		conplier.setValue(42);
		
		assertEquals("value should be correct", 42, conplier.get().intValue());
		assertEquals("value should be correct", 42, mirror.get().intValue());
		
		conplier.removePropertyChangeListener(pcl);
		
		conplier.setValue(1);
		
		assertEquals("value should be correct", 1, conplier.get().intValue());
		assertEquals("value should be correct", 42, mirror.get().intValue());
		
		
	}
	
	@Test
	public void test101()
	{
		ConplierBean<Integer> conplier = new ConplierBean<Integer>(1);
		
		assertEquals("value should be correct", 1, conplier.get().intValue());
		
		ConplierBean<Boolean> consumed = new ConplierBean<Boolean>(false);
		
		ConplierBean<Integer>.ConsumeOnChangeListener pcl = conplier.consumeOnChange
		(
			new Consumer<Integer>()
			{
				
				@Override
				public void accept(Integer t)
				{
					consumed.setValue(true);
					assertEquals("value should be correct", 42,t.intValue());
				}
			}
		);
		
		assertFalse("value should be not consumed",consumed.get());
		conplier.setValue(42);
		assertTrue("value should be consumed",consumed.get());
		
		consumed.setValue(false);
		pcl.unregister();
		conplier.setValue(1);
		assertFalse("value should be not consumed",consumed.get());
		
		
		
	}
}
