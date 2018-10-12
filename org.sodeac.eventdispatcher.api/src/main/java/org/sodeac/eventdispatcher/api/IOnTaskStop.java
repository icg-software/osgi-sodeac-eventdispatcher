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
package org.sodeac.eventdispatcher.api;

/**
 * extension interface for {@link IQueueTask} to facilitate prevention of deadlocks and inconsistent data structures in result of {@link java.lang.Thread#stop()} 
 * 
 * @author Sebastian Palarus
 *
 */
public interface IOnTaskStop extends IQueueTask
{
	/**
	 * 
	 * before invoke {@link java.lang.Thread#stop()} this method is invoked until the return-value is less than 1
	 * 
	 * Attention! The call is not synchronized in worker thread!
	 * 
	 * @param requestNumber how many times this request is invoked since {@link IQueueTask#run(IQueue, IMetrics, IPropertyBlock, IJobControl, java.util.List)} is invoked
	 * @param totalMoreTimeUntilNow how many time in ms was requested since {@link IQueueTask#run(IQueue, IMetrics, IPropertyBlock, IJobControl, java.util.List)} is invoked
	 * @param worker worker thread invoked {@link IQueueTask#run(IQueue, IMetrics, IPropertyBlock, IJobControl, java.util.List)} currently runs in timeout
	 * 
	 * @return time in ms tasks requires for clean up
	 */
	public long requestForMoreLifeTime(long requestNumber, long totalMoreTimeUntilNow,IQueueWorker worker);
}
