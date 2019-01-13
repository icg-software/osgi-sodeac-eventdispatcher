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

import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueTaskContext;

public class BaseTimeOutJob implements IQueueTask
{
	private long sleepValue;

	public BaseTimeOutJob(long sleepValue)
	{
		super();
		this.sleepValue = sleepValue;
	}

	@Override
	public void run(IQueueTaskContext taskContext)
	{
		try
		{
			Thread.sleep(this.sleepValue);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

}
