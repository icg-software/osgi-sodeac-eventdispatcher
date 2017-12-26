package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IMetrics;

public interface IExtensibleMetrics extends IMetrics
{
	public boolean isEnabled();
	public String getJobId();
	public IExtensibleQueue getQueue();
}
