/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.runner.providertests;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.components.rescheduleservice.RescheduleTestService;
import org.sodeac.eventdispatcher.itest.components.scope.ScopeTestSimpleManagementController;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RescheduleServiceTest extends AbstractTest
{
	@Inject
	private BundleContext bundleContext;
	
	@Inject
	private IEventDispatcher eventDispatcher;
	
	@Inject
	private EventAdmin eventAdmin;
	
	@Configuration
	public Option[] config() 
	{
		return super.config();
	}
	
	@Before
	public void setUp() {}
    
	@Test
	public void test00ComponentInstance() 
	{
		super.waitQueueIsUp(eventDispatcher, ScopeTestSimpleManagementController.QUEUE_ID, 3000);
		assertNotNull("bundleContext should not be null" ,bundleContext);
		System.out.println("\n\n");
		Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) 
        {
            System.out.println("[INFO]\t\tbundle " + bundle + ": " + bundle.getHeaders().get(Constants.BUNDLE_VERSION) + " " + getBundleStateName(bundle.getState()));
        }
        System.out.println("\n");
        
		assertNotNull("EventAdmin should not be null" ,eventAdmin);
		assertNotNull("eventDispatcher should not be null" ,eventDispatcher);
	}
	
	@Test(timeout=10000)
	public void test01() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		long index = 0;
		
		for(int i = 0; i < 100; i++)
		{
			index++;
			
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(RescheduleTestService.PROPERTY_DATA_VALUE,index);
			event =  new Event(RescheduleTestService.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
			
		}
		
		try
		{
			Thread.sleep(RescheduleTestService.TOLERANCE);
		}
		catch (Exception e) {}
		assertEquals("dataobject should equals index", index, dataObject.get());
	}
	
	@Test(timeout=20000)
	public void test02() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 100; x++)
		{
			for(int i = 0; i < 10; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(RescheduleTestService.PROPERTY_DATA_VALUE,index);
				event =  new Event(RescheduleTestService.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
				
			}
			
			try
			{
				Thread.sleep(RescheduleTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test03() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 100; x++)
		{
			for(int i = 0; i < 10; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(RescheduleTestService.PROPERTY_DATA_VALUE,index);
				event =  new Event(RescheduleTestService.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(event, RescheduleTestService.QUEUE_ID);
				
			}
			
			try
			{
				Thread.sleep(RescheduleTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test04() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 100; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(RescheduleTestService.PROPERTY_DATA_VALUE,index);
			event =  new Event(RescheduleTestService.DATA_VALUE_EVENT,eventProperties);
			eventDispatcher.schedule(event, RescheduleTestService.QUEUE_ID);
							
			try
			{
				Thread.sleep(RescheduleTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test05() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 100; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(RescheduleTestService.PROPERTY_DATA_VALUE,index);
				event =  new Event(RescheduleTestService.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(event, RescheduleTestService.QUEUE_ID);
				
			}
			
			try
			{
				Thread.sleep(RescheduleTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
}
