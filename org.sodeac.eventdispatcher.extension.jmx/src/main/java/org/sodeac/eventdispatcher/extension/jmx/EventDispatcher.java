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

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleEventDispatcher;
import org.sodeac.eventdispatcher.extension.api.IExtensibleGauge;
import org.sodeac.eventdispatcher.extension.api.IExtensibleHistogram;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMeter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMetrics;
import org.sodeac.eventdispatcher.extension.api.IExtensibleQueue;
import org.sodeac.eventdispatcher.extension.api.IExtensibleTimer;

public class EventDispatcher implements EventDispatcherMBean
{
	protected EventDispatcherJavaManagementExtension eventDispatcherExtension;
	protected IExtensibleEventDispatcher eventDispatcher;
	protected ObjectName managemantObjectName;
	protected String bundleId;
	protected String bundleVersion;
	protected String id = "";
	
	private Map<IEventController,EventController> eventControllerIndex = null;
	private Map<String,String> eventControllerObjectNameIndex = null;
	private Map<IExtensibleQueue,EventQueue> eventQueueIndex = null;
	private Map<String,String> eventQueueObjectNameIndex = null;
	
	private MetricContainer metricContainer = null;
	
	private ReentrantReadWriteLock lock = null;
	private ReadLock readLock = null;
	private WriteLock writeLock = null;
	
	
	public EventDispatcher(EventDispatcherJavaManagementExtension eventDispatcherExtension,IExtensibleEventDispatcher eventDispatcher) throws MalformedObjectNameException
	{
		super();
		this.eventDispatcher = eventDispatcher;
		this.id = eventDispatcher.getId();
		this.bundleId = eventDispatcher.getBundleId();
		this.bundleVersion = eventDispatcher.getBundleVersion();
		this.eventDispatcherExtension = eventDispatcherExtension;
		
		this.eventControllerIndex = new HashMap<IEventController,EventController>();
		this.eventControllerObjectNameIndex = new HashMap<String,String>();
		this.eventQueueIndex = new HashMap<IExtensibleQueue,EventQueue>();
		this.eventQueueObjectNameIndex = new HashMap<String,String>();
		this.lock = new ReentrantReadWriteLock(true);
		this.readLock = this.lock.readLock();
		this.writeLock = this.lock.writeLock();
		
		String objectNamePrefix = getObjectNamePrefix();
		this.metricContainer = new MetricContainer(objectNamePrefix + ",dispatchermetrics=metrics",eventDispatcherExtension);
		this.managemantObjectName = new ObjectName(objectNamePrefix + ",name=about");
	}
	
