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
package org.sodeac.eventdispatcher.extension.api;

import java.util.Map;

import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueueService;

public interface IExtensibleEventDispatcher extends IEventDispatcher
{
	public String getId();
	public String getBundleId();
	public String getBundleVersion();
	// TODO Properties / Metrics
	
	public void bindEventController(IEventController eventController,Map<String, ?> properties);
	public void bindQueueService(IQueueService queueService,Map<String, ?> properties);
	
	public void unbindEventController(IEventController eventController,Map<String, ?> properties);
	public void unbindQueueService(IQueueService queueService,Map<String, ?> properties);
}
