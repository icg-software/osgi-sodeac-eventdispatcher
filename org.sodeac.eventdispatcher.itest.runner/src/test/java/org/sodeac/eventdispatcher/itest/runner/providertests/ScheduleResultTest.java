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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IQueueEventResult;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;
import org.sodeac.eventdispatcher.itest.components.scheduleresult.ScheduleResultTestController1;
import org.sodeac.eventdispatcher.itest.components.scheduleresult.ScheduleResultTestController2;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@Ignore
public class ScheduleResultTest extends AbstractTest
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
		super.waitQueueIsUp(eventDispatcher, ScheduleResultTestController1.QUEUE_ID, 3000);
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
	public void test01SimplestScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 2000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController1.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_DONE, true);
		Event event =  new Event(ScheduleResultTestController1.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController1.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertTrue("event should be scheduled", result.isQeueued());
		assertFalse("result should not contains errors", result.hasErrors());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000,expected= java.util.concurrent.TimeoutException.class)
	public void test02TimeoutScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 2000L;
		long maxwaitTime = 1000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController1.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_DONE, true);
		Event event =  new Event(ScheduleResultTestController1.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController1.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		
	}
	
	@Test(timeout=12000)
	public void test03SetDoneFalseScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController1.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_DONE, false);
		Event event =  new Event(ScheduleResultTestController1.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController1.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertFalse("result should not contains errors", result.hasErrors());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000)
	public void test04SetDoneFalseAndManErrorScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController1.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_DONE, false);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_MANUAL_ADD_EXCEPTION, new Exception("ManualError"));
		Event event =  new Event(ScheduleResultTestController1.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController1.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertTrue("result should not contains errors", result.hasErrors());
		assertEquals("result should contains 1 error", 1, result.getErrorList().size());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000)
	public void test05SetDoneFalseAndThrowsErrorScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController1.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_DONE, false);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_THROWS_EXCEPTION, new RuntimeException("ManualError"));
		Event event =  new Event(ScheduleResultTestController1.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController1.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertTrue("result should not contains errors", result.hasErrors());
		assertEquals("result should contains 1 error", 1, result.getErrorList().size());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000)
	public void test06SetDoneFalseAndManuExceptionAndThrowsErrorScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController1.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_SCHEDULE_DONE, false);
		eventProperties.put(ScheduleResultTestController1.PROPERTY_MANUAL_ADD_EXCEPTION, new Exception("ManualError"));
		eventProperties.put(ScheduleResultTestController1.PROPERTY_THROWS_EXCEPTION, new RuntimeException("ManualError"));
		Event event =  new Event(ScheduleResultTestController1.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController1.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertTrue("result should not contains errors", result.hasErrors());
		assertEquals("result should contains 2 error", 2, result.getErrorList().size());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000)
	public void test11SimplestScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 2000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController2.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_DONE, true);
		Event event =  new Event(ScheduleResultTestController2.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController2.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertTrue("event should be scheduled", result.isQeueued());
		assertFalse("result should not contains errors", result.hasErrors());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000,expected= java.util.concurrent.TimeoutException.class)
	public void test12TimeoutScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 2000L;
		long maxwaitTime = 1000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController2.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_DONE, true);
		Event event =  new Event(ScheduleResultTestController2.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController2.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		
	}
	
	@Test(timeout=12000)
	public void test13SetDoneFalseScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController2.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_DONE, false);
		Event event =  new Event(ScheduleResultTestController2.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController2.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertFalse("result should not contains errors", result.hasErrors());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000)
	public void test14SetDoneFalseAndManErrorScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController2.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_DONE, false);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_MANUAL_ADD_EXCEPTION, new Exception("ManualError"));
		Event event =  new Event(ScheduleResultTestController2.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController2.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertTrue("result should not contains errors", result.hasErrors());
		assertEquals("result should contains 1 error", 1, result.getErrorList().size());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000)
	public void test15SetDoneFalseAndThrowsErrorScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController2.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_DONE, false);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_THROWS_EXCEPTION, new RuntimeException("ManualError"));
		Event event =  new Event(ScheduleResultTestController2.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController2.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertTrue("result should not contains errors", result.hasErrors());
		assertEquals("result should contains 1 error", 1, result.getErrorList().size());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
	
	@Test(timeout=12000)
	public void test16SetDoneFalseAndManuExceptionAndThrowsErrorScheduleResultWorkflow() throws InterruptedException, ExecutionException, TimeoutException 
	{
		long scheduleTime = 1000L;
		long maxwaitTime = 3000L;
		IQueue queue = this.eventDispatcher.getQueue(ScheduleResultTestController2.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		Map<String,Object> bridge = new HashMap<String,Object>();
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_TIME, scheduleTime);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_BRIDGE, bridge);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_SCHEDULE_DONE, false);
		eventProperties.put(ScheduleResultTestController2.PROPERTY_MANUAL_ADD_EXCEPTION, new Exception("ManualError"));
		eventProperties.put(ScheduleResultTestController2.PROPERTY_THROWS_EXCEPTION, new RuntimeException("ManualError"));
		Event event =  new Event(ScheduleResultTestController2.SCHEDULE_EVENT,eventProperties);
		long ts1 = System.currentTimeMillis();
		eventAdmin.sendEvent(event);
		
		Future<IQueueEventResult> resultFuture = (Future<IQueueEventResult>)bridge.get(ScheduleResultTestController2.PROPERTY_FUTURE);
		assertNotNull("resultFuture should not be null" ,resultFuture);
		IQueueEventResult result = resultFuture.get(maxwaitTime, TimeUnit.MILLISECONDS);
		assertNotNull("result should not be null" ,result);
		long ts2 = System.currentTimeMillis();
		
		assertTrue("future should be done", resultFuture.isDone());
		assertFalse("event should be scheduled", result.isQeueued());
		assertTrue("result should not contains errors", result.hasErrors());
		assertEquals("result should contains 2 error", 2, result.getErrorList().size());
		long timeDiff = ts2 - ts1;
		assertTrue("ScheduleTime should be in Tolarance " + scheduleTime + " (+/- 150ms) . Actual: " + timeDiff, super.checkTimeMeasure(scheduleTime, timeDiff, 150, -1));
		
	}
}
