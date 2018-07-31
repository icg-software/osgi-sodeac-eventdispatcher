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

import org.sodeac.xuri.ldapfilter.ComparativeOperator;
import org.sodeac.xuri.ldapfilter.IMatchable;

public class CapabilityStringValue implements ICapabilityValue, IMatchable
{
	private String value;
	
	public CapabilityStringValue(String expression)
	{
		super();
		this.value = expression;
	}

	@Override
	public String getExpression()
	{
		return value;
	}
	
	@Override
	public boolean matches(ComparativeOperator operator, String key, String valueExpression)
	{
		if(operator == null)
		{
			return false;
		}
		
		if(valueExpression == null)
		{
			return this.value == null;
		}
		
		if(operator == ComparativeOperator.EQUAL)
		{
			if(valueExpression.trim().equals("*"))
			{
				return true;
			}
			if(valueExpression.equals(this.value))
			{
				return true;
			}
		}
		
		if(operator == ComparativeOperator.APPROX)
		{
			if(valueExpression.equalsIgnoreCase(this.value))
			{
				return true;
			}
			
			if(this.value != null)
			{
				if(this.value.matches(valueExpression))
				{
					return true;
				}
			}
		}
		
		return false;
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + " " + this.getExpression();
	}
	
	

}
