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
package org.sodeac.eventdispatcher.api;

import org.osgi.service.event.Event;

/**
 * 
 * An extension interface for {@link IQueueController} to consume a notification if tasks (re)fire an event by invoke {@link IQueue}.send/postEvent 
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnFiredEvent extends IQueueController
{
	/**
	 * This is fired, if {@link IQueue} sends / posts  a new {@link org.osgi.service.event.Event}
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param event new fired {@link org.osgi.service.event.Event}
	 * @param queue fire by {@link IQueue}
	 */
	public void onFiredEvent(Event event,IQueue queue);
}
