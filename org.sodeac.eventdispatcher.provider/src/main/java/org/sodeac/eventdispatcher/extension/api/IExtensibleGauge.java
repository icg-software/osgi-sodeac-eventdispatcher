package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IGauge;

public interface IExtensibleGauge<T> extends IGauge<T>
{
	public String getKey();
	public String getName();
	public IExtensibleMetrics getMetrics();
}
