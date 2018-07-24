/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;

public class MetricFilterByName implements MetricFilter
{
	private String name = null;
	
	public MetricFilterByName(String name)
	{
		super();
		this.name = name;
	}

	@Override
	public boolean matches(String name, Metric metric)
	{
		if(name == null)
		{
			return false;
		}
		return name.equals(this.name);
	}

}
