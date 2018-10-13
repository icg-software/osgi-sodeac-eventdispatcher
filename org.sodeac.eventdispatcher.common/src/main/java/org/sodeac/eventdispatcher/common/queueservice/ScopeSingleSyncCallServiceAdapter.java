package org.sodeac.eventdispatcher.common.queueservice;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sodeac.eventdispatcher.api.IQueue;

public class ScopeSingleSyncCallServiceAdapter<T>
{
	private Exchanger<T> exchanger = null;
	private long defaultTimeout; 
	private TimeUnit defaultUnit;
	private IQueue queue;
	private T value;
	
	protected IQueue getQueue()
	{
		return queue;
	}
	protected T getValue()
	{
		return value;
	}
	protected Exchanger<T> getExchanger()
	{
		return exchanger;
	}
	protected void setQueue(IQueue queue)
	{
		this.queue = queue;
	}
	public ScopeSingleSyncCallServiceAdapter(long defaultTimeout, TimeUnit defaultUnit)
	{
		super();
		this.exchanger = new Exchanger<T>();
		this.defaultTimeout = defaultTimeout;
		this.defaultUnit = defaultUnit;
	}
	public void publishResult(T value)
	{
		this.value = value;
		if(queue != null)
		{
			queue.rescheduleTask(ScopeSingleSyncCallService.SERVICE_ID,System.currentTimeMillis(),-1,-1);
		}
	}
	public T waitForResult(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
	{
		try
		{
			return exchanger.exchange
			(
				null, 
				(timeout < 1L) ? defaultTimeout : timeout, 
				((timeout < 1L) || (unit == null)) ? defaultUnit : unit
			);
		}
		finally
		{
		}
	}
}
