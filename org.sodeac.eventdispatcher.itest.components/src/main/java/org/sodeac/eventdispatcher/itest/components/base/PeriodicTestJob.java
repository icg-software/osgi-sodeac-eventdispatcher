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

import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IPeriodicQueueTask;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTaskContext;

public class PeriodicTestJob implements IPeriodicQueueTask
{
	
	private int counter = 0;

	@Override
	public void run(IQueueTaskContext taskContext)
	{
		IQueue queue = taskContext.getQueue();
		ITaskControl taskControl = taskContext.getTaskControl();
		
		if(counter < 3)
		{
			counter++;
			queue.signal("COUNTER_"+ counter);
			return;
		}
		taskControl.setDone();

	}

	@Override
	public long getPeriodicRepetitionInterval()
	{
		return 2000;
	}
}
