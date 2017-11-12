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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.itest.components.TracingEvent;
import org.sodeac.eventdispatcher.itest.components.TracingObject;

@Component
(
	immediate=true,
	service=EventHandler.class,
	property=
	{
		EventConstants.EVENT_TOPIC + "=" +	CompressorStatics.TOPIC_PUBLISH_TRACING_OBJECT,
		EventConstants.EVENT_TOPIC + "=" +	CompressorStatics.TOPIC_COMPRESSED_EVENT
	}
)
public class Consumer implements EventHandler
{
	
	private TracingObject tracingObject = null;

	@Override
	public void handleEvent(Event event)
	{
		if(event.getTopic().equals(CompressorStatics.TOPIC_PUBLISH_TRACING_OBJECT))
		{
			this.tracingObject = (TracingObject)event.getProperty(TracingObject.class.getCanonicalName());
			return;
		}
		
		if(event.getTopic().equals(CompressorStatics.TOPIC_COMPRESSED_EVENT))
		{
			this.tracingObject.getTracingEventList().add(new TracingEvent(TracingEvent.SEND_EVENT, null, event));
			//System.out.println(System.currentTimeMillis() + ": " + event.getProperty(CompressorStatics.PROPERTY_COUNT_SIZE));
		}
	}

}
