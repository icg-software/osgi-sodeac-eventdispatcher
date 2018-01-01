package org.sodeac.eventdispatcher.impl;

import org.sodeac.eventdispatcher.extension.api.IPropertyBlockModifyListener;

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

}
