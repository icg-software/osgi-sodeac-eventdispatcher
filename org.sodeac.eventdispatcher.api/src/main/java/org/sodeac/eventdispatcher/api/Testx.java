package org.sodeac.eventdispatcher.api;

import java.util.List;

import org.osgi.service.event.Event;

public class Testx implements IDynamicController
{

	@Override
	public void onFireEvent(Event event, IQueue queue)
	{
		// TODO Auto-generated method stub
		IDynamicController.super.onFireEvent(event, queue);
	}

	@Override
	public void onRemoveEvent(IQueuedEvent event)
	{
		// TODO Auto-generated method stub
		IDynamicController.super.onRemoveEvent(event);
	}

	@Override
	public void onJobTimeout(IQueueJob job)
	{
		// TODO Auto-generated method stub
		IDynamicController.super.onJobTimeout(job);
	}


	
}
