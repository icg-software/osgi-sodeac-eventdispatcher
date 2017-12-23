package org.sodeac.eventdispatcher.extension.jmx;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.extension.api.IExtensibleEventDispatcher;

public class EventDispatcher implements EventDispatcherMBean
{
	protected EventDispatcherJavaManagementExtension eventDispatcherExtension;
	protected IExtensibleEventDispatcher eventDispatcher;
	protected ObjectName managemantObjectName;
	protected String bundleId;
	protected String bundleVersion;
	protected String id = "";
	
	private Map<IEventController,EventController> eventControllerIndex = null;
	private Map<String,ObjectName> eventControllerObjectNameIndex = null;
	
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
		this.eventControllerObjectNameIndex = new HashMap<String,ObjectName>();
		this.lock = new ReentrantReadWriteLock(true);
		this.readLock = this.lock.readLock();
		this.writeLock = this.lock.writeLock();
		
		this.managemantObjectName = new ObjectName(getObjectNamePrefix() + ",name=about");
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
				for(Entry<IEventController,EventController> entry : eventControllerIndex.entrySet())
				{
					try
					{
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(entry.getValue().controllerObjectName);
					}
					catch (Exception e) 
					{
						eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister event controller",e);
					}
				}
				eventControllerIndex.clear();
				
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(managemantObjectName);
				}
				catch (Exception e) 
				{
					eventDispatcherExtension.log(LogService.LOG_ERROR,"unregister event eventdispatcher",e);
				}
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
				ObjectName controllerObjectName = null;
				if(category == null)
				{
					controllerObjectName = new ObjectName(getObjectNamePrefix() + ",objecttype=controller,name=" + name);
					ObjectName inUse = this.eventControllerObjectNameIndex.get(controllerObjectName.getCanonicalName());
					counter = 2;
					while(inUse != null)
					{
						controllerObjectName = new ObjectName(getObjectNamePrefix() + ",objecttype=controller,name=" + name + "" + counter);
						inUse = this.eventControllerObjectNameIndex.get(controllerObjectName.getCanonicalName());
					}
					this.eventControllerObjectNameIndex.put(controllerObjectName.getCanonicalName(),controllerObjectName);
				}
				else
				{
					controllerObjectName = new ObjectName(getObjectNamePrefix() + ",objecttype=controller"+ ",category=" + category +",name=" + name);
					ObjectName inUse = this.eventControllerObjectNameIndex.get(controllerObjectName.getCanonicalName());
					counter = 2;
					while(inUse != null)
					{
						controllerObjectName = new ObjectName(getObjectNamePrefix() + ",objecttype=controller"+ ",category=" + category +",name=" + name + "" + counter);
						inUse = this.eventControllerObjectNameIndex.get(controllerObjectName.getCanonicalName());
					}
					this.eventControllerObjectNameIndex.put(controllerObjectName.getCanonicalName(),controllerObjectName);
				}
				EventController eventControllerBean = new EventController(eventController,properties,controllerObjectName);
				eventControllerIndex.put(eventController,eventControllerBean);
				mBeanServer.registerMBean(eventControllerBean, controllerObjectName);
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
				ManagementFactory.getPlatformMBeanServer().unregisterMBean(eventControllerBean.controllerObjectName);
				eventControllerIndex.remove(eventController);
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


}
