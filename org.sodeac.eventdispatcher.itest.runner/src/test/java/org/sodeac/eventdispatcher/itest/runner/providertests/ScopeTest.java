/*******************************************************************************
 * Copyright (c) 2017 Sebastian Palarus
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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueChildScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;
import org.sodeac.eventdispatcher.itest.components.scope.ScopeTestSimpleManagementController;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.cm.ConfigurationAdminOptions;
import org.ops4j.pax.exam.cm.ConfigurationOption;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@Ignore
public class ScopeTest extends AbstractTest
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
	
	@Test(timeout=12000)
	public void test01SimplestDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(ScopeTestSimpleManagementController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getStatePropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		CountDownLatch worklatch = new CountDownLatch(1);
		
		UUID scopeId = UUID.randomUUID(); 
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_LATCH, latch);
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH, worklatch);
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_SCOPEID, scopeId);
		Event event =  new Event(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_CREATE,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			worklatch.await(3, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		IQueueChildScope scope = queue.getChildScope(scopeId);
		assertNotNull("scope shoult be not null",scope);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		int counter = 0;
		TracingObject tracingObject2 = null;
		while(tracingObject2 == null)
		{
			tracingObject2 = (TracingObject) scope.getStatePropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
			try
			{
				Thread.sleep(200);
			}
			catch (Exception e) {}
			assertTrue("tracingObject schould created after 3 Seconds ",counter < 15);
			counter++;
		}
		int tracingEventPosition2 = 0;
		
		worklatch = new CountDownLatch(1);
		eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH, worklatch);
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_SCOPEID, scopeId);
		event =  new Event(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST1,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			worklatch.await(3, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		worklatch = new CountDownLatch(1);
		eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH, worklatch);
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_SCOPEID, scopeId);
		event =  new Event(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST2,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			worklatch.await(3, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		worklatch = new CountDownLatch(1);
		eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH, worklatch);
		event =  new Event(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_SIZE,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			worklatch.await(3, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// Global: 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_ATTACH, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
				
		// Global: 2. Signal
				
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		assertEquals("Signal should contains expected value","SCOPE_SIZE_0",tracingObject.getTracingEventList().get(tracingEventPosition).getSignal());
		tracingEventPosition++;
		
		// Global: 3. Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		assertEquals("Signal should contains expected value","SCOPE_SIZE_1",tracingObject.getTracingEventList().get(tracingEventPosition).getSignal());
		tracingEventPosition++;
		
		// Global: 4. Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		assertEquals("Signal should contains expected value","SCOPE_SIZE_0",tracingObject.getTracingEventList().get(tracingEventPosition).getSignal());
		tracingEventPosition++;
		
		// Scope: 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition2 , tracingObject2.getTracingEventList().size() > tracingEventPosition2);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_ATTACH, tracingObject2.getTracingEventList().get(tracingEventPosition2).getMethode());
		tracingEventPosition2++;
		
		// Scope: 2 Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition2 , tracingObject2.getTracingEventList().size() > tracingEventPosition2);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject2.getTracingEventList().get(tracingEventPosition2).getMethode());
		assertEquals("Signal should contains expected value",ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST1,tracingObject2.getTracingEventList().get(tracingEventPosition2).getSignal());
		tracingEventPosition2++;
		
		// Scope: 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition2 , tracingObject2.getTracingEventList().size() > tracingEventPosition2);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_DETACH, tracingObject2.getTracingEventList().get(tracingEventPosition2).getMethode());
		tracingEventPosition2++;
		
		/*for(TracingEvent te : tracingObject.getTracingEventList())
		{
			System.out.println("TE1: " + te.getMethode() + "  Signal: " + te.getSignal());
		}
		
		for(TracingEvent te2 : tracingObject2.getTracingEventList())
		{
			System.out.println("TE2: " + te2.getMethode() + "  Signal: " + te2.getSignal());
		}*/
		
	}
}
