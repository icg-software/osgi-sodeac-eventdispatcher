package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.ITimer;

public interface IExtensibleTimer extends ITimer
{
	public String getKey();
	public String getName();
	public IExtensibleMetrics getMetrics();
}
