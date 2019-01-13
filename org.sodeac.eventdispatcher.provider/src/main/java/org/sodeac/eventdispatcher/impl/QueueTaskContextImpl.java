package org.sodeac.eventdispatcher.impl;

import java.util.List;

import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueTaskContext;
import org.sodeac.eventdispatcher.api.ITaskControl;

public class QueueTaskContextImpl implements IQueueTaskContext
{
	private IQueue queue;
	private TaskContainer dueTask;
	private List<IQueueTask> currentProcessedTaskList;
	
	@Override
	public IQueue getQueue()
	{
		return this.queue;
	}

	@Override
	public IMetrics getTaskMetrics()
	{
		return this.dueTask.getMetrics();
	}

	@Override
	public IPropertyBlock getTaskPropertyBlock()
	{
		return this.dueTask.getPropertyBlock();
	}

	@Override
	public ITaskControl getTaskControl()
	{
		return this.dueTask.getTaskControl();
	}

	@Override
	public List<IQueueTask> currentProcessedTaskList()
	{
		return this.currentProcessedTaskList;
	}

	public void setQueue(IQueue queue)
	{
		this.queue = queue;
	}

	public void setCurrentProcessedTaskList(List<IQueueTask> currentProcessedTaskList)
	{
		this.currentProcessedTaskList = currentProcessedTaskList;
	}

	public void setDueTask(TaskContainer dueTask)
	{
		this.dueTask = dueTask;
	}
}
