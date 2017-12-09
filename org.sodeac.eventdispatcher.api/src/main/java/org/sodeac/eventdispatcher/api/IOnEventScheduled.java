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

/**
 * 
 * An extension interface for {@link IEventController} to consume a notification if {@link IQueue} scheduled a {@link IQueuedEvent}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnEventScheduled extends IEventController
{
	/**
	 * This is fired, if {@link IEventController} schedule a new {@link org.osgi.service.event.Event}.
	 * <br>
	 * invoked by queueworker
	 * 
	 * @param event new scheduled event, contains {@link org.osgi.service.event.Event}
	 */
	public void onEventScheduled(IQueuedEvent event);
}
