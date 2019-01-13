/*******************************************************************************
 * Copyright (c) 2017, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.components.base;

import org.sodeac.eventdispatcher.api.IQueueWorker;
import org.sodeac.eventdispatcher.api.IOnTaskStop;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueTaskContext;

public class BaseTimeOutAndStop2Job implements IQueueTask, IOnTaskStop
{
	private long sleepValue;

	public BaseTimeOutAndStop2Job(long sleepValue)
	{
		super();
		this.sleepValue = sleepValue;
	}
	
	@Override
	public void run(IQueueTaskContext taskContext)
	{
		IQueue queue = taskContext.getQueue();
		
		try
		{
			Thread.sleep(this.sleepValue);
		}
		catch (InterruptedException e) 
		{
			queue.signal("INTERRUPT");
			
			try
			{
				Thread.sleep(this.sleepValue);
			}
			catch (Exception ie) {}
			catch(ThreadDeath td )
			{
				queue.signal("THREAD_DEATH");
			}
			return;
		}
		queue.signal("JOB_DONE_SIGNAL");
	}

	@Override
	public long requestForMoreLifeTime(long requestNumber, long totalMoreTimeUntilNow,IQueueWorker worker)
	{
		if(requestNumber < 3)
		{
			worker.getQueue().signal("MORE_LIFE_TIME_" + requestNumber);
			return 100L;
		}
		return -1L;
	}

}
