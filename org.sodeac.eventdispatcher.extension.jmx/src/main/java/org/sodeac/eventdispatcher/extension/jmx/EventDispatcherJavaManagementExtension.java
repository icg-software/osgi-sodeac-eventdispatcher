package org.sodeac.eventdispatcher.extension.jmx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;
import org.sodeac.eventdispatcher.extension.api.IExtensibleCounter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleEventDispatcher;
import org.sodeac.eventdispatcher.extension.api.IExtensibleGauge;
import org.sodeac.eventdispatcher.extension.api.IExtensibleHistogram;
import org.sodeac.eventdispatcher.extension.api.IExtensibleMeter;
import org.sodeac.eventdispatcher.extension.api.IExtensibleQueue;
import org.sodeac.eventdispatcher.extension.api.IExtensibleTimer;

@Component(service=IEventDispatcherExtension.class,immediate=true)
public class EventDispatcherJavaManagementExtension implements IEventDispatcherExtension
{
	public EventDispatcherJavaManagementExtension()
	{
		super();
		
		this.eventDispatcherIndex = new HashMap<IExtensibleEventDispatcher,EventDispatcher>(); 
		this.lock = new ReentrantReadWriteLock(true);
		this.readLock = this.lock.readLock();
		this.writeLock = this.lock.writeLock();
	}
	
	private volatile ComponentContext context = null;
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile LogService logService = null;
	
	private Map<IExtensibleEventDispatcher,EventDispatcher> eventDispatcherIndex = null;
	
	private ReentrantReadWriteLock lock = null;
	private ReadLock readLock = null;
	private WriteLock writeLock = null;
	
	@Activate
	private void activate(ComponentContext context, Map<String, ?> properties)
	{
		this.context = context;
	}
	
	@Deactivate
	private void deactivate(ComponentContext context)
	{
		for(Entry<IExtensibleEventDispatcher,EventDispatcher> entry : this.eventDispatcherIndex.entrySet())
		{
			unregisterEventDispatcher(entry.getKey());
		}
		this.context = null;
	}
	
	

