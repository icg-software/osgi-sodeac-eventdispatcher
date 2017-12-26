package org.sodeac.eventdispatcher.extension.jmx;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.ObjectName;

import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;
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
}
