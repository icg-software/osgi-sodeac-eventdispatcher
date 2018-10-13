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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleGauge;
import org.sodeac.eventdispatcher.extension.api.IExtensibleHistogram;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMeter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleQueue;
import org.sodeac.eventdispatcher.extension.api.IExtensibleTimer;

public class EventQueue implements EventQueueMBean
{
	protected EventDispatcherJavaManagementExtension eventDispatcherExtension;
	private IExtensibleQueue queue;
	protected ObjectName queueObjectName;
	protected String objectNamePrefix;
	
	private MetricContainer metricContainer = null;
	
	private ReentrantReadWriteLock lock = null;
	private ReadLock readLock = null;
	private WriteLock writeLock = null;
	
	public EventQueue(EventDispatcherJavaManagementExtension eventDispatcherExtension,IExtensibleQueue queue,ObjectName name, String objectNamePrefix)
	{
		super();
		this.queue = queue;
		this.queueObjectName = name;
		this.eventDispatcherExtension  = eventDispatcherExtension;
		this.objectNamePrefix = objectNamePrefix;
		
		this.lock = new ReentrantReadWriteLock(true);
		this.readLock = this.lock.readLock();
		this.writeLock = this.lock.writeLock();
		
		this.metricContainer = new MetricContainer(objectNamePrefix,eventDispatcherExtension);
	}
	
	public void dispose()
	{
		this.metricContainer.dispose();
	}

	public String getName()
	{
		return this.queue.getName();
	}
	
	public String getCategory()
	{
		return this.queue.getCategory();
	}
	
	public void registerCounter(IExtensibleCounter counter)
	{
		this.metricContainer.registerCounter(counter);
	}
	
	public void unregisterCounter(IExtensibleCounter counter)
	{
		this.metricContainer.unregisterCounter(counter);
	}
	
	public void registerMeter(IExtensibleMeter meter)
	{
		this.metricContainer.registerMeter(meter);
	}
	
	public void unregisterMeter(IExtensibleMeter meter)
	{
		this.metricContainer.unregisterMeter(meter);
	}
	
	public void registerHistogram(IExtensibleHistogram histogram)
	{
		this.metricContainer.registerHistogram(histogram);
	}
	
	public void unregisterHistogram(IExtensibleHistogram histogram)
	{
		this.metricContainer.unregisterHistogram(histogram);
	}
	
	public void registerTimer(IExtensibleTimer timer)
	{
		this.metricContainer.registerTimer(timer);
	}
	
	public void unregisterTimer(IExtensibleTimer timer)
	{
		this.metricContainer.unregisterTimer(timer);
	}
	
	public void registerGauge(IExtensibleGauge<?> gauge)
	{
		this.metricContainer.registerGauge(gauge);
	}
	
	public void unregisterGauge(IExtensibleGauge<?> gauge)
	{
		this.metricContainer.unregisterGauge(gauge);
	}

	@Override
	public String showStateInfo()
	{
		StringBuilder info = new StringBuilder();
		
		IPropertyBlock properties = this.queue.getConfigurationPropertyBlock();
		info.append("Properties:\n");
		{
			for(String key : properties.getPropertyKeys())
			{
				info.append("\t" + key + " :: " + properties.getProperty(key) + "\n");
			}
		}
		info.append("\n");
		
		List<IQueuedEvent> eventList = this.queue.getEventList(null, null, null);
		info.append("Scheduled Events (" + eventList.size() + "):\n");
		for(IQueuedEvent event : eventList)
		{
			info.append("\t" + event.getUUID() +" :: " + event.getEvent().getTopic() + "\n" );
		}
		info.append("\n");
		
		Map<String,IQueueTask> taskIndex = this.queue.getTaskIndex(null);
		info.append("Scheduled tasks (" + taskIndex.size() + "):\n");
		for(Entry<String,IQueueTask> entry: taskIndex.entrySet())
		{
			info.append("\t" + entry.getKey() +" :: " + entry.getValue() + "\n" );
		}
		info.append("\n");
		return info.toString();
	}

	@Override
	public String showDescription()
	{
		return new String();
	}
}
