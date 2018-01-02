package org.sodeac.eventdispatcher.impl;

import java.util.List;

import org.sodeac.eventdispatcher.extension.api.IPropertyBlockModifyListener;
import org.sodeac.eventdispatcher.extension.api.PropertyBlockModifyItem;

public class QueueConfigurationModifyListener implements IPropertyBlockModifyListener
{
	private QueueImpl queue;
	
	public QueueConfigurationModifyListener(QueueImpl queue)
	{
		super();
		this.queue = queue;
	}

	@Override
	public void onModify(ModifyType type, String key, Object valueOld, Object valueNew)
	{
		((EventDispatcherImpl)queue.getDispatcher()).onConfigurationModify(this.queue);
	}

	@Override
	public void onModifySet(List<PropertyBlockModifyItem> modifySet)
	{
		((EventDispatcherImpl)queue.getDispatcher()).onConfigurationModify(this.queue);
	}

}
