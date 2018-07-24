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
 * 
 * An extension interface for {@link IEventController} to consume notifications if instance of {@link IEventController} finished observing a {@link IQueue}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnQueueReverse extends IEventController
{
	/**
	 * This is fired, if {@link IEventController} stops to observe a {@link IQueue}
	 * <br>
	 * Attention! This call is not synchronized by worker thread!
	 * 
	 * @param queue is unlinked from {@link IEventController}
	 */
	public void onQueueReverse(IQueue queue);
}
