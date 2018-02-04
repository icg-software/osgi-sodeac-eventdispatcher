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
import org.sodeac.eventdispatcher.itest.components.rescheduleservice.ReReScheduleJobInWorkTestService1;
import org.sodeac.eventdispatcher.itest.components.rescheduleservice.ReReScheduleJobInWorkTestService2;
import org.sodeac.eventdispatcher.itest.components.rescheduleservice.ReReScheduleJobInWorkTestService3;
import org.sodeac.eventdispatcher.itest.components.rescheduleservice.ReReScheduleJobInWorkTestService4;
import org.sodeac.eventdispatcher.itest.components.rescheduleservice.ReReScheduleJobInWorkTestService5;
import org.sodeac.eventdispatcher.itest.components.rescheduleservice.RescheduleJobByOnEventScheduledTestService;
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
	
	// Start RescheduleJobByOnEventScheduledTestService
	
	@Test(timeout=10000)
	public void test01RescheduleJobByOnEventScheduledTestService() 
	{
		System.out.println("\nTests for RescheduleJobByOnEventScheduledTestService");
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleJobByOnEventScheduledTestService.SHARED_OBJECT_EVENT,eventProperties);
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
			eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_VALUE,index);
			event =  new Event(RescheduleJobByOnEventScheduledTestService.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
			
		}
		
		try
		{
			Thread.sleep(RescheduleJobByOnEventScheduledTestService.TOLERANCE);
		}
		catch (Exception e) {}
		assertEquals("dataobject should equals index", index, dataObject.get());
	}
	
	@Test(timeout=20000)
	public void test02RescheduleJobByOnEventScheduledTestService() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleJobByOnEventScheduledTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_VALUE,index);
				event =  new Event(RescheduleJobByOnEventScheduledTestService.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
				
			}
			
			try
			{
				Thread.sleep(RescheduleJobByOnEventScheduledTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test03RescheduleJobByOnEventScheduledTestService() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleJobByOnEventScheduledTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_VALUE,index);
				event =  new Event(RescheduleJobByOnEventScheduledTestService.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(RescheduleJobByOnEventScheduledTestService.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(RescheduleJobByOnEventScheduledTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test04RescheduleJobByOnEventScheduledTestService() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleJobByOnEventScheduledTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_VALUE,index);
				event =  new Event(RescheduleJobByOnEventScheduledTestService.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
			}
			
			try
			{
				Thread.sleep(RescheduleJobByOnEventScheduledTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test05RescheduleJobByOnEventScheduledTestService() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleJobByOnEventScheduledTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_VALUE,index);
				event =  new Event(RescheduleJobByOnEventScheduledTestService.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(RescheduleJobByOnEventScheduledTestService.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(RescheduleJobByOnEventScheduledTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test06RescheduleJobByOnEventScheduledTestService() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleJobByOnEventScheduledTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_VALUE,index);
			event =  new Event(RescheduleJobByOnEventScheduledTestService.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
							
			try
			{
				Thread.sleep(RescheduleJobByOnEventScheduledTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test07RescheduleJobByOnEventScheduledTestService() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(RescheduleJobByOnEventScheduledTestService.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(RescheduleJobByOnEventScheduledTestService.PROPERTY_DATA_VALUE,index);
			event =  new Event(RescheduleJobByOnEventScheduledTestService.DATA_VALUE_EVENT,eventProperties);
			eventDispatcher.schedule(RescheduleJobByOnEventScheduledTestService.QUEUE_ID, event);
							
			try
			{
				Thread.sleep(RescheduleJobByOnEventScheduledTestService.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	// Ende RescheduleJobByOnEventScheduledTestService
	
	// Start ReReScheduleJobInWorkTestService1
	
	@Test(timeout=10000)
	public void test11ReReScheduleJobInWorkTestService1() 
	{
		System.out.println("Tests for ReScheduleJobInWorkTestService1");
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService1.SHARED_OBJECT_EVENT,eventProperties);
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
			eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService1.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
			
		}
		
		try
		{
			Thread.sleep(ReReScheduleJobInWorkTestService1.TOLERANCE);
		}
		catch (Exception e) {}
		assertEquals("dataobject should equals index", index, dataObject.get());
	}
	
	@Test(timeout=20000)
	public void test12ReReScheduleJobInWorkTestService1() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService1.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService1.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService1.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test13ReReScheduleJobInWorkTestService1() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService1.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService1.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService1.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService1.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test14ReReScheduleJobInWorkTestService1() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService1.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService1.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService1.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test15ReReScheduleJobInWorkTestService1() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService1.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService1.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService1.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService1.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test16ReReScheduleJobInWorkTestService1() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService1.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService1.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService1.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test17ReReScheduleJobInWorkTestService1() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService1.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService1.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService1.DATA_VALUE_EVENT,eventProperties);
			eventDispatcher.schedule(ReReScheduleJobInWorkTestService1.QUEUE_ID, event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService1.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	// Ende ReReScheduleJobInWorkTestService1
	
	// Start ReReScheduleJobInWorkTestService2
	
	@Test(timeout=10000)
	public void test21ReReScheduleJobInWorkTestService2() 
	{
		System.out.println("Tests for ReScheduleJobInWorkTestService2");
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService2.SHARED_OBJECT_EVENT,eventProperties);
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
			eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService2.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
			
		}
		
		try
		{
			Thread.sleep(ReReScheduleJobInWorkTestService2.TOLERANCE);
		}
		catch (Exception e) {}
		assertEquals("dataobject should equals index", index, dataObject.get());
	}
	
	@Test(timeout=20000)
	public void test22ReReScheduleJobInWorkTestService2() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService2.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService2.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService2.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test23ReReScheduleJobInWorkTestService2() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService2.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService2.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService2.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService2.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test24ReReScheduleJobInWorkTestService2() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService2.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService2.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService2.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test25ReReScheduleJobInWorkTestService2() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService2.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService2.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService2.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService2.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test26ReReScheduleJobInWorkTestService2() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService2.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService2.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService2.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test27ReReScheduleJobInWorkTestService2() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService2.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService2.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService2.DATA_VALUE_EVENT,eventProperties);
			eventDispatcher.schedule(ReReScheduleJobInWorkTestService2.QUEUE_ID, event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService2.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}

	// Ende ReReScheduleJobInWorkTestService2
	
	// Start ReReScheduleJobInWorkTestService3
	
	@Test(timeout=10000)
	public void test31ReReScheduleJobInWorkTestService3() 
	{
		System.out.println("Tests for ReScheduleJobInWorkTestService3");
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService3.SHARED_OBJECT_EVENT,eventProperties);
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
			eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService3.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
			
		}
		
		try
		{
			Thread.sleep(ReReScheduleJobInWorkTestService3.TOLERANCE);
		}
		catch (Exception e) {}
		assertEquals("dataobject should equals index", index, dataObject.get());
	}
	
	@Test(timeout=20000)
	public void test32ReReScheduleJobInWorkTestService3() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService3.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService3.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService3.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test33ReReScheduleJobInWorkTestService3() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService3.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService3.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService3.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService3.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test34ReReScheduleJobInWorkTestService3() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService3.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService3.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService3.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test35ReReScheduleJobInWorkTestService3() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService3.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService3.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService3.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService3.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test36ReReScheduleJobInWorkTestService3() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService3.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService3.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService3.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test37ReReScheduleJobInWorkTestService3() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService3.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService3.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService3.DATA_VALUE_EVENT,eventProperties);
			eventDispatcher.schedule(ReReScheduleJobInWorkTestService3.QUEUE_ID, event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService3.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	// Ende ReReScheduleJobInWorkTestService3
	
	
	// Start ReReScheduleJobInWorkTestService4
	
	@Test(timeout=10000)
	public void test41ReReScheduleJobInWorkTestService4() 
	{
		System.out.println("Tests for ReScheduleJobInWorkTestService4");
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService4.SHARED_OBJECT_EVENT,eventProperties);
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
			eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService4.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
			
		}
		
		try
		{
			Thread.sleep(ReReScheduleJobInWorkTestService4.TOLERANCE);
		}
		catch (Exception e) {}
		assertEquals("dataobject should equals index", index, dataObject.get());
	}
	
	@Test(timeout=20000)
	public void test42ReReScheduleJobInWorkTestService4() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService4.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService4.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService4.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test43ReReScheduleJobInWorkTestService4() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService4.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService4.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService4.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService4.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test44ReReScheduleJobInWorkTestService4() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService4.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService4.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService4.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test45ReReScheduleJobInWorkTestService4() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService4.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService4.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService4.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService4.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test46ReReScheduleJobInWorkTestService4() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService4.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService4.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService4.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test47ReReScheduleJobInWorkTestService4() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService4.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService4.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService4.DATA_VALUE_EVENT,eventProperties);
			eventDispatcher.schedule(ReReScheduleJobInWorkTestService4.QUEUE_ID, event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService4.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	// Ende ReReScheduleJobInWorkTestService4
	
	// Start ReReScheduleJobInWorkTestService5
	
	@Test(timeout=10000)
	public void test51ReReScheduleJobInWorkTestService5() 
	{
		System.out.println("Tests for ReScheduleJobInWorkTestService5");
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService5.SHARED_OBJECT_EVENT,eventProperties);
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
			eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService5.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
			
		}
		
		try
		{
			Thread.sleep(ReReScheduleJobInWorkTestService5.TOLERANCE);
		}
		catch (Exception e) {}
		assertEquals("dataobject should equals index", index, dataObject.get());
	}
	
	@Test(timeout=20000)
	public void test52ReReScheduleJobInWorkTestService5() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService5.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService5.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService5.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test53ReReScheduleJobInWorkTestService5() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService5.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 7; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService5.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService5.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService5.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test54ReReScheduleJobInWorkTestService5() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService5.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService5.DATA_VALUE_EVENT,eventProperties);
				eventAdmin.sendEvent(event);
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService5.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test55ReReScheduleJobInWorkTestService5() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService5.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			for(int i = 0; i < 2; i++)
			{
				index++;
				
				eventProperties = new HashMap<String,Object>();
				eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_VALUE,index);
				event =  new Event(ReReScheduleJobInWorkTestService5.DATA_VALUE_EVENT,eventProperties);
				eventDispatcher.schedule(ReReScheduleJobInWorkTestService5.QUEUE_ID, event);
				
			}
			
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService5.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test56ReReScheduleJobInWorkTestService5() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService5.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService5.DATA_VALUE_EVENT,eventProperties);
			eventAdmin.sendEvent(event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService5.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	@Test(timeout=20000)
	public void test57ReReScheduleJobInWorkTestService5() 
	{
		AtomicLong dataObject = new AtomicLong();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_OBJECT,dataObject);
		Event event =  new Event(ReReScheduleJobInWorkTestService5.SHARED_OBJECT_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		
		long index = 0;
		
		for(int x = 0; x < 27; x++)
		{
			index++;
				
			eventProperties = new HashMap<String,Object>();
			eventProperties.put(ReReScheduleJobInWorkTestService5.PROPERTY_DATA_VALUE,index);
			event =  new Event(ReReScheduleJobInWorkTestService5.DATA_VALUE_EVENT,eventProperties);
			eventDispatcher.schedule(ReReScheduleJobInWorkTestService5.QUEUE_ID, event);
							
			try
			{
				Thread.sleep(ReReScheduleJobInWorkTestService5.TOLERANCE);
			}
			catch (Exception e) {}
			assertEquals("dataobject should equals index", index, dataObject.get());
		}
	}
	
	// Ende ReReScheduleJobInWorkTestService5
}
