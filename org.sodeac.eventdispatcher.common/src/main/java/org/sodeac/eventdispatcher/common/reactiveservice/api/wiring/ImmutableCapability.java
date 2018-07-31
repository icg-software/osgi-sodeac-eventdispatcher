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
package org.sodeac.eventdispatcher.common.reactiveservice.api.wiring;

public class ImmutableCapability extends Capability
{
	
	public ImmutableCapability(Capability capability)
	{
		super(capability.getNameSpace(), capability.getName(), capability.getValue(),capability.isMandatory());
	}
	
	public Capability addRequirement(Requirement requirement)
	{
		return this;
	}
	
	public ImmutableCapability getImmutable()
	{
		return this;
	}
}
