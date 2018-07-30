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
package org.sodeac.eventdispatcher.common.edservice.impl.servicediscovery;

import java.util.List;
import java.util.UUID;

import org.sodeac.eventdispatcher.common.edservice.api.IEServiceReference;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.Capability;
import org.sodeac.xuri.URI;

public class EServiceReference implements IEServiceReference
{
	public EServiceReference(UUID id, URI serviceLocation, List<Capability> capabilityList)
	{
		super();
		this.id = id;
		this.serviceLocation = serviceLocation;
	}
	
	private UUID id;
	private URI serviceLocation;
	private List<Capability> capabilityList;

	@Override
	public UUID getId()
	{
		return this.id;
	}

	@Override
	public URI getServiceLocation()
	{
		return this.serviceLocation;
	}

	@Override
	public List<Capability> getCapabilityList()
	{
		return this.capabilityList;
	}

	@Override
	public Capability getCapability(String namespace, String name)
	{
		for(Capability capabililty : this.capabilityList)
		{
			if((namespace != null) &&(! namespace.equals(capabililty.getNameSpace())))
			{
				continue;
			}
			if((name != null) &&(! name.equals(capabililty.getName())))
			{
				continue;
			}
			return capabililty;
		}
		return null;
	}

}
