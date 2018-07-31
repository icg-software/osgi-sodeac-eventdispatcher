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

import java.util.List;
import java.util.UUID;

import org.sodeac.eventdispatcher.common.reactiveservice.api.wiring.Capability;
import org.sodeac.xuri.URI;

public interface IReactiveServiceReference
{
	public UUID getId();
	public URI getServiceLocation();
	public List<Capability> getCapabilityList(); 
	public Capability getCapability(String namespace,String name);
}
