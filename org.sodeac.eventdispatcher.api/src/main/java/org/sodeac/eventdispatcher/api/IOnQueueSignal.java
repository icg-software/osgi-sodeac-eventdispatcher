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
 * An extension interface for {@link IEventController} that makes it possible to react signal
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnQueueSignal extends IEventController
{
	/**
	 * This is fired, if a signal is fired for this queue
	 * <br>
	 * invoked by queueworker
	 * 
	 * @param queue parent queue
	 * @param signal fired signal
	 */
	public void onQueueSignal(IQueue queue, String signal);
}
