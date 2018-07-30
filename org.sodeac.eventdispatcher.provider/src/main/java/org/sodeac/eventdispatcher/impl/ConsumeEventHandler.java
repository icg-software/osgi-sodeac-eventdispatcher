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
package org.sodeac.eventdispatcher.impl;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.api.IEventDispatcher;

public class ConsumeEventHandler implements EventHandler
{
	private IEventDispatcher dispatcher = null;
	private String queueId = null;
	private ServiceRegistration<EventHandler> registration = null;
	
	public ConsumeEventHandler(IEventDispatcher dispatcher,String queueId)
	{
		super();
		this.dispatcher = dispatcher;
		this.queueId = queueId;
	}

	@Override
	public void handleEvent(Event event)
	{
		this.dispatcher.schedule(this.queueId, event);
	}

	public String getQueueId()
	{
		return queueId;
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

