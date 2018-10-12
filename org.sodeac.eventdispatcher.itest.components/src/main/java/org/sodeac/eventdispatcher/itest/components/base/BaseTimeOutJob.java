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
package org.sodeac.eventdispatcher.itest.components.base;

import java.util.List;

import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;

public class BaseTimeOutJob implements IQueueTask
{
	private long sleepValue;

	public BaseTimeOutJob(long sleepValue)
	{
		super();
		this.sleepValue = sleepValue;
	}

	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, ITaskControl taskControl,List<IQueueTask> currentProcessedJobList)
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
