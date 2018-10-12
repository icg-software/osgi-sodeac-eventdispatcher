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
package org.sodeac.eventdispatcher.itest.runner.providertests;

import java.util.List;

import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueService;

public class HotDeployService implements IQueueController, IQueueService
{

	private int count = 0;

	@Override
	public void run
	(
		IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, ITaskControl taskControl,
		List<IQueueTask> currentProcessedJobList
	)
	{
		this.count++;
	}
	
	public void reset()
	{
		this.count = 0;
	}

	public int getCount()
	{
		return count;
	}
}
