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
	private boolean valid = true;
	
	public QueueImpl getQueue()
	{
		return queue;
	}
	public void setQueue(QueueImpl queue)
	{
		this.queue = queue;
	}
	public long getWakeupTime()
	{
		return wakeupTime;
	}
	public void setWakeupTime(long wakeupTime)
	{
		this.wakeupTime = wakeupTime;
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
