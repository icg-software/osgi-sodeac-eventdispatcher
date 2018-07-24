/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueWorker;

public class QueueWorkerWrapper implements IQueueWorker
{
	private QueueWorker worker = null;
	
	public QueueWorkerWrapper(QueueWorker worker)
	{
		super();
		this.worker = worker;
	}

	@Override
	public void interrupt()
	{
		worker.interrupt();
	}

	@Override
	public IQueue getQueue()
	{
		return worker.getEventQueue();
	}

}
