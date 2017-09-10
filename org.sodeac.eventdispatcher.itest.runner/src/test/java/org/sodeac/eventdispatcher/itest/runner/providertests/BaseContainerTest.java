package org.sodeac.eventdispatcher.itest.runner.providertests;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.eventdispatcher.itest.runner.MetricFilterByName;
import org.sodeac.eventdispatcher.itest.components.MetricInstances;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;
import org.sodeac.eventdispatcher.itest.components.base.BaseDelayedTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseHeartbeatTimeoutTestController;
import org.sodeac.eventdispatcher.itest.components.base.BaseReCreateWorkerTestController;
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
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
		//TODO  Test getJobById
	}
	
	@Test(timeout=10000)
	public void test09SimpleReRunDispatcherWorkflow() 
	{
		// TODO Test ReRun (3 Times)
	}
	
	@Test(timeout=10000)
	public void test09PropertyBlockDispatcherWorkflow() 
	{
		// TODO Test PropertyBlock
	}
	
	@Test(timeout=10000)
	public void test10PropertyBlockDispatcherWorkflow() 
	{
		// TODO Test Filter
	}
	
	// TODO test inTimeOut
}
