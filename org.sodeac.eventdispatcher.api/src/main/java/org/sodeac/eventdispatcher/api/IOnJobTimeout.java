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
 * An extension interface for {@link IEventController} to consume notifications if a job runs in a timeout
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnJobTimeout extends IEventController
{
	/**
	 * This is fired, if {@link IQueueJob} runs in timeout
	 * <br>
	 * invoked by new thread
	 * 
	 * @param job runs in timeout
	 */
	public void onJobTimeout(IQueueJob job);
}
