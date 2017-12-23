/*******************************************************************************
 * Copyright (c) 2017 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.components.compressor;

import java.util.HashMap;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IJobControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IOnEventScheduled;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.IQueuedEvent;

@Component
(
	immediate=true,
	service={IEventController.class,IQueueService.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID + "=" + CompressorStatics.QUEUE_ID,
		IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL+"="+ CompressorStatics.SERVICE_REPETITION_INTERVAL,
		IQueueService.PROPERTY_SERVICE_ID+"=" + CompressorStatics.COMPRESSOR_SERVICE_ID,
		IEventController.PROPERTY_CONSUME_EVENT_TOPIC +"=" + CompressorStatics.TOPIC_START_COMPRESSOR,
		IEventController.PROPERTY_CONSUME_EVENT_TOPIC +"=" + CompressorStatics.TOPIC_STOP_COMPRESSOR,
		IEventController.PROPERTY_CONSUME_EVENT_TOPIC +"=" + CompressorStatics.TOPIC_RAW_EVENT
	}
)
public class Compressor implements IQueueService,IEventController,IOnEventScheduled
{
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	private boolean run = false;
	private int minCount = -1;
	private int maxCount = -1;
	private int collectedEvents = 0;
	
	@Override
	public void onEventScheduled(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(CompressorStatics.TOPIC_START_COMPRESSOR))
		{
			this.run = true;
			return;
		}
		
		if(event.getEvent().getTopic().equals(CompressorStatics.TOPIC_STOP_COMPRESSOR))
		{
			this.run = false;
			return;
		}
		
		if(run)
		{
			if(event.getEvent().getTopic().equals(CompressorStatics.TOPIC_RAW_EVENT))
			{
				int count = (Integer)event.getNativeEventProperties().get(CompressorStatics.PROPERTY_COUNT);
				if((minCount < 0) || (count < minCount))
				{
					minCount = count;
				}
				if((maxCount < 0) || (count > maxCount))
				{
					maxCount = count;
				}
				collectedEvents++;
				
				this.sendEvents(event.getQueue());
			}
		}
	}
	
	@Override
	public void run
	(
		IQueue queue, 
		IMetrics metrics, 
		IPropertyBlock propertyBlock, 
		IJobControl jobControl,
		List<IQueueJob> currentProcessedJobList
	)
	{
		if(run)
		{
			this.sendEvents(queue);
		}
	}
	
	private void sendEvents(IQueue queue)
	{
		long now = System.currentTimeMillis();
		Long lastSend = queue.getMetrics().getGauge(Long.class, IMetrics.GAUGE_LAST_SEND_EVENT).getValue();
		queue.rescheduleJob(CompressorStatics.COMPRESSOR_SERVICE_ID, now + CompressorStatics.HEARTBEAT_INTERVAL, -1, -1);
		
		if(collectedEvents == 0)
		{
			if((lastSend == null) || (lastSend.longValue() <= (now - CompressorStatics.HEARTBEAT_INTERVAL)))
			{
				HashMap<String,Object> properties = new HashMap<String,Object>();
				properties.put(CompressorStatics.PROPERTY_COUNT_SIZE, 0);
				queue.sendEvent(CompressorStatics.TOPIC_COMPRESSED_EVENT, properties);
			}
		}
		else
		{
			if((lastSend == null) || (lastSend.longValue() <= (now - CompressorStatics.MINIMAL_INTERVAL)))
			{
				HashMap<String,Object> properties = new HashMap<String,Object>();
				properties.put(CompressorStatics.PROPERTY_COUNT_SIZE, collectedEvents);
				properties.put(CompressorStatics.PROPERTY_COUNT_MIN, minCount);
				properties.put(CompressorStatics.PROPERTY_COUNT_MAX, maxCount);
				queue.sendEvent(CompressorStatics.TOPIC_COMPRESSED_EVENT, properties);
				collectedEvents = 0;
				minCount = -1;
				maxCount = -1;
			}
			else
			{
				queue.rescheduleJob(CompressorStatics.COMPRESSOR_SERVICE_ID, lastSend + CompressorStatics.MINIMAL_INTERVAL, -1, -1);
			}
		}
	}


	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}
}
