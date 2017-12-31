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
 * An extension interface for {@link IEventController} to consume notifications of finishing a job
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnJobDone extends IEventController
{
	/**
	 * This is fired, if {@link IQueueJob} remove a scheduled {@link IQueuedEvent}
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param job finished {@link IQueueJob}
	 */
	public void onJobDone(IQueueJob job);
}
