package org.sodeac.eventdispatcher.impl;

import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueWorker;

public class QueueWorkerWrapper implements IQueueWorker
{
	private QueueWorker worker = null;
	
	public QueueWorkerWrapper(QueueWorker worker)
	{
		super();
		this.worker = worker;
	}

	@Override
	public void interrupt()
	{
		worker.interrupt();
	}

	@Override
	public IQueue getQueue()
	{
		return worker.getEventQueue();
	}

}
