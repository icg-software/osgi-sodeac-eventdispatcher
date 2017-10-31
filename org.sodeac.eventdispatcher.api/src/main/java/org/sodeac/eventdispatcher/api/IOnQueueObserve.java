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
 * An extension interface for {@link IEventController} that makes it possible to react if starts to observe a {@link IQueue}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnQueueObserve extends IEventController
{
	/**
	 * This is fired, if {@link IEventController} starts to observe a {@link IQueue}
	 * 
	 * @param queue is linked with {@link IEventController}
	 */
	public void onQueueObserve(IQueue queue);
}
