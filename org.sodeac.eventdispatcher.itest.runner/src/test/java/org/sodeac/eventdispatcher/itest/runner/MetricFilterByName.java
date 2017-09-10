package org.sodeac.eventdispatcher.itest.runner;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class MetricFilterByName implements MetricFilter
{
	private String name = null;
	
	public MetricFilterByName(String name)
	{
		super();
		this.name = name;
	}

	@Override
	public boolean matches(String name, Metric metric)
	{
		if(name == null)
		{
			return false;
		}
		return name.equals(this.name);
	}

}
