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
import org.sodeac.eventdispatcher.itest.runner.MetricFilterByName;
import org.sodeac.eventdispatcher.itest.components.MetricInstances;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;
import org.sodeac.eventdispatcher.itest.components.base.BaseDelayedTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseExceptionTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseFilterTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseGetJobTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseHeartbeatTimeoutTestController;
import org.sodeac.eventdispatcher.itest.components.base.BasePeriodicJobTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseReCreateWorkerTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseReScheduleTestController;
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
public class BaseContainerTest extends AbstractTest
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
	public void test01ComponentRegistrationCounterNotEmpty() 
	{
		assertNotNull("metricInstances should not be null" ,metricInstances);
		MetricRegistry metricRegistry = metricInstances.getMetricRegistry();
		assertNotNull("metricRegistry should not be null" ,metricRegistry);
	
		SortedMap<String, Counter> countersControllerRegistration = metricRegistry.getCounters(new MetricFilterByName(MetricRegistry.name(IEventDispatcher.class, "controllerregistrations")));
		Counter counterControllerRegistration = countersControllerRegistration.get(countersControllerRegistration.firstKey());
		assertNotNull("counterControllerRegistration should not be null" ,counterControllerRegistration);
		assertNotEquals("counterControllerRegistration should not be empty", 0L,counterControllerRegistration.getCount());
		
	}
	
	@Test
	public void test02QueueCounterNotEmpty() 
	{
		assertNotNull("metricInstances should not be null" ,metricInstances);
		MetricRegistry metricRegistry = metricInstances.getMetricRegistry();
		assertNotNull("metricRegistry should not be null" ,metricRegistry);
	
		SortedMap<String, Counter> countersQueues = metricRegistry.getCounters(new MetricFilterByName(MetricRegistry.name(IEventDispatcher.class, "queues")));
		Counter counterQueues = countersQueues.get(countersQueues.firstKey());
		assertNotNull("counterQueues should not be null" ,counterQueues);
		assertNotEquals("counterQueues should not be empty", 0L,counterQueues.getCount());
			
	}
	
	@Test(timeout=5000)
	public void test03SimplestDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(5, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
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
		
	}
	
	@Test(timeout=5000 + BaseDelayedTestController.DELAY)
	public void test04SimpleDelayedDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseDelayedTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseDelayedTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseDelayedTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(5 + (BaseDelayedTestController.DELAY / 1000), TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long schedulingTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		// 3. Fire Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Fire Event",TracingEvent.ON_FIRE_EVENT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long fireEventTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
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
		
		assertTrue("Time between Scheduling Event and Refire Event should be >= " + BaseDelayedTestController.DELAY + ". Actual: " + (fireEventTimeStamp - schedulingTimeStamp), (fireEventTimeStamp - schedulingTimeStamp) >= BaseDelayedTestController.DELAY);
		
		System.out.println("[INFO] Definied delay for simpleDelayTest: " + BaseDelayedTestController.DELAY + " ms / measured delay on runtime: " + (fireEventTimeStamp - schedulingTimeStamp) + " ms");
	}
	
	@Test(timeout=5000 + BaseTimeoutTestController.SLEEP_VALUE)
	public void test05SimpleTimeoutDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseTimeoutTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseTimeoutTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseTimeoutTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(5 + (BaseTimeoutTestController.SLEEP_VALUE  / 1000), TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long schedulingTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		
		//  3. Job TimeOut
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job TimeOut",TracingEvent.ON_JOB_TIMEOUT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long timeoutTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		System.out.println("[INFO] Definied timeout for timeoutDelayTest: " + BaseTimeoutTestController.TIMEOUT_VALUE + " ms / measured timeouthandling on runtime: " + (timeoutTimeStamp - schedulingTimeStamp) + " ms");
		
	}
	
	@Test(timeout=60000)
	public void test06SimpleHearbeatTimeoutDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseHeartbeatTimeoutTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseHeartbeatTimeoutTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseHeartbeatTimeoutTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(20, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long schedulingTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		
		//  3. Job TimeOut
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job TimeOut",TracingEvent.ON_JOB_TIMEOUT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long timeoutTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		System.out.println("[INFO] Definied heartbeattimeout for target timeoutDelayTest: 9500 ms / measured timeouthandling on runtime: " + (timeoutTimeStamp - schedulingTimeStamp) + " ms");
		
	}
	
	@Test(timeout=25000)
	public void test07RecreateWorkerDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseReCreateWorkerTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseReCreateWorkerTestController.EVENT_PROPERTY_LATCH, latch);
		eventProperties.put(BaseReCreateWorkerTestController.EVENT_PROPERTY_SIGNAL, "FIRST_WORKER");
		Event event =  new Event(BaseReCreateWorkerTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(BaseReCreateWorkerTestController.TIMEOUT_VALUE +  1500);
		}
		catch (Exception e) {}
		
		latch = new CountDownLatch(1);
		
		eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseReCreateWorkerTestController.EVENT_PROPERTY_LATCH, latch);
		eventProperties.put(BaseReCreateWorkerTestController.EVENT_PROPERTY_SIGNAL, "SECOND_WORKER");
		event =  new Event(BaseReCreateWorkerTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(BaseReCreateWorkerTestController.SLEEP_VALUE +  3000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		String signal = null;
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
				
		// 2. Schedule First Event
				
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long schedulingTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		// 3. 5 Signals until Timeout (5400 ms)
		
		for(int i = 0; i < 5; i++)
		{	
			assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
			assertEquals("Expect Event scheduled",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
			signal = tracingObject.getTracingEventList().get(tracingEventPosition).getSignal();
			assertEquals("Expect Event scheduled","FIRST_WORKER_false",signal);
			tracingEventPosition++;
		}
		
		//  4. First Job TimeOut
				
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job TimeOut",TracingEvent.ON_JOB_TIMEOUT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long timeoutTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
			
		System.out.println("[INFO] Definied first jobtimeout in worker-recreate test: 5400 ms / measured timeouthandling on runtime: " + (timeoutTimeStamp - schedulingTimeStamp) + " ms");
		
		// 5. First Job continue working with timeout flag = true // + 6000
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		signal = tracingObject.getTracingEventList().get(tracingEventPosition).getSignal();
		assertEquals("Expect Event scheduled","FIRST_WORKER_true",signal);
		tracingEventPosition++;
		
		// 6. Schedule Second Event // +6900
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		schedulingTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		// 7. 2 x 5 Signals until second timeout
		
		for(int i = 0; i < 5; i++)
		{	
			assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
			assertEquals("Expect Event scheduled",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
			signal = tracingObject.getTracingEventList().get(tracingEventPosition).getSignal();
			assertEquals("Expect Event scheduled","FIRST_WORKER_true",signal);
			tracingEventPosition++;
			
			assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
			assertEquals("Expect Event scheduled",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
			signal = tracingObject.getTracingEventList().get(tracingEventPosition).getSignal();
			assertEquals("Expect Event scheduled","SECOND_WORKER_false",signal);
			tracingEventPosition++;
		}
		
		//  4. First Job TimeOut
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job TimeOut",TracingEvent.ON_JOB_TIMEOUT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		timeoutTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
				
		System.out.println("[INFO] Definied second jobtimeout with new worker in worker-recreate test: 5400 ms / measured timeouthandling on runtime: " + (timeoutTimeStamp - schedulingTimeStamp) + " ms");
		
		for(int i = 0; i < 6; i++)
		{
			assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
			assertEquals("Expect Event scheduled",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
			signal = tracingObject.getTracingEventList().get(tracingEventPosition).getSignal();
			assertEquals("Expect Event scheduled","SECOND_WORKER_true",signal);
			tracingEventPosition++;
		}

	}
	
	@Test(timeout=5000)
	public void test08SimpleExceptionDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseExceptionTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseExceptionTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseExceptionTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(5, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		//  3. Error
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job Error",TracingEvent.ON_JOB_ERROR, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
	}
	
	@Test(timeout=10000)
	public void test09SimpleReScheduleDispatcherWorkflow1() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseReScheduleTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseReScheduleTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseReScheduleTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(BaseReScheduleTestController.RESCHEDULE_DELAY);
		}
		catch (Exception e) {}
		
		event =  new Event(BaseReScheduleTestController.RESCHEDULE_EVENT1,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(5 + (BaseReScheduleTestController.DELAY / 1000), TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long schedulingTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		// 3. Fire Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Fire Event",TracingEvent.ON_FIRE_EVENT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long fireEventTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
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
		
		long expectOffset = BaseReScheduleTestController.DELAY + BaseReScheduleTestController.RESCHEDULE_DELAY;
		assertTrue("Time between Scheduling Event and Refire Event after rescheduling1 should be >= " +expectOffset + ". Actual: " + (fireEventTimeStamp - schedulingTimeStamp), (fireEventTimeStamp - schedulingTimeStamp) >= expectOffset);
		
		System.out.println("[INFO] Definied delay for reschedulingDelayTest1: " + expectOffset + " ms / measured delay on runtime: " + (fireEventTimeStamp - schedulingTimeStamp) + " ms");
		
		// Clear Tracing for Reschedule 2 Test
		TracingEvent queueObserve = tracingObject.getTracingEventList().get(0);
		tracingObject.getTracingEventList().clear();
		tracingObject.getTracingEventList().add(queueObserve);
	}
	
	@Test(timeout=10000)
	public void test10SimpleReScheduleDispatcherWorkflow2() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseReScheduleTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseReScheduleTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseReScheduleTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(BaseReScheduleTestController.RESCHEDULE_DELAY);
		}
		catch (Exception e) {}
		
		event =  new Event(BaseReScheduleTestController.RESCHEDULE_EVENT2,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(5, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long schedulingTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
		tracingEventPosition++;
		
		// 3. Fire Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Fire Event",TracingEvent.ON_FIRE_EVENT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		long fireEventTimeStamp = tracingObject.getTracingEventList().get(tracingEventPosition).getTimestamp();
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
		
		long expectOffset = BaseReScheduleTestController.RESCHEDULE_DELAY;
		assertTrue("Time between Scheduling Event and Refire Event after rescheduling2 should be >= " +expectOffset + ". Actual: " + (fireEventTimeStamp - schedulingTimeStamp), (fireEventTimeStamp - schedulingTimeStamp) >= expectOffset);
		
		long unexpectOffset = BaseReScheduleTestController.DELAY;
		assertTrue("Time between Scheduling Event and Refire Event after rescheduling2 should be < " +unexpectOffset + ". Actual: " + (fireEventTimeStamp - schedulingTimeStamp), (fireEventTimeStamp - schedulingTimeStamp) < unexpectOffset);
		
		System.out.println("[INFO] Definied delay for reschedulingDelayTest2: " + expectOffset + " ms / measured delay on runtime: " + (fireEventTimeStamp - schedulingTimeStamp) + " ms");
	}
	
	@Test(timeout=12000)
	public void test11GetJobDispatcherWorkflow() 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseGetJobTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseGetJobTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BaseGetJobTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) {}
		
		eventAdmin.sendEvent(new Event(BaseGetJobTestController.GETJOB_EVENT,new HashMap<String,Object>()));
		
		try
		{
			latch.await(5 + (BaseGetJobTestController.DELAY / 1000), TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 3. Signal 1 Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		String signal = tracingObject.getTracingEventList().get(tracingEventPosition).getSignal();
		assertEquals("First signal should be \"JOB_FOUND\"","JOB_FOUND",signal);
		tracingEventPosition++;
		
		// 4. Fire Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Fire Event",TracingEvent.ON_FIRE_EVENT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 5. Remove Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Remove Event",TracingEvent.ON_REMOVE_EVENT, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		//  6. Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
			
		//  7. Job Done
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job Done",TracingEvent.ON_JOB_DONE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		eventAdmin.sendEvent(new Event(BaseGetJobTestController.GETJOB_EVENT,new HashMap<String,Object>()));
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) {}
		
		// 8. Signal 2 Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		signal = tracingObject.getTracingEventList().get(tracingEventPosition).getSignal();
		assertEquals("Second signal should be \"JOB_NOT_FOUND\"","JOB_NOT_FOUND",signal);
		tracingEventPosition++;
	}
	
	@Test(timeout=10000)
	public void test12PropertyBlockDispatcherWorkflow() 
	{
		IPropertyBlock propertyBlock = this.eventDispatcher.createPropertyBlock();
		
		assertEquals("size of propertyBlockItems should be empty", 0, propertyBlock.getProperties().size());
		assertEquals("size of propertyBlockItems should be empty", 0, propertyBlock.getPropertyKeys().size());
		
		propertyBlock.setProperty("key1", "value1");
		propertyBlock.setProperty("key2", "value2");
		
		assertEquals("size of propertyBlockItems should have correct size", 2, propertyBlock.getProperties().size());
		assertEquals("size of propertyBlockItemsKeys should have correct size", 2, propertyBlock.getPropertyKeys().size());
		
		boolean foundKey1 = false;
		boolean foundKey2 = false;
		
		for(String key : propertyBlock.getPropertyKeys())
		{
			if(key.equals("key1"))
			{
				assertFalse("key should be found only one time", foundKey1);
				foundKey1 = true;
			}
			if(key.equals("key2"))
			{
				assertFalse("key should be found only one time", foundKey2);
				foundKey2 = true;
			}
		}
		
		assertTrue("both keys should be found", foundKey1  && foundKey2);
		
		
		assertEquals("value should be correct", "value1", propertyBlock.getProperty("key1"));
		assertEquals("value should be correct", "value2", propertyBlock.getProperty("key2"));
		
		propertyBlock.removeProperty("key1");
		
		assertEquals("size of propertyBlockItems should have correct size", 1, propertyBlock.getProperties().size());
		assertEquals("size of propertyBlockItemsKeys should have correct size", 1, propertyBlock.getPropertyKeys().size());
		
		foundKey2 = false;
		
		for(String key : propertyBlock.getPropertyKeys())
		{
			assertFalse("key1 should not be found",key.equals("key1"));
			if(key.equals("key2"))
			{
				assertFalse("key should be found only one time", foundKey2);
				foundKey2 = true;
			}
		}
		
		assertTrue("key should be found", foundKey2);
		
		
		assertNull("value should be correct", propertyBlock.getProperty("key1"));
		assertEquals("value should be correct", "value2", propertyBlock.getProperty("key2"));
		
		propertyBlock.clear();
		
		assertEquals("size of propertyBlockItems should have correct size", 0, propertyBlock.getProperties().size());
		assertEquals("size of propertyBlockItemsKeys should have correct size", 0, propertyBlock.getPropertyKeys().size());
		
		
		for(String key : propertyBlock.getPropertyKeys())
		{
			assertFalse("key1 should not be found",key.equals("key1"));
			assertFalse("key2 should not be found",key.equals("key2"));
		}
		
		
		assertNull("value should be correct", propertyBlock.getProperty("key1"));
		assertNull("value should be correct", propertyBlock.getProperty("key2"));
		
	}
	
	@Test(timeout=13000)
	public void test13FilterDispatcherWorkflow() throws InvalidSyntaxException 
	{
		IQueue queue = this.eventDispatcher.getQueue(BaseFilterTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		CountDownLatch latch = new CountDownLatch(1);
		
		String keyEventTopic = "EVENT_TOPIC"; 
		
		String stringValue1 = "STRVAL1";
		String stringValue2 = "STRVAL2";
		String stringValue3 = "STRVAL3";
		
		String intValue1 	= "INTVAL1";
		String intValue2 	= "INTVAL2";
		String intValue3 	= "INTVAL3";
		
		String boolValue1 	= "BOOLVAL1";
		String boolValue2 	= "BOOLVAL2";
		
		String queueEventPrefix = "Q";
		String jobPrefex = "J";
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseFilterTestController.EVENT_PROPERTY_LATCH, latch);
		
		eventProperties.put(keyEventTopic, BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT1);
		eventProperties.put(stringValue1, "value1");
		eventProperties.put(stringValue2, "value2");
		eventProperties.put(stringValue3, "value3");
		eventProperties.put(intValue1, 10);
		eventProperties.put(intValue2, 200);
		eventProperties.put(intValue3, 3000);
		eventProperties.put(boolValue1, true);
		eventProperties.put(boolValue2, false);
		
		Map<String,Object> queuedEventProps = new HashMap<String,Object>();
		for(Entry<String, Object> entry : eventProperties.entrySet())
		{
			queuedEventProps.put(queueEventPrefix + entry.getKey(), entry.getValue());
		}
		eventProperties.put(BaseFilterTestController.PROPERTY_QE_PROPS, queuedEventProps);
		
		Map<String,Object> jobProps = new HashMap<String,Object>();
		for(Entry<String, Object> entry : eventProperties.entrySet())
		{
			jobProps.put(jobPrefex + entry.getKey(), entry.getValue());
		}
		eventProperties.put(BaseFilterTestController.PROPERTY_JOB_PROPS, jobProps);
		
		
		Event event1 =  new Event(BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT1,eventProperties);
		eventAdmin.sendEvent(event1);
		
		eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseFilterTestController.EVENT_PROPERTY_LATCH, latch);
		
		eventProperties.put(keyEventTopic, BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT2);
		eventProperties.put(stringValue1, "valuea");
		eventProperties.put(stringValue2, "valueb");
		eventProperties.put(stringValue3, "valuec");
		eventProperties.put(intValue1, -10);
		eventProperties.put(intValue2, -200);
		eventProperties.put(intValue3, -3000);
		eventProperties.put(boolValue1, false);
		eventProperties.put(boolValue2, null);
		
		queuedEventProps = new HashMap<String,Object>();
		for(Entry<String, Object> entry : eventProperties.entrySet())
		{
			queuedEventProps.put(queueEventPrefix + entry.getKey(), entry.getValue());
		}
		eventProperties.put(BaseFilterTestController.PROPERTY_QE_PROPS, queuedEventProps);
		
		jobProps = new HashMap<String,Object>();
		for(Entry<String, Object> entry : eventProperties.entrySet())
		{
			jobProps.put(jobPrefex + entry.getKey(), entry.getValue());
		}
		eventProperties.put(BaseFilterTestController.PROPERTY_JOB_PROPS, jobProps);
		
		
		Event event2 =  new Event(BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT2,eventProperties);
		eventAdmin.sendEvent(event2);
		
		eventProperties = new HashMap<String,Object>();
		eventProperties.put(BaseFilterTestController.EVENT_PROPERTY_LATCH, latch);
		
		eventProperties.put(keyEventTopic, BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT3);
		eventProperties.put(stringValue1, "abc");
		eventProperties.put(stringValue2, "xyz");
		eventProperties.put(stringValue3, null);
		eventProperties.put(intValue1, 1);
		eventProperties.put(intValue2, 2);
		eventProperties.put(intValue3, 3);
		eventProperties.put(boolValue1, false);
		eventProperties.put(boolValue2, true);
		
		queuedEventProps = new HashMap<String,Object>();
		for(Entry<String, Object> entry : eventProperties.entrySet())
		{
			queuedEventProps.put(queueEventPrefix + entry.getKey(), entry.getValue());
		}
		eventProperties.put(BaseFilterTestController.PROPERTY_QE_PROPS, queuedEventProps);
		
		jobProps = new HashMap<String,Object>();
		for(Entry<String, Object> entry : eventProperties.entrySet())
		{
			jobProps.put(jobPrefex + entry.getKey(), entry.getValue());
		}
		eventProperties.put(BaseFilterTestController.PROPERTY_JOB_PROPS, jobProps);
		
		Event event3 =  new Event(BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT3,eventProperties);
		eventAdmin.sendEvent(event3);
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) {}
		
		IQueuedEvent queuedEvent1 = (IQueuedEvent)queue.getPropertyBlock().getProperty(BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT1);
		assertNotNull("queuedEvent1  should not be null" ,queuedEvent1);
		assertEquals("queuedEvent1.event should be event1", event1.getProperty(keyEventTopic), queuedEvent1.getEvent().getProperty(keyEventTopic));
		
		IQueuedEvent queuedEvent2 = (IQueuedEvent)queue.getPropertyBlock().getProperty(BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT2);
		assertNotNull("queuedEvent2  should not be null" ,queuedEvent2);
		assertEquals("queuedEvent2.event should be event2", event2.getProperty(keyEventTopic), queuedEvent2.getEvent().getProperty(keyEventTopic));
		
		IQueuedEvent queuedEvent3 = (IQueuedEvent)queue.getPropertyBlock().getProperty(BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT3);
		assertNotNull("queuedEvent3  should not be null" ,queuedEvent3);
		assertEquals("queuedEvent3.event should be event3", event3.getProperty(keyEventTopic), queuedEvent3.getEvent().getProperty(keyEventTopic));
		
		IQueueJob job1 = (IQueueJob)queuedEvent1.getProperty(BaseFilterTestController.PROPERTY_JOB);
		assertNotNull("job1  should not be null" ,job1);
		
		IQueueJob job2 = (IQueueJob)queuedEvent2.getProperty(BaseFilterTestController.PROPERTY_JOB);
		assertNotNull("job2  should not be null" ,job2);
		
		IQueueJob job3 = (IQueueJob)queuedEvent3.getProperty(BaseFilterTestController.PROPERTY_JOB);
		assertNotNull("job3  should not be null" ,job3);
		
		// test queueevent filter
		
		assertTrue("queue.getEventList  1 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1},
			queue.getEventList(null, bundleContext.createFilter("(QSTRVAL1=value1)"),null)
		));
		
		// test some filter expressions
		
		assertTrue("queue.getEventList  2 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent2},
			queue.getEventList(null, bundleContext.createFilter("(QINTVAL2=-200)"),null)
		));
		assertTrue("queue.getEventList  3 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent2,queuedEvent3},
			queue.getEventList(null,bundleContext.createFilter("(QINTVAL2>=-300)"),null)
		));
		assertTrue("queue.getEventList  4 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent3},
			queue.getEventList(null,bundleContext.createFilter("(QINTVAL2>=0)"),null)
		));
		assertTrue("queue.getEventList  5 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent2},
			queue.getEventList(null,bundleContext.createFilter("(!(QINTVAL2>=0))"),null)
		));
		assertTrue("queue.getEventList  6 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent2,queuedEvent3},
			queue.getEventList(null,bundleContext.createFilter("(QINTVAL3=*)"),null)
		));
		assertTrue("queue.getEventList  7 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1},
			queue.getEventList(null, bundleContext.createFilter("(QSTRVAL1=*ue1)"),null)
		));
		assertTrue("queue.getEventList  8 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent2},
			queue.getEventList(null, bundleContext.createFilter("(QSTRVAL1=value*)"),null)
		));
		assertTrue("queue.getEventList  9 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent2,queuedEvent3},
			queue.getEventList(null, bundleContext.createFilter("(|(QSTRVAL1=value*)(QSTRVAL1=abc))"),null)
		));
		assertTrue("queue.getEventList 10 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1},
			queue.getEventList(null, bundleContext.createFilter("(QBOOLVAL1=true)"),null)
		));
		assertTrue("queue.getEventList 11 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent2,queuedEvent3},
			queue.getEventList(null, bundleContext.createFilter("(QBOOLVAL1=false)"),null)
		));
		assertTrue("queue.getEventList 12 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent2},
			queue.getEventList(null, bundleContext.createFilter("(!(|(QBOOLVAL2=true)(QBOOLVAL2=false)))"),null)
		));
		
		// test event filter
		
		assertTrue("queue.getEventList nativ  1 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1},
			queue.getEventList(null, null, bundleContext.createFilter("(STRVAL1=value1)"))
		));
		
		// test job filter
		
		assertTrue("queue.getJobList  1 should returns correct list",listContentEqueals
		(
			new Object[] {job2},
			queue.getJobList(bundleContext.createFilter("(JSTRVAL2=valueb)"))
		));
		
		// test topic
		
		assertTrue("queue.getEventList topic 1 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent3},
			queue.getEventList(new String[] 
			{
				BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT1, 
				BaseFilterTestController.SCHEDULE_EVENT + BaseFilterTestController.EVENT3
			}, null, null)
		));
		
		assertTrue("queue.getEventList topic 2 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent2,queuedEvent3},
			queue.getEventList(new String[] {BaseFilterTestController.ALL}, null, null)
		));
		assertTrue("queue.getEventList topic 3 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent1,queuedEvent2,queuedEvent3},
			queue.getEventList(new String[] {BaseFilterTestController.SCHEDULE_EVENT +BaseFilterTestController.ALL}, null, null)
		));
		assertTrue("queue.getEventList topic 4 should returns correct list", listContentEqueals
		(
			new Object[] {queuedEvent2},
			queue.getEventList(new String[] {BaseFilterTestController.ALL + BaseFilterTestController.EVENT2}, null, null)
		));
	}
	
	private boolean listContentEqueals(Object[] expect, List<?> actual)
	{
		if(expect.length != actual.size())
		{
			return false;
		}
		Map<Object,Object> expectIndex = new HashMap<Object,Object>();
		Map<Object,Object> actualIndex =  new HashMap<Object,Object>();
		
		for(Object e : expect)
		{
			expectIndex.put(e,e);
		}
		
		for(Object a :actual)
		{
			actualIndex.put(a,a);
		}
		
		for(Object key : expectIndex.keySet())
		{
			if(actualIndex.get(key) == null)
			{
				return false;
			}
		}
		
		for(Object key : actualIndex.keySet())
		{
			if(expectIndex.get(key) == null)
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Test(timeout=13000)
	public void test14PeriodicJob() throws Exception 
	{
		IQueue queue = this.eventDispatcher.getQueue(BasePeriodicJobTestController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		TracingObject tracingObject = (TracingObject) queue.getPropertyBlock().getProperty(TracingObject.PROPERTY_KEY_TRACING_OBJECT);
		assertNotNull("tracingObject should not be null" ,tracingObject);
		
		int tracingEventPosition = 0;
		CountDownLatch latch = new CountDownLatch(1);
		
		Map<String,Object> eventProperties = new HashMap<String,Object>();
		eventProperties.put(BasePeriodicJobTestController.EVENT_PROPERTY_LATCH, latch);
		Event event =  new Event(BasePeriodicJobTestController.SCHEDULE_EVENT,eventProperties);
		eventAdmin.sendEvent(event);
		
		try
		{
			latch.await(10, TimeUnit.SECONDS);
		}
		catch (Exception e) {}
		
		// 1. Queue Observe
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Queue observer",TracingEvent.ON_QUEUE_OBSERVE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		// 2. Schedule Event
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Event scheduled",TracingEvent.ON_EVENT_SCHEDULED, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
		//  3. Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		assertEquals("Expect correct QueueSignal","COUNTER_1", tracingObject.getTracingEventList().get(tracingEventPosition).getSignal());
		tracingEventPosition++;
		
		//  4. Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		assertEquals("Expect correct QueueSignal","COUNTER_2", tracingObject.getTracingEventList().get(tracingEventPosition).getSignal());
		tracingEventPosition++;
	
		//  5. Signal
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect QueueSignal",TracingEvent.ON_QUEUE_SIGNAL, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		assertEquals("Expect correct QueueSignal","COUNTER_3", tracingObject.getTracingEventList().get(tracingEventPosition).getSignal());
		tracingEventPosition++;
		
		//  6. Job Done
		
		assertTrue("tracingEventLists should contains item " + tracingEventPosition , tracingObject.getTracingEventList().size() > tracingEventPosition);
		assertEquals("Expect Job Done",TracingEvent.ON_JOB_DONE, tracingObject.getTracingEventList().get(tracingEventPosition).getMethode());
		tracingEventPosition++;
		
	}
}
