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
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;

public class BaseRecreateTestJob implements IQueueJob
{
	private long sleepValue;
	private String signal;

	public BaseRecreateTestJob(long sleepValue,String signal)
	{
		super();
		this.sleepValue = sleepValue;
		this.signal = signal;
	}
	
	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}

	@Override
	public void run(IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl,List<IQueueJob> currentProcessedJobList)
	{
		TracingObject tracingObject = (TracingObject)propertyBlock.getProperty(TracingObject.class.getName());
		long timeOutTimeStamp = System.currentTimeMillis() + this.sleepValue;
		
		while(timeOutTimeStamp > System.currentTimeMillis())
		{
			try
			{
				Thread.sleep(1000);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
			tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.ON_QUEUE_SIGNAL,queue, "" + signal + "_" + jobControl.isInTimeOut()));
		}
	}

	@Override
	public String toString()
	{
		return super.toString() + "_" + signal;
	}

}
