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

@Component
(
	service=IQueueService.class,
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"="+"(org.sodeac.eventdispatcher.common.queueservice.TimeOutServiceAdapter=*)",
		IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL+"= 108000",
		IQueueService.PROPERTY_SERVICE_ID+"=" + TimeOutService.SERVICE_ID
	}
)
public class TimeOutService implements IQueueService
{
	
	public static final String SERVICE_ID = "org.sodeac.eventdispatcher.common.queueservice.timeout";

	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, ITaskControl taskControl, List<IQueueTask> currentProcessedJobList)
	{
		TimeOutServiceAdapter adapter = queue.getConfigurationPropertyBlock().getAdapter(TimeOutServiceAdapter.class);
		long nextTimeOut = adapter.calculateNextTimeOutTimestamp();
		
		if(nextTimeOut <= System.currentTimeMillis())
		{
			adapter.onTimeout(queue, propertyBlock, taskControl);
			queue.getConfigurationPropertyBlock().removeAdapter(TimeOutServiceAdapter.class);
			return;
		}
		taskControl.setExecutionTimeStamp(nextTimeOut, true);

	}

}
