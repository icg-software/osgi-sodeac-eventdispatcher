package org.sodeac.eventdispatcher.common.queueservice;

import java.util.concurrent.TimeUnit;

import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueChildScope;

public class ScopeTimeoutAdapter extends TimeOutServiceAdapter
{
	public ScopeTimeoutAdapter(long timeOut)
	{
		super(timeOut);
	}
	
	public ScopeTimeoutAdapter(long timeOut, TimeUnit unit)
	{
		super(timeOut,unit);
	}

	@Override
	public void onTimeout(IQueue queue,IPropertyBlock propertyBlock, ITaskControl taskControl)
	{
		((IQueueChildScope)queue).dispose();
	}

}
