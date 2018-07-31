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
package org.sodeac.eventdispatcher.common.reactiveservice.api;

import org.sodeac.eventdispatcher.api.IEventController;

public interface IReactiveServiceDiscovery
{
	public static final String EVENT_TOPIC_ALL = "org/sodeac/eventdispatcher/reactiveservice/discovery/*";
	public static final String EVENT_TOPIC_REQUEST = "org/sodeac/eventdispatcher/reactiveservice/discovery/request";
	public static final String EVENT_TOPIC_RESPONSE = "org/sodeac/eventdispatcher/reactiveservice/discovery/response";
	public static final String SERVICE_PROPERTY__CONSUME_EVENTS_DISCOVER_SERVICE = IEventController.PROPERTY_CONSUME_EVENT_TOPIC+"=" + IReactiveServiceDiscovery.EVENT_TOPIC_REQUEST;
	
	public static String EVENT_PROPERTY_REQUEST_ID = "reactiveservice.discovery.request.id";
	public static String EVENT_PROPERTY_REQUEST_OBJ = "reactiveservice.discovery.request.object";
	public static String EVENT_PROPERTY_RESPONSE_OBJ = "reactiveservice.discovery.response.object";
	
	public static final String QUEUE_ID = "org.sodeac.eventdispatcher.reactiveservice.discovery";
	
	public DiscoverReactiveServiceResponse discoverService(DiscoverReactiveServiceRequest discoverServerRequest);
}
