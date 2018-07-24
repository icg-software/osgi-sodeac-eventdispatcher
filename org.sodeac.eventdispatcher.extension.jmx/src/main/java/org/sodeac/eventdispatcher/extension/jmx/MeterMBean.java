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

public interface MeterMBean
{
	public String getName();
	public String getKey();
	
	public long getCount();
	public double getMeanRate();
	public double getOneMinuteRate();
	public double getFiveMinuteRate();
	public double getFifteenMinuteRate();
}
