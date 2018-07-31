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
package org.sodeac.eventdispatcher.common.reactiveservice.api.wiring.requirement;

import org.sodeac.eventdispatcher.common.reactiveservice.api.wiring.Requirement;
import org.sodeac.eventdispatcher.common.reactiveservice.api.wiring.capability.ServiceCapability;

public class ServiceRequirement extends Requirement
{
	public ServiceRequirement(String serviceId)
	{
		super(ServiceCapability.NAMESPACE, "(" + ServiceCapability.CAPABILITY_NAME_ID + "=" + serviceId + ")", false);
	}
}
