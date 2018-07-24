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

import org.sodeac.eventdispatcher.extension.api.IExtensibleGauge;

public class Gauge implements GaugeMBean
{
	protected ObjectName gaugeObjectName = null;
	private IExtensibleGauge<?> gauge = null;
	public Gauge(ObjectName gaugeObjectName, IExtensibleGauge<?> gauge)
	{
		super();
		this.gaugeObjectName = gaugeObjectName;
		this.gauge = gauge;
	}
	
	@Override
	public String getName()
	{
		return this.gauge.getName();
	}
	
	@Override
	public String getKey()
	{
		return this.gauge.getKey();
	}

	@Override
	public String getValue()
	{
		Object val = this.gauge.getValue();
		if(val == null)
		{
			return "NULL";
		}
		return val.toString();
	}

	@Override
	public String getType()
	{
		Object val = this.gauge.getValue();
		if(val == null)
		{
			return "";
		}
		return val.getClass().getCanonicalName();
	}

}