	@Override
	public void registerEventDispatcher(IExtensibleEventDispatcher dispatcher)
	{
		writeLock.lock();
		try
		{
			try
			{
				EventDispatcher found = this.eventDispatcherIndex.get(dispatcher);
				if(found != null)
				{
					found.setConnected(false);
					this.eventDispatcherIndex.remove(dispatcher);
				}
			
				EventDispatcher eventDispatcherBean = new EventDispatcher(this,dispatcher);
				this.eventDispatcherIndex.put(dispatcher,eventDispatcherBean);
				eventDispatcherBean.setConnected(true);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"register event dispatcher",e);
			}
		}
		finally 
		{
			writeLock.unlock();
		}
	}

	@Override
	public void unregisterEventDispatcher(IExtensibleEventDispatcher dispatcher)
	{
		writeLock.lock();
		try
		{
			try
			{
				EventDispatcher found = this.eventDispatcherIndex.get(dispatcher);
				if(found != null)
				{
					found.setConnected(false);
					this.eventDispatcherIndex.remove(dispatcher);
				}
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"unregister event dispatcher",e);
			}
		}
		finally 
		{
			writeLock.unlock();
		}
	}

	@Override
	public void registerEventController(IExtensibleEventDispatcher dispatcher, IEventController eventController,Map<String, ?> properties)
	{
		readLock.lock();
		try
		{
			try
			{
				EventDispatcher found = this.eventDispatcherIndex.get(dispatcher);
				if(found == null)
				{
					return;
				}
				
				found.registerEventController(eventController, properties);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"register event controller",e);
			}
		}
		finally 
		{
			readLock.unlock();
		}
	}

	@Override
	public void unregisterEventController(IExtensibleEventDispatcher dispatcher, IEventController eventController)
	{
		readLock.lock();
		try
		{
			try
			{
				EventDispatcher found = this.eventDispatcherIndex.get(dispatcher);
				if(found == null)
				{
					return;
				}
				
				found.unregisterEventController(eventController);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"register event controller",e);
			}
			
		}
		finally 
		{
			readLock.unlock();
		}
		
	}
	
	protected void log(int logServiceLevel,String logMessage, Throwable e)
	{
		
		try
		{
			LogService logService = this.logService;
			ComponentContext context = this.context;
			
			if(logService != null)
			{
				logService.log(context == null ? null : context.getServiceReference(), logServiceLevel, logMessage, e);
			}
			else
			{
				if(logServiceLevel == LogService.LOG_ERROR)
				{
					System.err.println(logMessage);
				}
				if(e != null)
				{
					e.printStackTrace();
				}
			}
		}
		catch (Exception ie) 
		{
			ie.printStackTrace();
		}
	}

	@Override
	public void registerEventQueue(IExtensibleEventDispatcher dispatcher, IExtensibleQueue extensibleQueue)
	{
		readLock.lock();
		try
		{
			try
			{
				EventDispatcher found = this.eventDispatcherIndex.get(dispatcher);
				if(found == null)
				{
					return;
				}
				
				found.registerEventQueue(extensibleQueue);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"register event queue",e);
			}
		}
		finally 
		{
			readLock.unlock();
		}
	}

	@Override
	public void unregisterEventQueue(IExtensibleEventDispatcher dispatcher, IExtensibleQueue extensibleQueue)
	{
		readLock.lock();
		try
		{
			try
			{
				EventDispatcher found = this.eventDispatcherIndex.get(dispatcher);
				if(found == null)
				{
					return;
				}
				
				found.unregisterEventQueue(extensibleQueue);
			}
			catch (Exception e) 
			{
				log(LogService.LOG_ERROR,"register event queue",e);
			}
			
		}
		finally 
		{
			readLock.unlock();
		}
	}

	@Override
	public void registerCounter(IExtensibleEventDispatcher dispatcher, IExtensibleCounter counter)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.registerCounter(counter);
	}
	
	
	public void updateCounter(IExtensibleEventDispatcher dispatcher, IExtensibleCounter counter)
	{
	}
	
	public void unregisterCounter(IExtensibleEventDispatcher dispatcher, IExtensibleCounter counter)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.unregisterCounter(counter);
	}
	
	@Override
	public void registerMeter(IExtensibleEventDispatcher dispatcher, IExtensibleMeter meter)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.registerMeter(meter);
	}
	
	
	public void updateMeter(IExtensibleEventDispatcher dispatcher, IExtensibleMeter meter)
	{
	}
	
	public void unregisterMeter(IExtensibleEventDispatcher dispatcher, IExtensibleMeter meter)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.unregisterMeter(meter);
	}
	
	@Override
	public void registerHistogram(IExtensibleEventDispatcher dispatcher, IExtensibleHistogram histogram)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.registerHistogram(histogram);
	}
	
	
	public void updateHistogram(IExtensibleEventDispatcher dispatcher, IExtensibleHistogram histogram)
	{
	}
	
	public void unregisterHistogram(IExtensibleEventDispatcher dispatcher, IExtensibleHistogram histogram)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.unregisterHistogram(histogram);
	}

	@Override
	public void registerTimer(IExtensibleEventDispatcher dispatcher, IExtensibleTimer timer)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.registerTimer(timer);
	}
	
	
	public void updateTimer(IExtensibleEventDispatcher dispatcher, IExtensibleTimer timer)
	{
	}
	
	public void unregisterTimer(IExtensibleEventDispatcher dispatcher, IExtensibleTimer timer)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.unregisterTimer(timer);
	}

	@Override
	public void registerGauge(IExtensibleEventDispatcher dispatcher, IExtensibleGauge<?> gauge)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.registerGauge(gauge);
	}
	
	public void unregisterGauge(IExtensibleEventDispatcher dispatcher, IExtensibleGauge<?> gauge)
	{
		EventDispatcher found = null;
		readLock.lock();
		try
		{
			found = this.eventDispatcherIndex.get(dispatcher);
		}
		finally 
		{
			readLock.unlock();
		}
		
		if(found == null)
		{
			return;
		}
		
		found.unregisterGauge(gauge);
	}

}
