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
 * wrapper object for worker thread 
 * 
 * @author Sebastian Palarus
 *
 */
public interface IQueueWorker
{

	/**
	 * invoke {@link java.lang.Thread#interrupt()}  on worker thread
	 */
	public void interrupt();
	
	/**
	 * get {@link IQueue} for which the worker works
	 * 
	 * @return queue for which the worker works
	 */
	public IQueue getQueue();
}
