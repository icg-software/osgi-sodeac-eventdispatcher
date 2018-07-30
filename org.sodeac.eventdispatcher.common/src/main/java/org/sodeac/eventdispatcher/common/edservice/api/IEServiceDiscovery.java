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
package org.sodeac.eventdispatcher.common.edservice.api;

import org.sodeac.eventdispatcher.api.IEventController;

public interface IEServiceDiscovery
{
	public static final String EVENT_TOPIC_ALL = "org/sodeac/eventdispatcher/service/discovery/*";
	public static final String EVENT_TOPIC_REQUEST = "org/sodeac/eventdispatcher/service/discovery/request";
	public static final String EVENT_TOPIC_RESPONSE = "org/sodeac/eventdispatcher/service/discovery/response";
	public static final String SERVICE_PROPERTY__CONSUME_EVENTS_DISCOVER_SERVICE = IEventController.PROPERTY_CONSUME_EVENT_TOPIC+"=" + IEServiceDiscovery.EVENT_TOPIC_REQUEST;
	
	public static String EVENT_PROPERTY_REQUEST_ID = "service.discovery.request.id";
	public static String EVENT_PROPERTY_REQUEST_OBJ = "service.discovery.request.object";
	public static String EVENT_PROPERTY_RESPONSE_OBJ = "service.discovery.response.object";
	
	public static final String QUEUE_ID = "org.sodeac.eventdispatcher.service.discovery";
	
	public DiscoverEServiceResponse discoverService(DiscoverEServiceRequest discoverServerRequest);
}
