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
package org.sodeac.eventdispatcher.extension.jmx;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;

public class Counter implements CounterMBean
{
	protected ObjectName counterObjectName = null;
	private IExtensibleCounter counter = null;
	public Counter(ObjectName counterObjectName, IExtensibleCounter counter)
	{
		super();
		this.counterObjectName = counterObjectName;
		this.counter = counter;
	}
	
	@Override
	public long getValue()
	{
		return this.counter.getCount();
	}

	@Override
	public String getName()
	{
		return this.counter.getName();
	}
	
	@Override
	public String getKey()
	{
		return this.counter.getKey();
	}
}
