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

import java.util.List;

/**
 * 
 * An extension interface for {@link IEventController} to consume a notification if {@link IQueue} has to schedule one or more {@link IQueuedEvent}s
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnScheduleEventList extends IEventController
{
	/**
	 * This is fired, if {@link IEventController} has to schedule one or more {@link org.osgi.service.event.Event}s.
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param queue related {@link IQueue}
	 * @param eventList new events, contains {@link org.osgi.service.event.Event}
	 */
	public void onScheduleEventList(IQueue queue, List<IQueuedEvent> eventList);
}
