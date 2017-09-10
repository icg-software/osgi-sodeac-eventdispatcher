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
package org.sodeac.eventdispatcher.api;

import java.util.List;

import org.osgi.service.event.Event;

public interface IEventDispatcher
{
	public static final String PROPERTY_QUEUE_ID = "queueid";
	
	public boolean schedule(Event event, String queueId);
	public IPropertyBlock createPropertyBlock();
	public List<String> getQueueIdList();
	public IQueue getQueue(String queueId);
}
