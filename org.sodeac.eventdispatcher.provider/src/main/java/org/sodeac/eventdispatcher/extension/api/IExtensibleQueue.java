package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IQueue;

public interface IExtensibleQueue extends IQueue
{
	public String getCategory();
	public void setCategory(String category);
	public String getName();
	public void setName(String name);
	
	// TODO Schedule Event
	// TODO Clear EventQueue
	// TODO Create Snapshot Overview
	// TODO Create Complete Snapshot/Live (maxValues)(Jobs/EventQueue/Worker/Properties/Metrics/JobMetrics/Signals/Scope ... )
}
