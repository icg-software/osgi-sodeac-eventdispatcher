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

import java.util.List;

import org.sodeac.eventdispatcher.common.edservice.api.wiring.Capability;

public interface IEventDrivenService
{
	public static final String PROPERTY_SERVICE_QUEUE_ID = "service.queueid";
	public static final String PROPERTY_DOMAIN = "service.domain";
	public static final String PROPERTY_SERVICE_ID = "service.id";
	
	public List<Capability> getCapabilityList();
}
