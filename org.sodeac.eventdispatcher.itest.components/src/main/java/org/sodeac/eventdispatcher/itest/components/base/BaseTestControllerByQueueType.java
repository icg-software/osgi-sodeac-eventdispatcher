package org.sodeac.eventdispatcher.itest.components.base;

import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IQueue;

public class BaseTestControllerByQueueType implements IEventController, IOnQueueObserve
{

	@Override
	public void onQueueObserve(IQueue queue)
	{
		// TODO Auto-generated method stub

	}

}
