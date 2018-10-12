package org.sodeac.eventdispatcher.common.queueservice;

import java.util.concurrent.TimeUnit;

import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;

public abstract class TimeOutServiceAdapter
{
	public TimeOutServiceAdapter(long timeOut)
	{
		super();
		this.lastHeartBeat = System.currentTimeMillis();
		this.timeOut = timeOut;
	}
	
	public TimeOutServiceAdapter(long timeOut,TimeUnit unit)
	{
		super();
		if(unit == null)
		{
			unit = TimeUnit.MILLISECONDS;
		}
		this.lastHeartBeat = System.currentTimeMillis();
		this.timeOut = unit.toMillis(timeOut);
	}
	private long lastHeartBeat;
	private long timeOut ;
	
	public long getLastHeartBeatTimeStamp()
	{
		return lastHeartBeat;
	}
	public void heartBeat()
	{
		this.lastHeartBeat = System.currentTimeMillis();
	}
	public long getTimeOutValue()
	{
		return timeOut;
	} 
	public long calculateNextTimeOutTimestamp()
	{
		return this.lastHeartBeat + timeOut;
	}
	
	public abstract void onTimeout(IQueue queue,IPropertyBlock propertyBlock, ITaskControl jobControl);
}
