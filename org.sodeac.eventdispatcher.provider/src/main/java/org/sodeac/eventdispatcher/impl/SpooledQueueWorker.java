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
package org.sodeac.eventdispatcher.impl;

public class SpooledQueueWorker
{
	public SpooledQueueWorker(QueueImpl queue,long wakeupTime)
	{
		super();
		this.queue = queue;
		this.wakeupTime = wakeupTime;
	}
	
	private QueueImpl queue;
	private long wakeupTime;
	private volatile boolean valid = true;
	
	public QueueImpl getQueue()
	{
		return queue;
	}
	public long getWakeupTime()
	{
		return wakeupTime;
	}
	public boolean isValid()
	{
		return valid;
	}
	public void setValid(boolean valid)
	{
		this.valid = valid;
	}
}
