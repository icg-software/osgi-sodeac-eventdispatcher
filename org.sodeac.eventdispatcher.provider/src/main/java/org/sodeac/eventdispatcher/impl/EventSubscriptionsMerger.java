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
package org.sodeac.eventdispatcher.impl;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.EventType;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;

public class EventSubscriptionsMerger
{
	private QueueImpl queue;
	private Map<String,QueueComponentConfiguration.SubscribeEvent> subscriptionIndex;
	private Map<String,ConsumeEventHandler> eventHandlerIndex;
	
	protected EventSubscriptionsMerger(QueueImpl queue, ControllerContainer remove, ControllerContainer add)
	{
		super();
		this.queue = queue;
		this.subscriptionIndex = new HashMap<String,QueueComponentConfiguration.SubscribeEvent>();
		this.eventHandlerIndex = new HashMap<String,ConsumeEventHandler>();
		this.analyse(remove,add);
	}
	
	private void analyse(ControllerContainer remove, ControllerContainer add)
	{
		subscriptionIndex.clear();
		eventHandlerIndex.clear();
		String key;
		
		for(ControllerContainer controllerContainer : queue.getConfigurationList())
		{
			if(controllerContainer == remove)
			{
				continue;
			}
			
			if(controllerContainer.getSubscribeEventList() != null)
			{
				for(QueueComponentConfiguration.SubscribeEvent subscribeEvent : controllerContainer.getSubscribeEventList())
				{
					if(subscribeEvent.getTopic() == null)
					{
						continue;
					}
					if(subscribeEvent.getTopic().isEmpty())
					{
						continue;
					}
					// TODO Bitoperation
					if(subscribeEvent.getEventType() == EventType.ScheduledByEventDispatcher)
					{
						continue;
					}
					if(subscribeEvent.getEventType() == EventType.PublishedByEventDispatcher)
					{
						continue;
					}
					if(subscribeEvent.getEventType() == EventType.AllByEventDispatcher)
					{
						continue;
					}
					
					key = subscribeEvent.getTopic() + "..." + (subscribeEvent.getLdapFilter() == null ? "" : subscribeEvent.getLdapFilter());
					
					if(subscriptionIndex.containsKey(key))
					{
						continue;
					}
					subscriptionIndex.put(key, subscribeEvent);
					
				} // end subscribe event loop			
			}
		}
		
		if(add != null)
		{
			if(add.getSubscribeEventList() != null)
			{
				for(QueueComponentConfiguration.SubscribeEvent subscribeEvent : add.getSubscribeEventList())
				{
					if(subscribeEvent.getTopic() == null)
					{
						continue;
					}
					if(subscribeEvent.getTopic().isEmpty())
					{
						continue;
					}
					// TODO Bitoperation
					if(subscribeEvent.getEventType() == EventType.ScheduledByEventDispatcher)
					{
						continue;
					}
					if(subscribeEvent.getEventType() == EventType.PublishedByEventDispatcher)
					{
						continue;
					}
					if(subscribeEvent.getEventType() == EventType.AllByEventDispatcher)
					{
						continue;
					}
					
					key = subscribeEvent.getTopic() + "..." + (subscribeEvent.getLdapFilter() == null ? "" : subscribeEvent.getLdapFilter());
					
					if(subscriptionIndex.containsKey(key))
					{
						continue;
					}
					
					subscriptionIndex.put(key, subscribeEvent);
				} // end subscribe event loop			
			}
		}
		
		List<ConsumeEventHandler> consumerList = queue.getConsumeEventHandlerList();
		if(consumerList != null)
		{
			for(ConsumeEventHandler eventHandler : consumerList)
			{
				this.eventHandlerIndex.put(eventHandler.getKey(), eventHandler);
			}
		}
	}
	
	public void merge()
	{
		for(Entry<String, QueueComponentConfiguration.SubscribeEvent> entry : this.subscriptionIndex.entrySet())
		{
			if(this.eventHandlerIndex.get(entry.getKey()) == null)
			{
				QueueComponentConfiguration.SubscribeEvent subscribeEvent = entry.getValue();
				ConsumeEventHandler handler = new ConsumeEventHandler(queue.getEventDispatcher(), queue.getId(), entry.getKey());
				Hashtable<String, Object> registerProperties = new Hashtable<String,Object>();
				registerProperties.put(EventConstants.EVENT_TOPIC,subscribeEvent.getTopic());
				if((subscribeEvent.getLdapFilter() != null) && (!subscribeEvent.getLdapFilter().isEmpty()))
				{
					registerProperties.put(EventConstants.EVENT_FILTER,subscribeEvent.getLdapFilter());
				}
				ServiceRegistration<EventHandler> registration = queue.getEventDispatcher().getContext().getBundleContext().registerService(EventHandler.class, handler, registerProperties);
				handler.setRegistration(registration);
				queue.addConsumeEventHandler(handler);
			}
		}
		for(Entry<String,ConsumeEventHandler> entry : this.eventHandlerIndex.entrySet())
		{
			if(this.subscriptionIndex.get(entry.getKey()) == null)
			{
				ConsumeEventHandler handler = entry.getValue();
				if(handler.getQueueId() == null)
				{
					continue;
				}
				if(! handler.getQueueId().equals(this.queue.getId()))
				{
					continue;
				}
				
				ServiceRegistration<EventHandler> registration = handler.getRegistration();
				handler.setRegistration(null);
				try
				{
					if(registration != null)
					{
						registration.unregister();
					}
				}
				catch (Exception e) 
				{
					queue.log(LogService.LOG_ERROR,"Unregistration Consuming Event for queue " + queue.getId() , e);
				}
				queue.removeConsumeEventHandler(entry.getValue());
			}
		}
	}
}
