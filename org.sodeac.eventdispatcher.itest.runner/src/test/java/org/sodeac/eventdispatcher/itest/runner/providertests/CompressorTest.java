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
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;
import org.sodeac.eventdispatcher.itest.components.compressor.CompressorStatics;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CompressorTest extends AbstractTest
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
		super.waitQueueIsUp(eventDispatcher, CompressorStatics.QUEUE_ID, 3000);
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
	
	@Test(timeout=20000)
	public void test01SimplestDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(CompressorStatics.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = new TracingObject();
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(TracingObject.class.getCanonicalName(), tracingObject);
		Event event =  new Event(CompressorStatics.TOPIC_PUBLISH_TRACING_OBJECT,eventProperties);
		eventAdmin.sendEvent(event);
		
		eventAdmin.sendEvent(new Event(CompressorStatics.TOPIC_START_TEST,new HashMap<>()));
		
		/*
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 3. Fire Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Fire Event",TracingEvent.ON_FIRE_EVENT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 4. Remove Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Remove Event",TracingEvent.ON_REMOVE_EVENT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		//  5. Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
			
		//  6. Job Done
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job Done",TracingEvent.ON_JOB_DONE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		*/
	}
}
