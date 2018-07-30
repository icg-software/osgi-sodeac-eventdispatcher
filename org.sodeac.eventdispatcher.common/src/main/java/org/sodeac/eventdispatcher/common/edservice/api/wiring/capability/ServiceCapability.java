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
package org.sodeac.eventdispatcher.common.edservice.api.wiring.capability;

import org.sodeac.eventdispatcher.common.edservice.api.wiring.Capability;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.CapabilityStringValue;

public class ServiceCapability extends Capability
{
	public static final String NAMESPACE = "service";
	public static final String CAPABILITY_NAME_ID = "service";
	
	public ServiceCapability(String domain)
	{
		super(NAMESPACE, CAPABILITY_NAME_ID, new CapabilityStringValue(domain), true);
	}
}
