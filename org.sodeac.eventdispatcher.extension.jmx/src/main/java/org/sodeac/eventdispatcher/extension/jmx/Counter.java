package org.sodeac.eventdispatcher.extension.jmx;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;

public class Counter implements CounterMBean
{
	protected ObjectName counterObjectName = null;
	private IExtensibleCounter counter = null;
	public Counter(ObjectName counterObjectName, IExtensibleCounter counter)
	{
		super();
		this.counterObjectName = counterObjectName;
		this.counter = counter;
	}
	
	@Override
	public long getValue()
	{
		return this.counter.getCount();
	}

	@Override
	public String getName()
	{
		return this.counter.getName();
	}
	
	@Override
	public String getKey()
	{
		return this.counter.getKey();
	}
}
