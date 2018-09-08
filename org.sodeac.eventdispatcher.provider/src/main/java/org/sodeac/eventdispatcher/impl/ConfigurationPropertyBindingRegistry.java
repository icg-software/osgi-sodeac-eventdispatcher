package org.sodeac.eventdispatcher.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sodeac.eventdispatcher.impl.ControllerContainer.ControllerFilterObjects;
import org.sodeac.eventdispatcher.impl.ServiceContainer.ServiceFilterObjects;

public class ConfigurationPropertyBindingRegistry
{
	public ConfigurationPropertyBindingRegistry()
	{
		super();
		this.controllerContainerIndex = new HashMap<String,Set<ControllerContainer>>();
		this.serviceContainerIndex = new HashMap<String,Set<ServiceContainer>>();
	}
	
	private Map<String,Set<ControllerContainer>> controllerContainerIndex = null;
	private Map<String,Set<ServiceContainer>> serviceContainerIndex = null;
	
	public void register(ControllerContainer controllerContainer)
	{
		if(controllerContainer == null)
		{
			return;
		}
		
		List<ControllerFilterObjects>  controllerFilterObjectsList = controllerContainer.getFilterObjectList();
		if(controllerFilterObjectsList == null)
		{
			return;
		}
		
		for(ControllerFilterObjects controllerFilterObjects : controllerFilterObjectsList)
		{
			if((controllerFilterObjects.attributes != null) && (! controllerFilterObjects.attributes.isEmpty()))
			{
				for(String attributeName : controllerFilterObjects.attributes)
				{
					Set<ControllerContainer> controllerContainerSet = controllerContainerIndex.get(attributeName);
					if(controllerContainerSet == null)
					{
						controllerContainerSet = new HashSet<ControllerContainer>();
						controllerContainerIndex.put(attributeName,controllerContainerSet);
					}
					controllerContainerSet.add(controllerContainer);
				}
			}
		}
	}
	
	public Set<ControllerContainer> getControllerContainer(String... attributes)
	{
		if(attributes == null)
		{
			return null;
		}
		
		if(attributes.length == 0)
		{
			return null;
		}
		
		Set<ControllerContainer> set = null;
		for(String attribute : attributes)
		{
			Set<ControllerContainer> controllerContainerSet = controllerContainerIndex.get(attribute);
			if(controllerContainerSet == null)
			{
				continue;
			}
			if(set == null)
			{
				set = new HashSet<ControllerContainer>();
			}
			set.addAll(controllerContainerSet);
		}
		return set;
	}
	
	public void unregister(ControllerContainer controllerContainer)
	{
		if(controllerContainer == null)
		{
			return;
		}
		
		List<ControllerFilterObjects>  controllerFilterObjectsList = controllerContainer.getFilterObjectList();
		if(controllerFilterObjectsList == null)
		{
			return;
		}
		
		LinkedList<String> removeList = null;
		
		for(Entry<String,Set<ControllerContainer>> controllerContainerSetEntry : controllerContainerIndex.entrySet())
		{
			if(controllerContainerSetEntry.getValue().remove(controllerContainer))
			{

				if(controllerContainerSetEntry.getValue().isEmpty())
				{
					if(removeList == null)
					{
						removeList = new LinkedList<String>();
					}
					removeList.add(controllerContainerSetEntry.getKey());
				}
			}
		}
		
		if(removeList != null)
		{
			for(String attribute : removeList)
			{
				controllerContainerIndex.remove(attribute);
			}
		}
	}
	
	public void register(ServiceContainer serviceContainer)
	{
		if(serviceContainer == null)
		{
			return;
		}
		
		List<ServiceFilterObjects>  controllerFilterObjectsList = serviceContainer.getFilterObjectList();
		if(controllerFilterObjectsList == null)
		{
			return;
		}
		
		for(ServiceFilterObjects controllerFilterObjects : controllerFilterObjectsList)
		{
			if((controllerFilterObjects.attributes != null) && (! controllerFilterObjects.attributes.isEmpty()))
			{
				for(String attributeName : controllerFilterObjects.attributes)
				{
					Set<ServiceContainer> serviceContainerSet = serviceContainerIndex.get(attributeName);
					if(serviceContainerSet == null)
					{
						serviceContainerSet = new HashSet<ServiceContainer>();
						serviceContainerIndex.put(attributeName,serviceContainerSet);
					}
					serviceContainerSet.add(serviceContainer);
				}
			}
		}
	}
	
	public Set<ServiceContainer> getServiceContainer(String... attributes)
	{
		if(attributes == null)
		{
			return null;
		}
		
		if(attributes.length == 0)
		{
			return null;
		}
		
		Set<ServiceContainer> set = null;
		for(String attribute : attributes)
		{
			Set<ServiceContainer> serviceContainerSet = serviceContainerIndex.get(attribute);
			if(serviceContainerSet == null)
			{
				continue;
			}
			if(set == null)
			{
				set = new HashSet<ServiceContainer>();
			}
			set.addAll(serviceContainerSet);
		}
		return set;
	}
	
	public void unregister(ServiceContainer serviceContainer)
	{
		if(serviceContainer == null)
		{
			return;
		}
		
		List<ServiceFilterObjects>  controllerFilterObjectsList = serviceContainer.getFilterObjectList();
		if(controllerFilterObjectsList == null)
		{
			return;
		}
		
		LinkedList<String> removeList = null;
		
		for(Entry<String,Set<ServiceContainer>> serviceContainerSetEntry : serviceContainerIndex.entrySet())
		{
			if(serviceContainerSetEntry.getValue().remove(serviceContainer))
			{

				if(serviceContainerSetEntry.getValue().isEmpty())
				{
					if(removeList == null)
					{
						removeList = new LinkedList<String>();
					}
					removeList.add(serviceContainerSetEntry.getKey());
				}
			}
		}
		
		if(removeList != null)
		{
			for(String attribute : removeList)
			{
				serviceContainerIndex.remove(attribute);
			}
		}
	}
	
	public void clear()
	{
		// TODO 
	}
}
