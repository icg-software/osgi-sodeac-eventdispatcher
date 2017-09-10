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

import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;

public interface IQueue
{
	public IPropertyBlock getPropertyBlock();
	public IMetrics getMetrics();
	public IEventDispatcher getDispatcher();
	
	public IQueuedEvent getEvent(String uuid);
	public List<IQueuedEvent> getEventList(String[] topics, Filter queuedEventFilter, Filter nativeEventFilter);
	public boolean removeEvent(String uuid);
	public boolean removeEventList(List<String> uuidList);
	
	public List<IQueueJob> getJobList(Filter filter);
	public Map<String,IQueueJob> getJobIndex(Filter filter);
	
	public String scheduleJob(IQueueJob job);
	public String scheduleJob(String id, IQueueJob job, IPropertyBlock propertyBlock, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut );
	public IQueueJob rescheduleJob(String id, long executionTimeStamp, long timeOutValue, long heartBeatTimeOut );
	public IQueueJob getJob(String id);
	public IQueueJob removeJob(String id);
	public IPropertyBlock getJobPropertyBlock(String id);
	
	public void signal(String signal);
	
	public void sendEvent(String topic, Map<String, ?> properties);
	public void postEvent(String topic, Map<String, ?> properties);
	
	
}
