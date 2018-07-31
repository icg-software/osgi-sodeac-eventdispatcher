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

public class ReactiveServiceLifecycle
{
	public static final String EVENT_TOPIC_COMMAND_CONNECT = "org/sodeac/eventdispatcher/service/lifecycle/command/connect";
	public static final String EVENT_TOPIC_ACK_CONNECTED = "org/sodeac/eventdispatcher/service/lifecycle/acknowledge/connected";
	//public static final String EVENT_TYPE_DATASET = "org.sodeac.eventdispatcher.service.lifecycle.type.dataset";
	
	//public static final String CAT_DATASET = "org.sodeac.eventdispatcher.service.lifecycle.type.dataset";
	
	public static final String PROPERTY_SRV_CONSUMER = "org.sodeac.eventdispatcher.service.property.consumer";
	public static final String PROPERTY_SRV_PUBLISHER = "org.sodeac.eventdispatcher.service.property.publisher";
	public static final String PROPERTY_CAT = "org.sodeac.eventdispatcher.service.property.category";
	public static final String PROPERTY_ID = "org.sodeac.eventdispatcher.service.property.id";
	
	
}
