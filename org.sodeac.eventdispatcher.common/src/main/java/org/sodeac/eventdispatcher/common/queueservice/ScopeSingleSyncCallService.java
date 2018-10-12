package org.sodeac.eventdispatcher.common.queueservice;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;

@Component
(
	service=IQueueService.class,
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"="+"(org.sodeac.eventdispatcher.common.queueservice.ScopeSingleSyncCallServiceAdapter=*)",
		IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL+"= 108000",
		IQueueService.PROPERTY_SERVICE_ID+"=" + ScopeSingleSyncCallService.SERVICE_ID
	}
)
public class ScopeSingleSyncCallService implements IQueueService
{
	public static final String SERVICE_ID = "org.sodeac.eventdispatcher.common.queueservice.scopesinglecall";

	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, ITaskControl taskControl,List<IQueueTask> currentProcessedJobList)
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
				((IQueueSessionScope)queue).dispose();
			}
			catch (Exception e) 
			{}
		}

	}

}
