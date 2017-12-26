package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.ICounter;

public interface IExtensibleCounter extends ICounter
{
	public String getKey();
	public String getName();
	public IExtensibleMetrics getMetrics();
}
