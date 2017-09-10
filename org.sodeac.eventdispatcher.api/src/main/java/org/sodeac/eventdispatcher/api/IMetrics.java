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

public interface IMetrics
{
	public long getLastHeartBeat();
	public void heartBeat();
	
	public <T> T getGaugeValue(String name, Class<T> type);
	public void registerQueueGauge(String name, IGauge<?> gauge);
}
