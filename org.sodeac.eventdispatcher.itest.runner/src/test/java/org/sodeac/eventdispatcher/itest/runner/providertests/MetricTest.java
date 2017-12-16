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
import org.sodeac.eventdispatcher.itest.components.metrics.JobDisableMetricTestController1;
import org.sodeac.eventdispatcher.itest.components.metrics.JobDisableMetricTestController2;
import org.sodeac.eventdispatcher.itest.components.metrics.JobMetricTestController;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
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
import static org.junit.Assert.assertNull;
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
		int sleeptime = 800;
		int worktime = 200;
		String jobId = "job" + repeat +"_" + sleeptime + "_" + worktime;
		int tolerance = 50 + (30 * repeat);
		
		String key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, null, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_LAST_SEND_EVENT);
		Gauge<Long> queueLastSendEventGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNotNull("queueLastSendEventGauge should not be null" ,queueLastSendEventGauge);
		assertNull("queueLastSendEventGauge should be NULL ", queueLastSendEventGauge.getValue());
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, null, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_LAST_POST_EVENT);
		Gauge<Long> queueLastPostEventGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNotNull("queueLastPostEventGauge should not be null" ,queueLastPostEventGauge);
		assertNull("queueLastPostEventGauge should be NULL ",  queueLastPostEventGauge.getValue());
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SCHEDULE_EVENT);
		long scheduleCountBefore = metricRegistry.meter(key).getCount();
		
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
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SCHEDULE_EVENT);
		long scheduleCountAfter = metricRegistry.meter(key).getCount();
		
		assertEquals("scheduled events should be 1", 1, scheduleCountAfter - scheduleCountBefore);
		
		// Job Created
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_CREATED);
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
		
		// Last SendEvent
		
		diff = queueLastSendEventGauge.getValue() - stopTS;
		assertTrue("diff queue-lastsendevent-gauge should be 0 (+/- " + tolerance + "): " + diff, super.checkTimeMeasure(0, diff, tolerance, -1));
		
		// Last PostEvent
		
		diff = queueLastPostEventGauge.getValue() - stopTS;
		assertTrue("diff queue-lastpostevent-gauge should be 0 (+/- " + tolerance + "): " + diff, super.checkTimeMeasure(0, diff, tolerance, -1));
		
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

		assertTrue("statistic runtime. (event / second) should be 1.0 (+/- 0.3): " +runMeter.getMeanRate() , super.checkMetric(1.0, runMeter.getMeanRate(), 0.3, -1));
		Snapshot sn =  runMeter.getSnapshot();
		double runtimeAVG = sn.getMedian() / 1000000.0;
		assertTrue("runtime.avg should equals 200 (+/- 20) " + runtimeAVG,  super.checkMetric(200.0, runtimeAVG, 20, -1));
		
		// Counter/Meter SendEvent
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, null, IMetrics.POSTFIX_TIMER, IMetrics.METRICS_SEND_EVENT);
		Timer sendEventMeter = metricRegistry.getTimers(new MetricFilterByName(key)).get(key);
		assertNotNull("sendEventMeter should not be null" ,sendEventMeter);
		assertEquals("sendEventMeter.count should equals repeat", repeat, (int)sendEventMeter.getCount());

		assertTrue("statistic sendEvent (event / second) should be 1.0 (+/- 0.3): " +sendEventMeter.getMeanRate(), super.checkMetric(1.0, sendEventMeter.getMeanRate(), 0.3, -1));
		
		// Counter/Meter SendEvent
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_POST_EVENT);
		Meter postEventMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNotNull("postEventMeter should not be null" ,postEventMeter);
		assertEquals("postEventMeter.count should equals repeat", repeat, (int)postEventMeter.getCount());

		assertTrue("statistic postEvent (event / second) should be 1.0 (+/- 0.3): " +postEventMeter.getMeanRate() , super.checkMetric(1.0, postEventMeter.getMeanRate(), 0.3, -1));
		
		// Counter/Meter Signal
		
		key = IMetrics.metricName(JobMetricTestController.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SIGNAL);
		Meter signalMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNotNull("signalMeter should not be null" ,signalMeter);
		assertEquals("signalMeter.count should equals repeat", repeat, (int)signalMeter.getCount());

		assertTrue("statistic signal (event / second) should be 1.0 (+/- 0.3): " +signalMeter.getMeanRate() , super.checkMetric(1.0, signalMeter.getMeanRate(), 0.3, -1));
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test04DisableJobMetrics1() 
	{
		assertNotNull("metricInstances should not be null" ,metricInstances);
		MetricRegistry metricRegistry = metricInstances.getMetricRegistry();
		assertNotNull("metricRegistry should not be null" ,metricRegistry);
	
		CountDownLatch latch = new CountDownLatch(1);
		
		
		int repeat = 3;
		int sleeptime = 100;
		int worktime = 500;
		String jobId = "job1" + repeat +"_" + sleeptime + "_" + worktime;
		
		String key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, null, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_LAST_SEND_EVENT);
		Gauge<Long> queueLastSendEventGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("queueLastSendEventGauge  of disabled metric should not be null" ,queueLastSendEventGauge);
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, null, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_LAST_POST_EVENT);
		Gauge<Long> queueLastPostEventGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("queueLastPostEventGauge of disabled metric should be null" ,queueLastPostEventGauge);
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SCHEDULE_EVENT);
		Meter scheduleCountBeforeMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("scheduleCountBeforeMeter of disabled metric should be null" ,scheduleCountBeforeMeter);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(JobDisableMetricTestController1.EVENT_PROPERTY_LATCH, latch);
		eventProperties.put(JobDisableMetricTestController1.EVENT_PROPERTY_JOB_ID, jobId);
		eventProperties.put(JobDisableMetricTestController1.EVENT_PROPERTY_REPEAT, repeat);
		eventProperties.put(JobDisableMetricTestController1.EVENT_PROPERTY_SLEEP_TIME, sleeptime);
		eventProperties.put(JobDisableMetricTestController1.EVENT_PROPERTY_WORK_TIME, worktime);
		Event event =  new Event(JobDisableMetricTestController1.RUN_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(108, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SCHEDULE_EVENT);
		Meter scheduleCountAfterMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("scheduleCountAfterMeter of disabled metric should be null" ,scheduleCountAfterMeter);
		
		// Job Created
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_CREATED);
		Gauge<Long> jobCreatedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobCreatedGauge of disabled metric should be null" ,jobCreatedGauge);
		
		// Job Started
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_STARTED);
		Gauge<Long> jobStartedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobStartedGauge of disabled metric should be null" ,jobStartedGauge);
		
		// Last Heartbeat
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_LAST_HEARTBEAT);
		Gauge<Long> jobLastHeartbeatGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobLastHeartbeatGauge of disabled metric should be null" ,jobLastHeartbeatGauge);
		
		// Job Finished
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_FINISHED);
		Gauge<Long> jobFinishedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobFinishedGauge of disabled metric should be null" ,jobFinishedGauge);
		
		// Counter/Meter/Timer Run Job
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, jobId, IMetrics.POSTFIX_TIMER, IMetrics.METRICS_RUN_JOB);
		Timer runMeter = metricRegistry.getTimers(new MetricFilterByName(key)).get(key);
		assertNull("disabled jobRunTimer should be null" ,runMeter);
		
		// Counter/Meter SendEvent
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, null, IMetrics.POSTFIX_TIMER, IMetrics.METRICS_SEND_EVENT);
		Timer sendEventMeter = metricRegistry.getTimers(new MetricFilterByName(key)).get(key);
		assertNull("sendEventMeter should be null" ,sendEventMeter);
		
		// Counter/Meter SendEvent
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_POST_EVENT);
		Meter postEventMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("postEventMeter should be null" ,postEventMeter);
		
		// Counter/Meter Signal
		
		key = IMetrics.metricName(JobDisableMetricTestController1.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SIGNAL);
		Meter signalMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("signalMeter should be null" ,signalMeter);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void test05DisableJobMetrics2() 
	{
		assertNotNull("metricInstances should not be null" ,metricInstances);
		MetricRegistry metricRegistry = metricInstances.getMetricRegistry();
		assertNotNull("metricRegistry should not be null" ,metricRegistry);
	
		CountDownLatch latch = new CountDownLatch(1);
		
		
		int repeat = 3;
		int sleeptime = 100;
		int worktime = 500;
		String jobId = "job2" + repeat +"_" + sleeptime + "_" + worktime;
		
		String key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, null, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_LAST_SEND_EVENT);
		Gauge<Long> queueLastSendEventGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("queueLastSendEventGauge  of disabled metric should not be null" ,queueLastSendEventGauge);
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, null, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_LAST_POST_EVENT);
		Gauge<Long> queueLastPostEventGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("queueLastPostEventGauge of disabled metric should be null" ,queueLastPostEventGauge);
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SCHEDULE_EVENT);
		Meter scheduleCountBeforeMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("scheduleCountBeforeMeter of disabled metric should be null" ,scheduleCountBeforeMeter);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(JobDisableMetricTestController2.EVENT_PROPERTY_LATCH, latch);
		eventProperties.put(JobDisableMetricTestController2.EVENT_PROPERTY_JOB_ID, jobId);
		eventProperties.put(JobDisableMetricTestController2.EVENT_PROPERTY_REPEAT, repeat);
		eventProperties.put(JobDisableMetricTestController2.EVENT_PROPERTY_SLEEP_TIME, sleeptime);
		eventProperties.put(JobDisableMetricTestController2.EVENT_PROPERTY_WORK_TIME, worktime);
		Event event =  new Event(JobDisableMetricTestController2.RUN_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(108, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SCHEDULE_EVENT);
		Meter scheduleCountAfterMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("scheduleCountAfterMeter of disabled metric should be null" ,scheduleCountAfterMeter);
		
		// Job Created
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_CREATED);
		Gauge<Long> jobCreatedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobCreatedGauge of disabled metric should be null" ,jobCreatedGauge);
		
		// Job Started
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_STARTED);
		Gauge<Long> jobStartedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobStartedGauge of disabled metric should be null" ,jobStartedGauge);
		
		// Last Heartbeat
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_LAST_HEARTBEAT);
		Gauge<Long> jobLastHeartbeatGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobLastHeartbeatGauge of disabled metric should be null" ,jobLastHeartbeatGauge);
		
		// Job Finished
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, jobId, IMetrics.POSTFIX_GAUGE, IMetrics.GAUGE_JOB_FINISHED);
		Gauge<Long> jobFinishedGauge = metricRegistry.getGauges(new MetricFilterByName(key)).get(key);
		assertNull("jobFinishedGauge of disabled metric should be null" ,jobFinishedGauge);
		
		// Counter/Meter/Timer Run Job
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, jobId, IMetrics.POSTFIX_TIMER, IMetrics.METRICS_RUN_JOB);
		Timer runMeter = metricRegistry.getTimers(new MetricFilterByName(key)).get(key);
		assertNull("disabled jobRunTimer should be null" ,runMeter);
		
		// Counter/Meter SendEvent
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, null, IMetrics.POSTFIX_TIMER, IMetrics.METRICS_SEND_EVENT);
		Timer sendEventMeter = metricRegistry.getTimers(new MetricFilterByName(key)).get(key);
		assertNull("sendEventMeter should be null" ,sendEventMeter);
		
		// Counter/Meter SendEvent
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_POST_EVENT);
		Meter postEventMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("postEventMeter should be null" ,postEventMeter);
		
		// Counter/Meter Signal
		
		key = IMetrics.metricName(JobDisableMetricTestController2.QUEUE_ID, null, IMetrics.POSTFIX_METER, IMetrics.METRICS_SIGNAL);
		Meter signalMeter = metricRegistry.getMeters(new MetricFilterByName(key)).get(key);
		assertNull("signalMeter should be null" ,signalMeter);
		
	}
	
	
}
