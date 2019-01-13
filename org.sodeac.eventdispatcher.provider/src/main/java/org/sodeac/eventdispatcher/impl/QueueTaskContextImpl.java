package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
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
	private List<IQueueTask> currentProcessedTaskListWritable;
	private List<IQueueTask> currentProcessedTaskListReadOnly;
	private List<TaskContainer> dueTaskList;
	
	public QueueTaskContextImpl(List<TaskContainer> dueTaskList)
	{
		super();
		this.dueTaskList = dueTaskList;
		currentProcessedTaskListWritable = new ArrayList<IQueueTask>();
		currentProcessedTaskListReadOnly = Collections.unmodifiableList(currentProcessedTaskListWritable);
	}
	
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
		if(currentProcessedTaskList == null)
		{
			currentProcessedTaskListWritable.clear();
			for(TaskContainer taskContainer : this.dueTaskList)
			{
				currentProcessedTaskListWritable.add(taskContainer.getTask());
			}
		}
		this.currentProcessedTaskList = this.currentProcessedTaskListReadOnly;
		return this.currentProcessedTaskList;
	}

	public void setQueue(IQueue queue)
	{
		this.queue = queue;
	}

	public void resetCurrentProcessedTaskList()
	{
		this.currentProcessedTaskList = null;
		if(! this.currentProcessedTaskListWritable.isEmpty())
		{
			this.currentProcessedTaskListWritable.clear();
		}
	}

	public void setDueTask(TaskContainer dueTask)
	{
		this.dueTask = dueTask;
	}
}
