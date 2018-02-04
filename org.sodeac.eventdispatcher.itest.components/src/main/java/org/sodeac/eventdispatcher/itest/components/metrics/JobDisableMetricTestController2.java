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
package org.sodeac.eventdispatcher.itest.components.metrics;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.sodeac.eventdispatcher.api.IDisableMetricsOnQueueObserve;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;

@Component
(
	immediate=true,
	service={IEventController.class,EventHandler.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_ID+"="+JobDisableMetricTestController2.QUEUE_ID,
		EventConstants.EVENT_TOPIC+"=" + JobDisableMetricTestController2.RUN_EVENT
	}
)
public class JobDisableMetricTestController2 extends JobMetricTestController implements IDisableMetricsOnQueueObserve
{
	public static final String QUEUE_ID 					= "jobdisablemetrics2"	;
	public static final String RUN_EVENT 					= "org/sodeac/eventdispatcher/itest/metrics/jobdisablemetrics2/run";
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	@Override
	public void handleEvent(Event event)
	{
		this.dispatcher.schedule(JobDisableMetricTestController2.QUEUE_ID, event);
	}
	
}