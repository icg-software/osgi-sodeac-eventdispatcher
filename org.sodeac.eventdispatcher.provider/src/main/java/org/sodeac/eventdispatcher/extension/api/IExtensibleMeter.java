package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IMeter;

public interface IExtensibleMeter extends IMeter
{
	public String getKey();
	public String getName();
	public IExtensibleMetrics getMetrics();
}
