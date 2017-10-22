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
package org.sodeac.eventdispatcher.impl;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.api.IEventDispatcher;

public class ConsumeEventHandler implements EventHandler
{
	private IEventDispatcher dispatcher = null;
	private String queueId = null;
	private String topic = null;
	private ServiceRegistration<EventHandler> registration = null;
	
	public ConsumeEventHandler(IEventDispatcher dispatcher,String queueId,String topic)
	{
		super();
		this.dispatcher = dispatcher;
		this.queueId = queueId;
		this.topic = topic;
	}

	@Override
	public void handleEvent(Event event)
	{
		this.dispatcher.schedule(event, this.queueId);
	}

	public String getTopic()
	{
		return topic;
	}

	public ServiceRegistration<EventHandler> getRegistration()
	{
		return registration;
	}

	public void setRegistration(ServiceRegistration<EventHandler> registration)
	{
		this.registration = registration;
	}
	
}

