package org.sodeac.eventdispatcher.extension.api;

import java.util.Map;

import org.sodeac.eventdispatcher.api.IEventController;

public interface IEventDispatcherExtension
{
	public void setConnected(boolean connected);
	public void registerEventController(IEventController eventController,Map<String, ?> properties);
	public void unregisterEventController(IEventController eventQueueConfiguration);
}
