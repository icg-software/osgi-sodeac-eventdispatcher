package org.sodeac.eventdispatcher.api;

public interface IPeriodicQueueJob extends IQueueJob
{
	public long getPeriodicRepetitionInterval();
}
