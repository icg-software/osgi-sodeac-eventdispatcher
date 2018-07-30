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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sodeac.eventdispatcher.common.edservice.api.wiring.Requirement;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.requirement.DomainRequirement;
import org.sodeac.eventdispatcher.common.edservice.api.wiring.requirement.ServiceRequirement;

public class DiscoverEServiceRequest
{
	public DiscoverEServiceRequest(String domain, String serviceId,  List<Requirement> requirements, TimeOut timeOut)
	{
		super();
		if(requirements == null)
		{
			requirements = new ArrayList<Requirement>();
		}
		if((domain != null) && (!domain.isEmpty()))
		{
			requirements.add(new DomainRequirement(domain));
		}
		if((serviceId != null) && (!serviceId.isEmpty()))
		{
			requirements.add(new ServiceRequirement(serviceId));
		}
		
		this.timeout = timeOut;
		this.requirementList = Collections.unmodifiableList(requirements);
	}
	
	private TimeOut timeout;
	private List<Requirement> requirementList;
	
	public TimeOut getTimeout()
	{
		return timeout;
	}
	public List<Requirement> getRequirementList()
	{
		return requirementList;
	}
	
	
}
