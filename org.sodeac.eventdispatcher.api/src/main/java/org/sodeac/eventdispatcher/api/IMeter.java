/*******************************************************************************
 * Copyright (c) 2017 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.api;

public interface IMeter
{
	public void mark();
	public void mark(long n);
	public long getCount();
	public double getMeanRate();
	public double getOneMinuteRate();
	public double getFiveMinuteRate();
	public double getFifteenMinuteRate();
}
