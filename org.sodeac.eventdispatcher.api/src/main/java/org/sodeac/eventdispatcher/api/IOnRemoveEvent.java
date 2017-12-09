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
 * An extension interface for {@link IEventController} to consume notifications if a {@link IQueuedEvent} is removed on {@link IQueue}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnRemoveEvent extends IEventController
{
	/**
	 * This methode is fired, if {@link IEventController} remove a scheduled {@link IQueuedEvent}
	 * 
	 * @param event removed event
	 */
	public void onRemoveEvent(IQueuedEvent event);
}
