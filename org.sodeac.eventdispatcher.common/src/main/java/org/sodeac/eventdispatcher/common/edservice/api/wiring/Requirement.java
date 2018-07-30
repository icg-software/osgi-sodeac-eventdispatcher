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
package org.sodeac.eventdispatcher.common.edservice.api.wiring;

import java.util.ArrayList;
import java.util.List;

public class Requirement
{
	public static final String EFFECTIVE_SERVICE = "eventdrivenservice";
	public static final String FILTER_TYPE_LDAP = "ldapfilter";
	
	private String namespace;
	private String effective = EFFECTIVE_SERVICE;
	private boolean optional = false;
	private String filterType = FILTER_TYPE_LDAP;
	private String filterExpression;
	
	public Requirement(String namespace, String filter)
	{
		this(namespace,filter,false);
	}
	
	public Requirement(String namespace, String filterExpression,boolean optional)
	{
		super();
		this.namespace = namespace;
		this.filterExpression = filterExpression;
		this.optional = optional;
	}
	
	public String getNamespace()
	{
		return namespace;
	}
	public String getEffective()
	{
		return effective;
	}
	public boolean isOptional()
	{
		return optional;
	}
	public String getFilterType()
	{
		return filterType;
	}
	public String getFilterExpression()
	{
		return filterExpression;
	}	

	public static List<Requirement> toList(Requirement[] requirements)
	{
		List<Requirement> list = new ArrayList<Requirement>();
		if(requirements == null)
		{
			return list;
		}
		for(Requirement requirement : requirements)
		{
			if(requirement == null)
			{
				continue;
			}
			if(list.contains(requirement))
			{
				continue;
			}
			list.add(requirement);
		}
		return list;
	}
}
