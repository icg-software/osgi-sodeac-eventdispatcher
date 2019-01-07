/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.runner.providertests;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
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

import java.util.Dictionary;
import java.util.Hashtable;
import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@Ignore
public class HotDeployTest extends AbstractTest
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
	
	@Test(timeout=60000)
	public void test01RescheduleServiceA() 
	{
		try
		{
			Thread.sleep(2000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		HotDeployService service = new HotDeployService();
		
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventDispatcherConstants.PROPERTY_QUEUE_ID, "hotdeployservicetest");
		properties.put(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL, "3000");
		properties.put(EventDispatcherConstants.PROPERTY_SERVICE_ID, "hotdeployservice");
		ServiceRegistration<IQueueController> regController = bundleContext.registerService(IQueueController.class, service, properties);
		ServiceRegistration<IQueueService> regService = bundleContext.registerService(IQueueService.class, service, properties);
		
		try
		{
			Thread.sleep(13500);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regController.unregister();
		regService.unregister();
		
		assertEquals("service should invoked 5 times", 5, service.getCount());
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		service.reset();
		
		regController = bundleContext.registerService(IQueueController.class, service, properties);
		regService = bundleContext.registerService(IQueueService.class, service, properties);
		
		try
		{
			Thread.sleep(13000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regController.unregister();
		regService.unregister();
		
	}
	
	@Test(timeout=60000)
	public void test02RescheduleServiceB() 
	{
		try
		{
			Thread.sleep(2000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		HotDeployService service = new HotDeployService();
		
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventDispatcherConstants.PROPERTY_QUEUE_ID, "hotdeployservicetest");
		properties.put(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL, "3000");
		properties.put(EventDispatcherConstants.PROPERTY_SERVICE_ID, "hotdeployservice");
		ServiceRegistration<IQueueService> regService = bundleContext.registerService(IQueueService.class, service, properties);
		ServiceRegistration<IQueueController> regController = bundleContext.registerService(IQueueController.class, service, properties);
		try
		{
			Thread.sleep(13500);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regController.unregister();
		regService.unregister();
		
		assertEquals("service should invoked 5 times", 5, service.getCount());
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		service.reset();
		
		regService = bundleContext.registerService(IQueueService.class, service, properties);
		regController = bundleContext.registerService(IQueueController.class, service, properties);
		
		try
		{
			Thread.sleep(13000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regController.unregister();
		regService.unregister();
		
	}
	
	public void test03RescheduleServiceC() 
	{
		try
		{
			Thread.sleep(2000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		HotDeployService service = new HotDeployService();
		
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventDispatcherConstants.PROPERTY_QUEUE_ID, "hotdeployservicetest");
		properties.put(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL, "3000");
		properties.put(EventDispatcherConstants.PROPERTY_SERVICE_ID, "hotdeployservice");
		ServiceRegistration<IQueueService> regService = bundleContext.registerService(IQueueService.class, service, properties);
		ServiceRegistration<IQueueController> regController = bundleContext.registerService(IQueueController.class, service, properties);
		try
		{
			Thread.sleep(13500);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regService.unregister();
		regController.unregister();
		
		assertEquals("service should invoked 5 times", 5, service.getCount());
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		service.reset();
		
		regService = bundleContext.registerService(IQueueService.class, service, properties);
		regController = bundleContext.registerService(IQueueController.class, service, properties);
		
		try
		{
			Thread.sleep(13000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regService.unregister();
		regController.unregister();
		
	}
	
	@Test(timeout=60000)
	public void test04RescheduleServiceD() 
	{
		try
		{
			Thread.sleep(2000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		HotDeployService service = new HotDeployService();
		
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventDispatcherConstants.PROPERTY_QUEUE_ID, "hotdeployservicetest");
		properties.put(EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL, "3000");
		properties.put(EventDispatcherConstants.PROPERTY_SERVICE_ID, "hotdeployservice");
		ServiceRegistration<IQueueController> regController = bundleContext.registerService(IQueueController.class, service, properties);
		ServiceRegistration<IQueueService> regService = bundleContext.registerService(IQueueService.class, service, properties);
		
		try
		{
			Thread.sleep(13500);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regService.unregister();
		regController.unregister();
		
		assertEquals("service should invoked 5 times", 5, service.getCount());
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		service.reset();
		
		regController = bundleContext.registerService(IQueueController.class, service, properties);
		regService = bundleContext.registerService(IQueueService.class, service, properties);
		
		try
		{
			Thread.sleep(13000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		regService.unregister();
		regController.unregister();
		
	}
}
