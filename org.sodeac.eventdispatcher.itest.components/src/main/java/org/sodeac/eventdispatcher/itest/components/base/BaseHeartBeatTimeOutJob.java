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

import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;

public class BaseHeartBeatTimeOutJob implements IQueueJob
{
	public static final long HEARTBEATS_IN_TIME = 1000 + 2000 + 3000;
	
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
		
		metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
		
		try
		{
			Thread.sleep(2000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
		
		try
		{
			Thread.sleep(3000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
		
		try
		{
			Thread.sleep(4000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
		try
		{
			Thread.sleep(5000);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		metrics.setQualityValue(IMetrics.QUALITY_VALUE_LAST_HEARTBEAT, System.currentTimeMillis());
	}

}
