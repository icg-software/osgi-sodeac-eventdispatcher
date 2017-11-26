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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.runner.MetricFilterByName;
import org.sodeac.eventdispatcher.itest.components.MetricInstances;
import org.sodeac.eventdispatcher.itest.components.base.BaseTestController;
import org.sodeac.eventdispatcher.itest.components.metrics.JobMetricTestController;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
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
	
	@SuppressWarnings("unchecked")
	@Test
	public void test03JobMetrics() 
	{
		assertNotNull("metricInstances should not be null" ,metricInstances);
		MetricRegistry metricRegistry = metricInstances.getMetricRegistry();
		assertNotNull("metricRegistry should not be null" ,metricRegistry);
	
		CountDownLatch latch = new CountDownLatch(1);
		
		
		int repeat = 21;
		int sleeptime = 480;
		int worktime = 20;
		String jobId = "job" + repeat +"_" + sleeptime + "_" + worktime;
		int tolerance = 50 + (3 * repeat);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(JobMetricTestController.EVENT_PROPERTY_LATCH, latch);
		eventProperties.put(JobMetricTestController.EVENT_PROPERTY_JOB_ID, jobId);
		eventProperties.put(JobMetricTestController.EVENT_PROPERTY_REPEAT, repeat);
		eventProperties.put(JobMetricTestController.EVENT_PROPERTY_SLEEP_TIME, sleeptime);
		eventProperties.put(JobMetricTestController.EVENT_PROPERTY_WORK_TIME, worktime);
		Event event =  new Event(JobMetricTestController.RUN_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		long startTS = System.currentTimeMillis();
		
		try
		{
			latch.await(108, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		long stopTS = System.currentTimeMillis();
		
		// Job Created
		
		String key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_CREATED);
		Gauge<Long> jobCreatedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNotNull("jobCreatedGauge should not be null" ,jobCreatedGauge);
		long diff = jobCreatedGauge.getValue() - startTS;
		assertTrue("diff job-created-gauge should be 0 (+/- 50): " + diff, super.checkTimeMeasure(0, diff, 50, -1));
		
		// Job Started
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_STARTED);
		Gauge<Long> jobStartedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNotNull("jobStartedGauge should not be null" ,jobStartedGauge);
		diff = jobStartedGauge.getValue() - (startTS + ((repeat-1) * sleeptime) + ((repeat-1) * worktime));
		assertTrue("diff job-started-gauge should be 0 (+/- " + tolerance + "): " + diff, super.checkTimeMeasure(0, diff, tolerance, -1));
		
		// Last Heartbeat
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_LAST_HEARTBEAT);
		Gauge<Long> jobLastHeartbeatGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNotNull("jobLastHeartbeatGauge should not be null" ,jobLastHeartbeatGauge);
		diff = jobLastHeartbeatGauge.getValue() - ( startTS + ((repeat-1) * sleeptime) + ((repeat-1) * worktime));
		assertTrue("diff job-lastheartbeat-gauge should be 0 (+/- " + tolerance + "): " + diff, super.checkTimeMeasure(0, diff, tolerance, -1));
		
		// Job Finished
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_FINISHED);
		Gauge<Long> jobFinishedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNotNull("jobFinishedGauge should not be null" ,jobFinishedGauge);
		diff = jobFinishedGauge.getValue() - stopTS;
		assertTrue("diff job-finished-gauge should be 0 (+/- 50) ", super.checkTimeMeasure(0, diff, 50, -1));
		
		
		// Counter/Meter/Timer Run Job
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, jobId, IMetrics.POSTFIX_TIMER, IMetrics.METRICS_RUN_JOB);
		Timer runMeter = metricRegistry.getTimers(new MetricFilterByName(key)).get(key);
		assertNotNull("jobRunTimer should not be null" ,runMeter);
		assertEquals("jobRunTimer.count should equals repeat", repeat, (int)runMeter.getCount());

		assertTrue("statistic (event / second) should be 2.0 (+/- 0.2) " , super.checkMetric(2.0, runMeter.getMeanRate(), 0.2, -1));
		Snapshot sn =  runMeter.getSnapshot();
		double runtimeAVG = sn.getMedian() / 1000000.0;
		assertTrue("runtime.avg should equals 20 (+/- 2)",  super.checkMetric(20.0, runtimeAVG, 2, -1));
	}
	
	
}
