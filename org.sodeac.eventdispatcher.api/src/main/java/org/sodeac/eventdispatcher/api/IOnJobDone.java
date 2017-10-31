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
 * An extension interface for {@link IEventController} that makes it possible to react on finishing a job
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnJobDone extends IEventController
{
	/**
	 * This is fired, if {@link IQueueJob} remove a scheduled {@link IQueuedEvent}
	 * <br>
	 * invoked by queueworker
	 * 
	 * @param job finished {@link IQueueJob}
	 */
	public void onJobDone(IQueueJob job);
}
