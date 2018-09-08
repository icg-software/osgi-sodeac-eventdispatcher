/*******************************************************************************
 * Copyright (c) 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.List;

import org.sodeac.eventdispatcher.api.IPropertyBlockModifyListener;
import org.sodeac.eventdispatcher.api.PropertyBlockModifyItem;

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
		((EventDispatcherImpl)queue.getDispatcher()).onConfigurationModify(this.queue, key);
	}

	@Override
	public void onModifySet(List<PropertyBlockModifyItem> modifySet)
	{
		if(modifySet == null)
		{
			return;
		}
		if(modifySet.isEmpty())
		{
			return;
		}
		String[] attributes = new String[modifySet.size()];
		int index = 0;
		for(PropertyBlockModifyItem item : modifySet)
		{
			attributes[index++] = item.getKey();
		}
 		((EventDispatcherImpl)queue.getDispatcher()).onConfigurationModify(this.queue,attributes);
	}

}
