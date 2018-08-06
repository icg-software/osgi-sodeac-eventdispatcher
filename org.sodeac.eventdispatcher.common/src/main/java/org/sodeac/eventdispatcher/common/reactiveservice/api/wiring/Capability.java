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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Capability
{
	public Capability(String nameSpace,String name,ICapabilityValue value)
	{
		this(nameSpace, name, value, false);
	}
	
	public Capability(String nameSpace,String name,ICapabilityValue value, boolean mandatory)
	{
		super();
		this.nameSpace = nameSpace;
		this.name = name;
		this.value = value;
		this.mandatory = mandatory;
		this.requirementListLock = new ReentrantLock();
	}
	
	private String nameSpace;
	private String name;
	private ICapabilityValue value;
	private List<Requirement> requirementList;
	private Lock requirementListLock;
	private boolean mandatory;
	
	public String getNameSpace()
	{
		return nameSpace;
	}
	public String getName()
	{
		return name;
	}
	public ICapabilityValue getValue()
	{
		return value;
	}
	public boolean isMandatory()
	{
		return mandatory;
	}

	public Capability addRequirement(Requirement requirement)
	{
		requirementListLock.lock();
		try
		{
			if(requirementList == null)
			{
				requirementList = new ArrayList<Requirement>();
			}
			else if(requirementList.contains(requirement))
			{
				return this;
			}
			requirementList.add(requirement);
			return this;
			
		}
		finally 
		{
			requirementListLock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Requirement> getRequirementList()
	{
		requirementListLock.lock();
		try
		{
			if(requirementList == null)
			{
				return Collections.unmodifiableList(Collections.EMPTY_LIST);
			}
			return Collections.unmodifiableList(new ArrayList<Requirement>(this.requirementList));
		}
		finally 
		{
			requirementListLock.unlock();
		}
	}
	
	public ImmutableCapability getImmutable()
	{
		return new ImmutableCapability(this);
	}
}
