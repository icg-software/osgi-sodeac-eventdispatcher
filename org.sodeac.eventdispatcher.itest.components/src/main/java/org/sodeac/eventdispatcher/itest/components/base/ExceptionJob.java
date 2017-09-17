package org.sodeac.eventdispatcher.itest.components.base;

import java.util.List;

import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;

public class ExceptionJob implements IQueueJob
{

	public ExceptionJob()
	{
		super();
	}
	
	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}

	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl,List<IQueueJob> currentProcessedJobList)
	{
		throw new RuntimeException("JobException");
	}

}
