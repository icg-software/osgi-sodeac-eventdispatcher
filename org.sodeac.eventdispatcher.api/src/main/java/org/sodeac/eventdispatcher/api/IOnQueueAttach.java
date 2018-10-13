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
 * An extension interface for {@link IQueueController} to consume notifications if instance of {@link IQueueController} attach to a {@link IQueue}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnQueueAttach extends IQueueController
{
	/**
	 * This is fired, if {@link IQueueController} attach to a {@link IQueue}
	 * <br>
	 * invoked and synchronized by queue worker
	 * 
	 * @param queue is attached with {@link IQueueController}
	 */
	public void onQueueAttach(IQueue queue);
}
