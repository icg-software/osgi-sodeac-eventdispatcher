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
		IQueueService.PROPERTY_PERIODIC_REPETITION_INTERVAL+"="+ CompressorStatics.HEARTBEAT,
		IQueueService.PROPERTY_SERVICE_ID+"=" + CompressorStatics.COMPRESSOR_SERVICE_ID,
		IEventController.CONSUME_EVENT_TOPIC +"=" + CompressorStatics.TOPIC_RAW_EVENT
	}
)
public class Compressor implements IQueueService,IEventController,IOnEventScheduled
{
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;

	@Override
	public void onEventScheduled(IQueuedEvent event)
	{
		//System.out.println("Schedule Event: " + event.getEvent().getTopic());
		//System.out.println("List " + event.getQueue().getEventList(null, null, null).size());
		
		// Minimal offset
		// MEtrics??? job counter / job timout / event send/rcv counter/time (über interface parametrisierbar machen(ja/nein))
		// garbage collector vor metrics !!!!
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
	}


	@Override
	public void configure(String id, IMetrics metrics, IPropertyBlock propertyBlock, IJobControl jobControl){}
}
