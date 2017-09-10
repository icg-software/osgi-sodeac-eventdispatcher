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

import java.util.Map;

import org.sodeac.eventdispatcher.api.IEventController;

public class ControllerContainer
{
	private Map<String, ?> properties = null;
	private IEventController eventController = null;
	
	public Map<String, ?> getProperties()
	{
		return properties;
	}
	public void setProperties(Map<String, ?> properties)
	{
		this.properties = properties;
	}
	public IEventController getEventController()
	{
		return eventController;
	}
	public void setEventController(IEventController eventController)
	{
		this.eventController = eventController;
	}
}
