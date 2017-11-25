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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.runner.MetricFilterByName;
import org.sodeac.eventdispatcher.itest.components.MetricInstances;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;
import org.sodeac.eventdispatcher.itest.components.base.BaseDelayedTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseEventRegistrationTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseExceptionTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseFilterTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseGetJobTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseHeartbeatTimeoutTestController;
import org.sodeac.eventdispatcher.itest.components.base.BasePeriodicJobTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseReCreateWorkerTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseReScheduleTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseServiceTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseTimeoutTestController;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MetricTest extends AbstractTest
{
	@Inject
	private BundleContext bundleContext;
	
	@Inject
	private IEventDispatcher eventDispatcher;
	
	@Inject
	private EventAdmin eventAdmin;
	
	@Inject
	private MetricInstances metricInstances;
	
	@Inject
	
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
		super.waitQueueIsUp(eventDispatcher, BaseTestController.QUEUE_ID, 3000);
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
	public void test01EventControllerCounter() 
	{
		assertNotNull("metricInstances should not be null" ,metricInstances);
		MetricRegistry metricRegistry = metricInstances.getMetricRegistry();
		assertNotNull("metricRegistry should not be null" ,metricRegistry);
	
		SortedMap<String, Counter> countersControllerRegistration = metricRegistry.getCounters
		(
			new MetricFilterByName(MetricRegistry.name
			(
				IEventDispatcher.class, 
				IMetrics.METRICS_EVENT_CONTROLLER,
				IMetrics.POSTFIX_COUNTER
			)
		));
		Counter counterControllerRegistration = countersControllerRegistration.get(countersControllerRegistration.firstKey());
		assertNotNull("counterControllerRegistration should not be null" ,counterControllerRegistration);
		
		long counterBefore = counterControllerRegistration.getCount();
		assertNotEquals("controllerlist should not be empty, because auf component regs", 0L,counterControllerRegistration.getCount());
		
		// register new controller
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(IEventDispatcher.PROPERTY_QUEUE_ID, "test123");
		ServiceRegistration<IEventController> reg = bundleContext.registerService(IEventController.class, new IEventController(){}, properties);
	
	
		long counterWhile = counterControllerRegistration.getCount();
		assertEquals("size of controllerlist should increment after add registration", counterBefore + 1,counterWhile);
		
		// unregister controller
		
		reg.unregister();
		
		long counterAfter = counterControllerRegistration.getCount();
		assertEquals("sizeof controllerlist should decrement after remove registration", counterWhile -1 , counterAfter);
		
	}
	
	@Test
	public void test02QueueCounter() 
	{
		assertNotNull("metricInstances should not be null" ,metricInstances);
		MetricRegistry metricRegistry = metricInstances.getMetricRegistry();
		assertNotNull("metricRegistry should not be null" ,metricRegistry);
	
		SortedMap<String, Counter> countersQueues = metricRegistry.getCounters
		(
			new MetricFilterByName(MetricRegistry.name
			(
				IEventDispatcher.class, 
				IMetrics.METRICS_QUEUE,
				IMetrics.POSTFIX_COUNTER
			)
		));
		Counter counterQueues = countersQueues.get(countersQueues.firstKey());
		assertNotNull("counterQueues should not be null" ,counterQueues);
		
		long counterBefore = counterQueues.getCount();
		assertNotEquals("queuelist should not be empty, because auf component regs", 0L,counterQueues.getCount());
		
		// register new controller
		
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(IEventDispatcher.PROPERTY_QUEUE_ID, "test123");
		ServiceRegistration<IEventController> reg = bundleContext.registerService(IEventController.class, new IEventController(){}, properties);
	
	
		long counterWhile = counterQueues.getCount();
		assertEquals("size of queuelist size should increment after add registration", counterBefore + 1,counterWhile);
		
		// unregister controller
		
		reg.unregister();
		
		long counterAfter = counterQueues.getCount();
		assertEquals("size of  queuelist size should decrement after remove registration", counterWhile -1 , counterAfter);
			
	}
	
	
}
