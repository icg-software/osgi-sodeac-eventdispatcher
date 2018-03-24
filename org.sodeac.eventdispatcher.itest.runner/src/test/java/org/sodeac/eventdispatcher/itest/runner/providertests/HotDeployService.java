/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.runner.providertests;

import java.util.List;

import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;

public class HotDeployService implements IEventController, IQueueService
{

	private int count = 0;
	
	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}

	@Override
	public void run
	(
		IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl,
		List<IQueueJob> currentProcessedJobList
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
