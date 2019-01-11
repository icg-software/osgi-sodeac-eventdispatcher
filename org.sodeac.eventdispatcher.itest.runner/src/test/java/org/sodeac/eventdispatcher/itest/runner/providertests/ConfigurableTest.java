/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
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
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.itest.runner.AbstractTest;
import org.sodeac.multichainlist.Snapshot;
import org.sodeac.eventdispatcher.itest.components.configurable.ConfigurableQueueController;

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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import javax.inject.Inject;
import javax.lang.model.element.Element;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
//@Ignore
public class ConfigurableTest extends AbstractTest
{
	@Inject
	private BundleContext bundleContext;
	
	@Inject
	private IEventDispatcher eventDispatcher;
	
	@Inject
	private EventAdmin eventAdmin;
	
	
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
		super.waitQueueIsUp(eventDispatcher, ConfigurableQueueController.QUEUE_ID, 3000);
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
	public void test01Configurable1() 
	{
		
		IQueue queue = this.eventDispatcher.getQueue(ConfigurableQueueController.QUEUE_ID);
		assertNotNull("queue should not be null" ,queue);
		
		queue.queueEvent(new Event("event1",new HashMap<String,Object>()));
		queue.queueEvent(new Event("event2",new HashMap<String,Object>()));
		queue.queueEvent(new Event("event3",new HashMap<String,Object>()));
		
		Snapshot<IQueuedEvent> snapNull = queue.getEventSnapshot(null);
		try
		{
			assertEquals("size should be correct", 0, snapNull.size());
		}
		finally 
		{
			snapNull.close();
			snapNull = null;
		}
		
		Snapshot<IQueuedEvent> snapChain1 = queue.getEventSnapshot("chain1");
		try
		{
			assertEquals("size should be correct", 3, snapChain1.size());
			for(IQueuedEvent event : snapChain1)
			{
				event.removeFromQueue();
			}
		}
		finally 
		{
			snapChain1.close();
			snapChain1 = null;
		}
		
		snapChain1 = queue.getEventSnapshot("chain1");
		try
		{
			assertEquals("size should be correct", 0, snapChain1.size());
		}
		finally 
		{
			snapChain1.close();
			snapChain1 = null;
		}
		
		queue.queueEvents(Arrays.asList(new Event[]
		{
				new Event("event1",new HashMap<String,Object>()),
				new Event("event2",new HashMap<String,Object>()),
				new Event("event3",new HashMap<String,Object>())
		}));
		
		snapNull = queue.getEventSnapshot(null);
		try
		{
			assertEquals("size should be correct", 0, snapNull.size());
		}
		finally 
		{
			snapNull.close();
			snapNull = null;
		}
		
		snapChain1 = queue.getEventSnapshot("chain1");
		try
		{
			assertEquals("size should be correct", 3, snapChain1.size());
			for(IQueuedEvent event : snapChain1)
			{
				event.removeFromQueue();
			}
		}
		finally 
		{
			snapChain1.close();
			snapChain1 = null;
		}
		
		snapChain1 = queue.getEventSnapshot("chain1");
		try
		{
			assertEquals("size should be correct", 0, snapChain1.size());
		}
		finally 
		{
			snapChain1.close();
			snapChain1 = null;
		}
		
		queue.disableRule("chain1rule");
		
		queue.queueEvent(new Event("event1",new HashMap<String,Object>()));
		queue.queueEvent(new Event("event2",new HashMap<String,Object>()));
		queue.queueEvent(new Event("event3",new HashMap<String,Object>()));
		
		snapChain1 = queue.getEventSnapshot("chain1");
		try
		{
			assertEquals("size should be correct", 0, snapChain1.size());
		}
		finally 
		{
			snapChain1.close();
			snapChain1 = null;
		}
		
		snapNull = queue.getEventSnapshot(null);
		try
		{
			assertEquals("size should be correct", 3, snapNull.size());
			for(IQueuedEvent event : snapNull)
			{
				event.removeFromQueue();
			}
		}
		finally 
		{
			snapNull.close();
			snapNull = null;
		}
		
		snapNull = queue.getEventSnapshot(null);
		try
		{
			assertEquals("size should be correct", 0, snapNull.size());
		}
		finally 
		{
			snapNull.close();
			snapNull = null;
		}
	}
	
}
