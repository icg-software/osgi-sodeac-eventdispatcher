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

import org.sodeac.multichainlist.Snapshot;

/**
 * 
 * An extension interface for {@link IQueueController} to consume a notification if {@link IQueue} has queued one or more {@link IQueuedEvent}s
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnQueuedEventList extends IQueueController
{
	/**
	 * This is fired, if {@link IQueue} has queued one or more {@link IQueuedEvent}s
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param queue related {@link IQueue}
	 * @param queuedEvents new events, wraps {@link org.osgi.service.event.Event}
	 */
	public void onQueuedEventList(IQueue queue, Snapshot<IQueuedEvent> queuedEvents);
}
