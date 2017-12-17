package org.sodeac.eventdispatcher.extension.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.extension.api.IEventDispatcherExtension;

@Component(service=IEventDispatcherExtension.class,immediate=true)
public class EventDispatcherJavaManagementExtension implements IEventDispatcherExtension
{

	private List<EventController> eventControllerList = new ArrayList<EventController>();
	
	@Override
	public void setConnected(boolean connected)
	{
		System.out.println("Connected: " + connected);
		
		if(! connected)
		{
			for(EventController eventControllerBean : eventControllerList)
			{
				try
				{
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(eventControllerBean.name);
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
			eventControllerList.clear();
		}
	}

	@Override
	public void registerEventController(IEventController eventController, Map<String, ?> properties)
	{
		try
		{
			MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = new ObjectName("test:type=EventController_"+ eventController.toString());
			EventController eventControllerBean = new EventController(eventController,properties,name);
			eventControllerList.add(eventControllerBean);
			mBeanServer.registerMBean(eventControllerBean, name);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		System.out.println("Register EventController: " + eventController);

	}

	@Override
	public void unregisterEventController(IEventController eventController)
	{
		System.out.println("Unregister EventController: " + eventController);
		
		EventController ec = null;
		for(EventController eventControllerBean : eventControllerList)
		{
			if(eventControllerBean.eventController == eventController)
			{
				ec = eventControllerBean;
				break;
			}
		}
		if(ec == null)
		{
			return;
		}
		try
		{
			ManagementFactory.getPlatformMBeanServer().unregisterMBean(ec.name);
			while(eventControllerList.remove(ec)) {}
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

}
