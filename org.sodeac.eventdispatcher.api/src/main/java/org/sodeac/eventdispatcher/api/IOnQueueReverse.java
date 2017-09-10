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
 * An extension interface for components of {@link IEventController} that makes it possible to react if stops to observe a {@link IQueue}
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnQueueReverse extends IEventController
{
	/**
	 * This methode is fired, if {@link IEventController} stops to observe a {@link IQueue}
	 * 
	 * @param queue is unlinked with this {@link IEventController}
	 */
	public void onQueueReverse(IQueue queue);
}
