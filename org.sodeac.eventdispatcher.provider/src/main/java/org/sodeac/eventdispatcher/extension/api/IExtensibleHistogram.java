package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IHistogram;

public interface IExtensibleHistogram extends IHistogram
{
	public String getKey();
	public String getName();
	public IExtensibleMetrics getMetrics();
}
