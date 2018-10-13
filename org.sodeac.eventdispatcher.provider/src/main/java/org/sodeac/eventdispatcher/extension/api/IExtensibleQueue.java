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
package org.sodeac.eventdispatcher.extension.api;

import org.sodeac.eventdispatcher.api.IQueue;

public interface IExtensibleQueue extends IQueue
{
	public String getCategory();
	public void setCategory(String category);
	public String getName();
	public void setName(String name);
	
	public int getEventListLimit();
	public void setEventListLimit(int eventListLimit);
	
	// TODO Schedule Event
	// TODO Clear EventQueue
	// TODO Create Snapshot Overview
	// TODO Create Complete Snapshot/Live (maxValues)(Tasks/EventQueue/Worker/Properties/Metrics/TaskMetrics/Signals/Scope ... )
}
