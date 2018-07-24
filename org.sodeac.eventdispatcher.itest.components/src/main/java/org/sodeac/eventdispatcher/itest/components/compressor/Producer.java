/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.itest.components.compressor;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

@Component
(
	immediate=true,
	service=EventHandler.class,
	property=EventConstants.EVENT_TOPIC + "=" +	CompressorStatics.TOPIC_START_TEST
)
public class Producer implements EventHandler
{

	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile EventAdmin eventAdmin;
	
	
	@Override
	public void handleEvent(Event event)
	{
		if(event.getTopic().equals(CompressorStatics.TOPIC_START_TEST))
		{
			for(int i = 0; i < 100 ; i++)
			{
				try
				{
					Map<String,Object> properties = new HashMap<String,Object>();
					properties.put(CompressorStatics.PROPERTY_COUNT, i);
					Event rawEvent = new Event(CompressorStatics.TOPIC_RAW_EVENT,properties);
					eventAdmin.sendEvent(rawEvent);
					if((i == 30) || (i == 70))
					{
						Thread.sleep(1600);
					}
					else
					{
						Thread.sleep(100);
					}
				}
				catch (Exception e) {}
			}
		}
	}
}
