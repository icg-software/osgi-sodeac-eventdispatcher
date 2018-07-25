/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;
import org.sodeac.eventdispatcher.itest.components.compressor.CompressorStatics;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@Ignore
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
		
		long previewsTimestamp = System.currentTimeMillis();
		eventAdmin.sendEvent(new Event(CompressorStatics.TOPIC_START_COMPRESSOR,new HashMap<>()));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		eventAdmin.sendEvent(new Event(CompressorStatics.TOPIC_START_TEST,new HashMap<>()));
		
		try
		{
			Thread.sleep(2000);
		}
		catch (Exception e) {}
		
		eventAdmin.sendEvent(new Event(CompressorStatics.TOPIC_STOP_COMPRESSOR,new HashMap<>()));
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		boolean firstSignal = true;
		boolean firstHeartBeat = true;
		
		List<Integer> values = new ArrayList<Integer>();
		for(int i = 0;i < tracingObject.getTracingEventList().size(); i++)
		{
			TracingEvent te = tracingObject.getTracingEventList().get(i);
			if(te.getMethode() != TracingEvent.SEND_EVENT)
			{
				continue;
			}
			event = te.getRawEvent();
			long currentTimeStanp = te.getTimestamp();
			long diff = (currentTimeStanp - previewsTimestamp);
			
			if(((Integer)event.getProperty(CompressorStatics.PROPERTY_COUNT_SIZE)).intValue() == 0)
			{
				if(firstHeartBeat)
				{
					firstHeartBeat = false;
				}
				else
				{
					System.out.println("" + diff + " ms heartbeat");
					assertTrue("heartbeat should fired after 1000 ms (+/- 50 ms)  . Actual: " + diff, super.checkTimeMeasure(1000, diff, 50, -1));
				}
			}
			else
			{
				int min = ((Integer)event.getProperty(CompressorStatics.PROPERTY_COUNT_MIN)).intValue();
				int max = ((Integer)event.getProperty(CompressorStatics.PROPERTY_COUNT_MAX)).intValue();
				System.out.println("" + diff + " ms  " + min + " - " + max);
				
				for(int j = min; j <= max; j++ )
				{
					values.add(j);
				}
				
				if(firstSignal)
				{
					firstSignal = false;
				}
				else
				{
					assertTrue("publish values should fired after 500 ms (+/- 50 ms)  . Actual: " + diff, super.checkTimeMeasure(500, diff, 50, -1));
				}
			}
			previewsTimestamp = currentTimeStanp;
		}
		
		assertEquals("100 signals should be fired", 100, values.size());
		
		int nextSignal = 0;
		
		for(int signal : values)
		{
			assertEquals("signal should be ordered and complete", nextSignal,signal);
			nextSignal++;
		}
		
		System.out.println("");
	}
}
