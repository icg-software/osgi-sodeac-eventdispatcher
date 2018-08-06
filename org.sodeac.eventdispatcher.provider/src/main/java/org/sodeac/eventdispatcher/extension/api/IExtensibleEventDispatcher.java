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

import org.sodeac.eventdispatcher.api.IQueueController;
import org.osgi.framework.Bundle;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IQueueComponentConfigurable;
import org.sodeac.eventdispatcher.api.IQueueService;

public interface IExtensibleEventDispatcher extends IEventDispatcher
{
	public String getBundleId();
	public String getBundleVersion();
	
	public void registerQueueController(IQueueController queueController, IQueueComponentConfigurable configuration, Bundle bundle, Map<String, ?> properties);
	public void registerQueueService(IQueueService queueService,IQueueComponentConfigurable configuration, Bundle bundle, Map<String, ?> properties);
	
	public void unregisterQueueController(IQueueController eventController);
	public void unregisterQueueService(IQueueService queueService);
}
