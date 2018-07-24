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

/**
 * An extension-interface for {@link IEventController} to declare relations or responsibilities to events.
 * 
 * @author Sebastian Palarus
 *
 */
public interface IConcernEvent
{
	/**
	 * request for relation or responsibility to an event
	 * 
	 * @param event {@link IQueuedEvent} to test relation
	 * @return true if object concern {@code event}, otherwise false
	 */
	public boolean concernEvent(IQueuedEvent event);
}
