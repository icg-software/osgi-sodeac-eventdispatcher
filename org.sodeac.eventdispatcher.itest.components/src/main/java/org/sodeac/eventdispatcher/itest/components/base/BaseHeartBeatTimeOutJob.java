package org.sodeac.eventdispatcher.itest.components.base;

import java.util.List;

import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;

public class BaseHeartBeatTimeOutJob implements IQueueJob
{
	public BaseHeartBeatTimeOutJob()
	{
		super();
	}
	
	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}

	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl,List<IQueueJob> currentProcessedJobList)
	{
		try
		{
			Thread.sleep(1000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		metrics.heartBeat();
		
		try
		{
			Thread.sleep(2000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		metrics.heartBeat();
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		metrics.heartBeat();
		
		try
		{
			Thread.sleep(4000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		metrics.heartBeat();
		
		try
		{
			Thread.sleep(5000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		metrics.heartBeat();
	}

}
