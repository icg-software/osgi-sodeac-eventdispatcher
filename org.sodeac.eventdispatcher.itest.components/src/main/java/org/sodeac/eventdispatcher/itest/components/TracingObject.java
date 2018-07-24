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
package org.sodeac.eventdispatcher.itest.components;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TracingObject
{
	public static final String PROPERTY_KEY_TRACING_OBJECT = "TRACING_OBJECT";
	
	public TracingObject()
	{
		super();
		this.tracingEventList = new CopyOnWriteArrayList<>();//ArrayList<TracingEvent>();
	}
	
	private List<TracingEvent> tracingEventList = null;

	public List<TracingEvent> getTracingEventList()
	{
		return tracingEventList;
	}
}
