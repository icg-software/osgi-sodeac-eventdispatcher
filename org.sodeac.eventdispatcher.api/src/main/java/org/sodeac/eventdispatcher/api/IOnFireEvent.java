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

import org.osgi.service.event.Event;

/**
 * 
 * An extension interface for {@link IEventController} to consume a notification if jobs (re)fire an event by invoke {@link IQueue}.send/postEvent 
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnFireEvent extends IEventController
{
	/**
	 * This is fired, if {@link IQueue} sends / posts  a new {@link org.osgi.service.event.Event}
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param event new fired {@link org.osgi.service.event.Event}
	 * @param queue fire by {@link IQueue}
	 */
	public void onFireEvent(Event event,IQueue queue);
}
