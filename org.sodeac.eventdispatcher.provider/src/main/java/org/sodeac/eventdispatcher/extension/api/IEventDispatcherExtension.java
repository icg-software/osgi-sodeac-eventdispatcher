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
package org.sodeac.eventdispatcher.extension.api;

import java.util.Map;

import org.sodeac.eventdispatcher.api.IEventController;

public interface IEventDispatcherExtension
{
	public void registerEventDispatcher(IExtensibleEventDispatcher dispatcher);
	public void unregisterEventDispatcher(IExtensibleEventDispatcher dispatcher);
	public void registerEventController(IExtensibleEventDispatcher dispatcher, IEventController eventController,Map<String, ?> properties);
	public void unregisterEventController(IExtensibleEventDispatcher dispatcher, IEventController eventController);
	public void registerEventQueue(IExtensibleEventDispatcher dispatcher, IExtensibleQueue extensibleQueue);
	public void unregisterEventQueue(IExtensibleEventDispatcher dispatcher, IExtensibleQueue extensibleQueue);
	
	public void registerCounter(IExtensibleEventDispatcher dispatcher, IExtensibleCounter counter);
	public void updateCounter(IExtensibleEventDispatcher dispatcher, IExtensibleCounter counter);
	public void unregisterCounter(IExtensibleEventDispatcher dispatcher, IExtensibleCounter counter);
	
	public void registerMeter(IExtensibleEventDispatcher dispatcher, IExtensibleMeter meter);
	public void updateMeter(IExtensibleEventDispatcher dispatcher, IExtensibleMeter meter);
	public void unregisterMeter(IExtensibleEventDispatcher dispatcher, IExtensibleMeter meter);
	
	public void registerHistogram(IExtensibleEventDispatcher dispatcher, IExtensibleHistogram histrogram);
	public void updateHistogram(IExtensibleEventDispatcher dispatcher, IExtensibleHistogram histrogram);
	public void unregisterHistogram(IExtensibleEventDispatcher dispatcher, IExtensibleHistogram histrogram);
	
	public void registerTimer(IExtensibleEventDispatcher dispatcher, IExtensibleTimer timer);
	public void updateTimer(IExtensibleEventDispatcher dispatcher, IExtensibleTimer timer);
	public void unregisterTimer(IExtensibleEventDispatcher dispatcher, IExtensibleTimer timer);
	
	public void registerGauge(IExtensibleEventDispatcher dispatcher, IExtensibleGauge<?> gauge);
	public void unregisterGauge(IExtensibleEventDispatcher dispatcher, IExtensibleGauge<?> gauge);
}
