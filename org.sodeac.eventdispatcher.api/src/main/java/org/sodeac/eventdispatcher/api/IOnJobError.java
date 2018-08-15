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
 * An extension interface for {@link IQueueController} to consume notifications if a job throws an exception or an error
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnJobError extends IQueueController
{
	/**
	 * This methode is fired, if {@link IQueueJob} throws an exception or an error
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param job job which throws the exception
	 * @param throwable throwed exception or error
	 */
	public void onJobError(IQueueJob job, Throwable throwable);
}
