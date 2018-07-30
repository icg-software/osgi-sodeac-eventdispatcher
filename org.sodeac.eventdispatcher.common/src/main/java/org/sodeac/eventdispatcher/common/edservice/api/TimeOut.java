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

import java.util.concurrent.TimeUnit;

public class TimeOut
{
	private long time = -1;
	private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
	
	public TimeOut(long time, TimeUnit timeUnit)
	{
		super();
		this.time = time;
		this.timeUnit = timeUnit;
	}
	
	public long getTime()
	{
		return time;
	}
	public void setTime(long time)
	{
		this.time = time;
	}
	public TimeUnit getTimeUnit()
	{
		return timeUnit;
	}
	public void setTimeUnit(TimeUnit timeUnit)
	{
		this.timeUnit = timeUnit;
	}
	
	
}
