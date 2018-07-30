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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.common.edservice.api.DiscoverEServiceRequest;
import org.sodeac.eventdispatcher.common.edservice.api.DiscoverEServiceResponse;
import org.sodeac.eventdispatcher.common.edservice.api.IEServiceDiscovery;
import org.sodeac.eventdispatcher.common.edservice.api.IEventDrivenService;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.Capability;
import org.sodeac.eventdispatcher.itest.components.scope.ScopeTestSimpleManagementController;
import org.sodeac.eventdispatcher.itest.components.service.Service1;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@Ignore
public class ServiceTest extends AbstractTest
{
	@Inject
	private BundleContext bundleContext;
	
	@Inject
	private IEventDispatcher eventDispatcher;
	
	@Inject
	private EventAdmin eventAdmin;
	
	@Inject
	private IEServiceDiscovery serviceDiscovery;
	
	public static final String QUEUE_ID_CREATE_AND_REMOVE = "servicequeue.create.remove";
	
	@Configuration
	public Option[] config() 
	{
		return super.config();
	}
	
	@Before
	public void setUp() {}
    
	@Test
	public void test001ComponentInstance() 
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
	
	@Test
	public void test002ServiceManagementQueueCreateAndRemove() 
	{
		Bundle testComponentBundle = null;
		Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) 
        {
            if(bundle.getSymbolicName().equals("org.sodeac.eventdispatcher.common"))
            {
            	testComponentBundle = bundle;
            }
        }
		assertNotNull("common should not be null" ,testComponentBundle);
		
		IQueue queue = this.eventDispatcher.getQueue(QUEUE_ID_CREATE_AND_REMOVE);
		assertNull("queue should be null" ,queue);
		
		IEventDrivenService eventDrivenService = new IEventDrivenService()
		{

			@Override
			public List<Capability> getCapabilityList(){return null;}
		};
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(IEventDrivenService.PROPERTY_SERVICE_QUEUE_ID, QUEUE_ID_CREATE_AND_REMOVE);
		ServiceRegistration<IEventDrivenService> registration = testComponentBundle.getBundleContext().registerService(IEventDrivenService.class,eventDrivenService, properties);
		
		try {Thread.sleep(2000);} catch (Exception e) {}
		
		try
		{
			for(int i = 0; i < 100; i++)
			{
				queue = this.eventDispatcher.getQueue(QUEUE_ID_CREATE_AND_REMOVE);
				if(queue != null)
				{
					break;
				}
				Thread.sleep(100);
			}
		}
		catch (Exception e) {}
		

		assertNotNull("queue should not be null" ,queue);
		
		registration.unregister();
		
		try
		{
			for(int i = 0; i < 100; i++)
			{
				queue = this.eventDispatcher.getQueue(QUEUE_ID_CREATE_AND_REMOVE);
				if(queue == null)
				{
					break;
				}
				Thread.sleep(100);
			}
		}
		catch (Exception e) {}
		
		assertNull("queue should be null" ,queue);
		
		registration = testComponentBundle.getBundleContext().registerService(IEventDrivenService.class,eventDrivenService, properties);
		
		try {Thread.sleep(2000);} catch (Exception e) {}
		
		try
		{
			for(int i = 0; i < 100; i++)
			{
				queue = this.eventDispatcher.getQueue(QUEUE_ID_CREATE_AND_REMOVE);
				if(queue != null)
				{
					break;
				}
				Thread.sleep(100);
			}
		}
		catch (Exception e) {}
		
		assertNotNull("queue should not be null" ,queue);
		
		registration.unregister();
		
		try
		{
			for(int i = 0; i < 100; i++)
			{
				queue = this.eventDispatcher.getQueue(QUEUE_ID_CREATE_AND_REMOVE);
				if(queue == null)
				{
					break;
				}
				Thread.sleep(100);
			}
		}
		catch (Exception e) {}
		
		assertNull("queue should be null" ,queue);
		
	}
	
	@Test(timeout=60000)
	public void test003DiscoveryService() 
	{
		super.waitQueueIsUp(eventDispatcher, IEServiceDiscovery.QUEUE_ID, 3000);
		
		assertNotNull("servicediscovery should not be null" ,this.serviceDiscovery);
		DiscoverEServiceResponse response = this.serviceDiscovery.discoverService(new DiscoverEServiceRequest(Service1.DOMAIN, Service1.SERVICE, null, null));
		assertNotNull("response should not be null" ,response);
		assertEquals("size of discovered services should be correct", 1, response.getServiceReferenceList().size());
		
		response = this.serviceDiscovery.discoverService(new DiscoverEServiceRequest("????", Service1.SERVICE, null, null));
		assertNotNull("response should not be null" ,response);
		assertEquals("size of discovered services should be correct", 0, response.getServiceReferenceList().size());
		
		response = this.serviceDiscovery.discoverService(new DiscoverEServiceRequest(Service1.DOMAIN, "????", null, null));
		assertNotNull("response should not be null" ,response);
		assertEquals("size of discovered services should be correct", 0, response.getServiceReferenceList().size());
		
		response = this.serviceDiscovery.discoverService(new DiscoverEServiceRequest(null, Service1.SERVICE, null, null));
		assertNotNull("response should not be null" ,response);
		assertEquals("size of discovered services should be correct", 0, response.getServiceReferenceList().size());
		
		response = this.serviceDiscovery.discoverService(new DiscoverEServiceRequest(Service1.DOMAIN, null, null, null));
		assertNotNull("response should not be null" ,response);
		assertEquals("size of discovered services should be correct", 0, response.getServiceReferenceList().size());
		
		response = this.serviceDiscovery.discoverService(new DiscoverEServiceRequest(null, null, null, null));
		assertNotNull("response should not be null" ,response);
		assertEquals("size of discovered services should be correct", 0, response.getServiceReferenceList().size());
			
	}
}
