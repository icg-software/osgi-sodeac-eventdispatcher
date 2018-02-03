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
 * An extension interface for {@link IEventController} to consume a notification if {@link IQueue} has to schedule a {@link IQueuedEvent}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnScheduleEvent extends IEventController
{
	/**
	 * This is fired, if {@link IEventController} has to schedule a new {@link org.osgi.service.event.Event}.
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param event new event, contains {@link org.osgi.service.event.Event}
	 */
	public void onScheduleEvent(IQueuedEvent event);
}