	public String getObjectNamePrefix()
	{
		return "org.sodeac:sodeacproject=eventdispatcher,dispatcherinstance=" + eventDispatcher.getId() ;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getBundleId()
	{
		return bundleId;
	}

	public void setBundleId(String bundleId)
	{
		this.bundleId = bundleId;
	}

	public String getBundleVersion()
	{
		return bundleVersion;
	}

	public void setBundleVersion(String bundleVersion)
	{
		this.bundleVersion = bundleVersion;
	}

	@Override
	public String printInfo()
	{
		return this.eventDispatcher.toString();
	}
	
	public void setConnected(boolean connected)
	{
		writeLock.lock();
		try
		{
			if(! connected)
			{
				for(Entry<IExtensibleQueue,EventQueue> entry : eventQueueIndex.entrySet())
				{
					try
					{
						entry.getValue().dispose();
					}
					catch(Exception e)
					{
						eventDispatcherExtension.log(LogService.LOG_ERROR,"dispose event queue",e);
					}
					
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().queueObjectName);
					}
					catch (InstanceNotFoundException e) {}
					catch (Exception e) 
					{
						eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister event qeueue",e);
					}
				}
				eventQueueIndex.clear();
				
				
				for(Entry<IEventController,EventController> entry : eventControllerIndex.entrySet())
				{
					try
					{
						entry.getValue().dispose();
					}
					catch (Exception e) 
					{
						eventDispatcherExtension.log(LogService.LOG_ERROR,"dispose controller",e);
					}
					
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().controllerObjectName);
					}
					catch (InstanceNotFoundException e) {}
					catch (Exception e) 
					{
						e.printStackTrace();
						eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister event controller",e);
					}
				}
				eventControllerIndex.clear();
				
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(managemantObjectName);
				}
				catch (InstanceNotFoundException e) {}
				catch (Exception e) 
				{
					eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister event eventdispatcher",e);
				}
				
				this.metricContainer.dispose();
			}
			else
			{
				try
				{
					ManagementFactory.getPlatformMBeanServer().registerMBean(this, managemantObjectName);
				}
				catch (Exception e) 
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(managemantObjectName);
						ManagementFactory.getPlatformMBeanServer().registerMBean(this, managemantObjectName);
					}
					catch (Exception e2) 
					{
						eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister event eventdispatcher",e2);
						eventDispatcherExtension.log(LogService.LOG_ERROR,"register event eventdispatcher",e);
					}
					
				}
			}
		}
		finally 
		{
			writeLock.unlock();
		}
	}

	public void registerEventController(IEventController eventController, Map<String, ?> properties)
	{
		writeLock.lock();
		try
		{
			try
			{
				MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				
				String name  = eventController.getClass().getSimpleName();
				if((properties.get(IEventController.PROPERTY_JMX_NAME) != null) && (! properties.get(IEventController.PROPERTY_JMX_NAME).toString().isEmpty()))
				{
					name = properties.get(IEventController.PROPERTY_JMX_NAME).toString();
				}
				
				String category = null;
				if((properties.get(IEventController.PROPERTY_JMX_CATEGORY) != null) && (! properties.get(IEventController.PROPERTY_JMX_CATEGORY).toString().isEmpty()))
				{
					category = properties.get(IEventController.PROPERTY_JMX_CATEGORY).toString();
				}
	
				int counter = 2;
				String objectNamePrefix = null;
				ObjectName controllerObjectName = null;
				if((category == null) || category.isEmpty())
				{
					category = "default";
				}
				
				objectNamePrefix = getObjectNamePrefix() + ",objecttype=controller"+ ",category=" + category +",name=" + name;
				String inUse = this.eventControllerObjectNameIndex.get(objectNamePrefix);
				counter = 2;
				while(inUse != null)
				{
					objectNamePrefix = getObjectNamePrefix() + ",objecttype=controller"+ ",category=" + category +",name=" + name + "" + counter;
					inUse = this.eventControllerObjectNameIndex.get(objectNamePrefix);
				}
				this.eventControllerObjectNameIndex.put(objectNamePrefix,objectNamePrefix);
				
				controllerObjectName = new ObjectName(objectNamePrefix);
				EventController eventControllerBean = new EventController(eventController,properties,controllerObjectName,objectNamePrefix);
				eventControllerIndex.put(eventController,eventControllerBean);
				try
				{
					mBeanServer.registerMBean(eventControllerBean, controllerObjectName);
				}
				catch(InstanceAlreadyExistsException e)
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(controllerObjectName);
					}
					catch (Exception ie) {}
					try
					{
						mBeanServer.registerMBean(eventControllerBean, controllerObjectName);
					}
					catch (Exception ie) 
					{
						this.eventDispatcherExtension.log(LogService.LOG_ERROR,"re-register eventcontroller",e);
					}
				}
				catch (Exception e) 
				{
					this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register eventcontroller",e);
				}
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register eventcontroller",e);
			}
		}
		finally 
		{
			writeLock.unlock();
		}
	}
	
	public void registerEventQueue(IExtensibleQueue extensibleQueue)
	{
		writeLock.lock();
		try
		{
			try
			{
				MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				
				String name = extensibleQueue.getId();
				String category = extensibleQueue.getCategory();
				
				int counter = 2;
				String objectNamePrefix = null;
				
				if(category == null)
				{
					category = "default";
				}
				
				objectNamePrefix = getObjectNamePrefix() + ",objecttype=queue,category=" + category +",queueid=" + name;
				String inUse = this.eventQueueObjectNameIndex.get(objectNamePrefix);
				counter = 2;
				while(inUse != null)
				{
					objectNamePrefix = getObjectNamePrefix() + ",objecttype=queue,category=" + category +",queueid=" + name + "" + counter;
					inUse = this.eventQueueObjectNameIndex.get(objectNamePrefix);
				}
				this.eventQueueObjectNameIndex.put(objectNamePrefix,objectNamePrefix);
				
				ObjectName queueObjectName = new ObjectName(objectNamePrefix + ",name=about");
				EventQueue eventQueueBean = new EventQueue(this.eventDispatcherExtension,extensibleQueue,queueObjectName,objectNamePrefix);
				eventQueueIndex.put(extensibleQueue,eventQueueBean);
				mBeanServer.registerMBean(eventQueueBean, queueObjectName);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register eventqueue",e);
			}
		}
		finally 
		{
			writeLock.unlock();
		}
	}

	public void unregisterEventController(IEventController eventController)
	{
		writeLock.lock();
		try
		{
			try
			{
				EventController eventControllerBean = eventControllerIndex.get(eventController);
				if(eventControllerBean == null)
				{
					return;
				}
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(eventControllerBean.controllerObjectName);
				}
				catch(InstanceNotFoundException e) {}
				eventControllerIndex.remove(eventController);
				eventControllerObjectNameIndex.remove(eventControllerBean.objectNamePrefix);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister eventcontroller",e);
			}
		}
		finally 
		{
			writeLock.unlock();
		}
	}
	
	public void unregisterEventQueue(IExtensibleQueue eventQueue)
	{
		writeLock.lock();
		try
		{
			try
			{
				EventQueue eventQueueBean = eventQueueIndex.get(eventQueue);
				if(eventQueueBean == null)
				{
					return;
				}
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(eventQueueBean.queueObjectName);
				}
				catch(InstanceNotFoundException e) {}
				eventQueueIndex.remove(eventQueue);
				eventQueueObjectNameIndex.remove(eventQueueBean.objectNamePrefix);
				eventQueueBean.dispose();
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister eventqueue",e);
			}
		}
		finally 
		{
			writeLock.unlock();
		}
	}

	public void registerEventDispatcher()
	{
		try
		{
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, managemantObjectName);
		}
		catch (Exception e) 
		{
			try
			{
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(managemantObjectName);
				ManagementFactory.getPlatformMBeanServer().registerMBean(this, managemantObjectName);
			}
			catch (Exception e2) 
			{
				e.printStackTrace();
				e2.printStackTrace();
			}
			
		}
	}
	
	public void registerCounter(IExtensibleCounter counter)
	{
		IExtensibleMetrics metrics = counter.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.registerCounter(counter);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register counter",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.registerCounter(counter);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register counter",e);
		}
	}

	public void unregisterCounter(IExtensibleCounter counter)
	{
		IExtensibleMetrics metrics = counter.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.unregisterCounter(counter);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister counter",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.unregisterCounter(counter);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister counter",e);
		}
	}
	
	public void registerMeter(IExtensibleMeter meter)
	{
		IExtensibleMetrics metrics = meter.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.registerMeter(meter);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register meter",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.registerMeter(meter);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register meter",e);
		}
	}

	public void unregisterMeter(IExtensibleMeter meter)
	{
		IExtensibleMetrics metrics = meter.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.unregisterMeter(meter);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister meter",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.unregisterMeter(meter);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister meter",e);
		}
	}
	
	
	public void registerHistogram(IExtensibleHistogram histogram)
	{
		IExtensibleMetrics metrics = histogram.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.registerHistogram(histogram);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register histogram",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.registerHistogram(histogram);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register histogram",e);
		}
	}

	public void unregisterHistogram(IExtensibleHistogram histogram)
	{
		IExtensibleMetrics metrics = histogram.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.unregisterHistogram(histogram);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister histogram",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.unregisterHistogram(histogram);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister histogram",e);
		}
	}

	public void registerTimer(IExtensibleTimer timer)
	{
		IExtensibleMetrics metrics = timer.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.registerTimer(timer);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register timer",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.registerTimer(timer);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register timer",e);
		}
	}

	public void unregisterTimer(IExtensibleTimer timer)
	{
		IExtensibleMetrics metrics = timer.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.unregisterTimer(timer);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister timer",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.unregisterTimer(timer);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister timer",e);
		}
	}
	
	public void registerGauge(IExtensibleGauge<?> gauge)
	{
		IExtensibleMetrics metrics = gauge.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.registerGauge(gauge);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register gauge",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.registerGauge(gauge);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"register gauge",e);
		}
	}

	public void unregisterGauge(IExtensibleGauge<?> gauge)
	{
		IExtensibleMetrics metrics = gauge.getMetrics();
		IExtensibleQueue extensibleQueue = metrics.getQueue();
		if(metrics.getJobId() != null)
		{
			return;
		}
		
		if(extensibleQueue == null)
		{
			try
			{
				this.metricContainer.unregisterGauge(gauge);
			}
			catch (Exception e) 
			{
				this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister gauge",e);
			}
			return;
		}
		
		EventQueue eventQueueBean = null;
		readLock.lock();
		try
		{
			eventQueueBean = eventQueueIndex.get(extensibleQueue);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(eventQueueBean == null)
		{
			return;
		}
		
		try
		{
			eventQueueBean.unregisterGauge(gauge);
		}
		catch (Exception e) 
		{
			this.eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister gauge",e);
		}
	}
}
