/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.common.queueservice;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueueChildScope;

@Component
(
	service=IQueueService.class,
	property=
	{
		EventDispatcherConstants.PROPERTY_QUEUE_MATCH_FILTER+"="+"(org.sodeac.eventdispatcher.common.queueservice.ScopeSingleSyncCallServiceAdapter=*)",
		EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL+"= 108000",
		EventDispatcherConstants.PROPERTY_SERVICE_ID+"=" + ScopeSingleSyncCallService.SERVICE_ID
	}
)
public class ScopeSingleSyncCallService implements IQueueService
{
	public static final String SERVICE_ID = "org.sodeac.eventdispatcher.common.queueservice.scopesinglecall";

	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, ITaskControl taskControl,List<IQueueTask> currentProcessedTaskList)
	{
		@SuppressWarnings("unchecked")
		ScopeSingleSyncCallServiceAdapter<Object> adapter = queue.getConfigurationPropertyBlock().getAdapter(ScopeSingleSyncCallServiceAdapter.class);
		if(adapter.getQueue() == null)
		{
			adapter.setQueue(queue);
		}
		else if(adapter.getQueue() != queue)
		{
			return;
		}
		Object value = adapter.getValue();
		if(value != null)
		{
			try
			{
				adapter.getExchanger().exchange(value);
				((IQueueChildScope)queue).dispose();
			}
			catch (Exception e) 
			{}
		}

	}

}
