package org.sodeac.eventdispatcher.extension.jmx;

public interface EventControllerMBean
{
	public boolean isImplementsOnEventScheduled();
	public boolean isImplementsOnFireEvent();
	public boolean isImplementsOnJobDone();
	public boolean isImplementsOnJobError();
	public boolean isImplementsOnJobTimeout();
	public boolean isImplementsOnQueueObserve();
	public boolean isImplementsOnQueueReserve();
	public boolean isImplementsOnQueueSignal();
	public boolean isImplementsOnRemoveEvent();
	public String getControllerClassName();
	public String showStateInfo();
	public String showDescription();
}
