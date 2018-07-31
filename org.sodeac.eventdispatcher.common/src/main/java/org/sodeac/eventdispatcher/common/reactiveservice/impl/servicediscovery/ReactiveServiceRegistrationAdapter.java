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
package org.sodeac.eventdispatcher.common.reactiveservice.impl.servicediscovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.framework.Bundle;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.common.reactiveservice.api.DiscoverReactiveServiceRequest;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IContainerLocation;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceReference;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveService;
import org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceRegistrationAdapter;
import org.sodeac.eventdispatcher.common.reactiveservice.api.wiring.Capability;
import org.sodeac.eventdispatcher.common.reactiveservice.api.wiring.Requirement;
import org.sodeac.eventdispatcher.common.reactiveservice.impl.ContainerLocation;
import org.sodeac.xuri.URI;
import org.sodeac.xuri.ldapfilter.Attribute;
import org.sodeac.xuri.ldapfilter.AttributeLinker;
import org.sodeac.xuri.ldapfilter.ComparativeOperator;
import org.sodeac.xuri.ldapfilter.IFilterItem;
import org.sodeac.xuri.ldapfilter.IMatchable;
import org.sodeac.xuri.ldapfilter.LDAPFilterDecodingHandler;
import org.sodeac.xuri.ldapfilter.LDAPFilterEncodingHandler;
import org.sodeac.xuri.ldapfilter.LogicalOperator;

public class ReactiveServiceRegistrationAdapter implements IReactiveServiceRegistrationAdapter
{
	public ReactiveServiceRegistrationAdapter(IQueue queue)
	{
		super();
		this.serviceContainerLock = new ReentrantReadWriteLock();
		this.serviceContainerReadLock = this.serviceContainerLock.readLock();
		this.serviceContainerWriteLock = this.serviceContainerLock.writeLock();
		this.queue = queue;
	}
	
	private ReentrantReadWriteLock serviceContainerLock = null;
	private ReadLock serviceContainerReadLock = null;
	private WriteLock serviceContainerWriteLock = null;
	
	private IQueue queue = null;
	private List<ServiceContainer> serviceContainerList = new ArrayList<ServiceContainer>();
	private Map<UUID,ServiceContainer> serviceContainerIndex = new HashMap<UUID,ServiceContainer>();
	
	@Override
	public void register(IReactiveService service, Dictionary<String, ?> properties, Bundle bundle)
	{
		serviceContainerWriteLock.lock();
		try
		{
			for(ServiceContainer serviceContainer : serviceContainerList)
			{
				if(serviceContainer.service == service)
				{
					return;
				}
			}

			ServiceContainer serviceContainer = new ServiceContainer();
			serviceContainer.service = service;
			serviceContainer.properties = properties;
			serviceContainer.bundle = bundle;
			serviceContainer.uuid = UUID.randomUUID();
			
			this.serviceContainerList.add(serviceContainer);
			this.serviceContainerIndex.put(serviceContainer.uuid, serviceContainer);
			
		}
		finally 
		{
			serviceContainerWriteLock.unlock();
		}
	}

	@Override
	public void unregister(IReactiveService service, Dictionary<String, ?> properties, Bundle bundle)
	{
		serviceContainerWriteLock.lock();
		try
		{
			List<ServiceContainer> toRemoveList = new ArrayList<ServiceContainer>();
			for(ServiceContainer serviceContainer : serviceContainerList)
			{
				if(serviceContainer.service == service)
				{
					toRemoveList.add(serviceContainer);
				}
			}
			
			for(ServiceContainer toRemove : toRemoveList)
			{
				this.serviceContainerList.remove(toRemove);
				this.serviceContainerIndex.remove(toRemove.uuid);
			}
			
		}
		finally 
		{
			serviceContainerWriteLock.unlock();
		}
	}

	@Override
	public boolean isEmpty()
	{
		serviceContainerReadLock.lock();
		try
		{
			return this.serviceContainerList.isEmpty();
		}
		finally 
		{
			serviceContainerReadLock.unlock();
		}
	}
	
	@Override
	public void updateRegistrations()
	{
		IReactiveServiceRegistrationAdapter registration = queue.getConfigurationPropertyBlock().getAdapter(IReactiveServiceRegistrationAdapter.class);
		if(registration == null)
		{
			return;
		}
		if(registration != this)
		{
			return;
		}
		
		if(isEmpty())
		{
			queue.getConfigurationPropertyBlock().removeAdapter(IReactiveServiceRegistrationAdapter.class);
			return;
		}
		
		serviceContainerWriteLock.lock();
		try
		{
			for(ServiceContainer serviceContainer : serviceContainerList)
			{
				serviceContainer.update();
			}
		}
		finally 
		{
			serviceContainerWriteLock.unlock();
		}
	}
	
	private class ServiceContainer
	{
		private IReactiveService service;
		private UUID uuid;
		private Dictionary<String, ?> properties;
		private Bundle bundle;
		private ReactiveServiceReference serviceReference = null;
		
		private Map<String,NameSpace> nameSpaceIndex;
		
		private void update()
		{
			nameSpaceIndex = new HashMap<String,NameSpace>();
			List<Capability> capabilitiesForReference = new ArrayList<Capability>();
			for(Capability capability : service.getCapabilityList())
			{
				capabilitiesForReference.add(capability.getImmutable());
				NameSpace nameSpace = nameSpaceIndex.get(capability.getNameSpace());
				if(nameSpace == null)
				{
					nameSpace = new NameSpace();
					nameSpaceIndex.put(capability.getNameSpace(),nameSpace);
				}
				nameSpace.capabilityList.put(capability.getName(), capability);
				if(capability.isMandatory())
				{
					nameSpace.mandatoryList.put(capability.getName(), capability);
				}
				if(capability.getValue() instanceof IMatchable)
				{
					nameSpace.ldapFilterMatchables.put(capability.getName(), (IMatchable)capability.getValue());
				}
			}
			IContainerLocation location = queue.getDispatcher().getPropertyBlock().getAdapter(IContainerLocation.class);
			if(location == null)
			{
				location = new ContainerLocation();
			}
			
			URI serviceLocation = new URI
			(
				"sdc://" 
						+ location.getClusterId() + ":" 
						+ location.getDataCenterId() + ":" 
						+ location.getMachineId() + ":" 
						+ location.getContainerId() +  "/"
						+ "eventdispatcher" + LDAPFilterEncodingHandler.getInstance().encodeToString
							(
									new Attribute().setName("id").setOperator(ComparativeOperator.EQUAL).setValue(queue.getDispatcher().getId())
							) + "/"
						+ "reactiveservice" + LDAPFilterEncodingHandler.getInstance().encodeToString
						(
								new Attribute().setName("uuid").setOperator(ComparativeOperator.EQUAL).setValue(this.uuid.toString())
						)
			);
			serviceReference = new ReactiveServiceReference(this.uuid, serviceLocation, Collections.unmodifiableList(capabilitiesForReference) );
			
		}
	}
	
	private class NameSpace
	{
		private String nameSpaceName;
		private Map<String,Capability> capabilityList =  new HashMap<String,Capability>();
		private Map<String,Capability> mandatoryList =  new HashMap<String,Capability>();
		Map<String,IMatchable> ldapFilterMatchables = new HashMap<String,IMatchable>();
		
	}

	@Override
	public List<IReactiveServiceReference> discoverServices(DiscoverReactiveServiceRequest request)
	{
		List<IReactiveServiceReference> referenceList = null;
		
		serviceContainerReadLock.lock();
		try
		{
			if(this.serviceContainerList == null)
			{
				return null;
			}
			if(this.serviceContainerList.isEmpty())
			{
				return null;
			}
			
			Map<String,List<Requirement>> filterTypes = new HashMap<String,List<Requirement>>();
			
			if(request.getRequirementList() != null)
			{
				for(Requirement requirement : request.getRequirementList() )
				{
					List<Requirement> listByFilterType = filterTypes.get(requirement.getFilterType());
					if(listByFilterType == null)
					{
						listByFilterType = new ArrayList<Requirement>();
						filterTypes.put(requirement.getFilterType(), listByFilterType);
					}
					if(listByFilterType.contains(requirement))
					{
						continue;
					}
					listByFilterType.add(requirement);
				}
			}
			
			
			Map<String,Map<String,Capability>> matchedMandatoryPositivIndex = new HashMap<String,Map<String,Capability>>();
			LinkedList<IFilterItem> discoverLDAPItem = new LinkedList<IFilterItem>();
			
			serviceloop:
			for(ServiceContainer serviceContainer : this.serviceContainerList)
			{
				if(serviceContainer.nameSpaceIndex == null)
				{
					continue serviceloop;
				}
				
				IReactiveServiceReference serviceReference = serviceContainer.serviceReference;
				if(serviceReference == null)
				{
					continue serviceloop;
				}
				
				try
				{
					matchedMandatoryPositivIndex.clear();
					
					
					for(Entry<String,List<Requirement>> entry : filterTypes.entrySet())
					{		
						List<Requirement> listByFilterType = entry.getValue();
						if(entry.getKey().equals(Requirement.FILTER_TYPE_LDAP))
						{
							requirementloop:
							for(Requirement requirement : listByFilterType)
							{
								
								String requirementNameSpace = requirement.getNamespace();
								NameSpace capabilityNameSpace = serviceContainer.nameSpaceIndex.get(requirementNameSpace);
								if(capabilityNameSpace == null)
								{
									if(requirement.isOptional())
									{
										continue requirementloop;
									}
									else
									{
										continue serviceloop;
									}
								}
								
								IFilterItem filter = LDAPFilterDecodingHandler.getInstance().decodeFromString(requirement.getFilterExpression());
								if(filter.matches(capabilityNameSpace.ldapFilterMatchables))
								{
									discoverLDAPItem.clear();
									discoverLDAPItem.addLast(filter);
									
									while(! discoverLDAPItem.isEmpty())
									{
										filter = discoverLDAPItem.removeFirst();
										if(filter.isInvert())
										{
											continue;
										}
										if(filter instanceof Attribute) 
										{
											if(((Attribute)filter).getOperator() != ComparativeOperator.EQUAL)
											{
												continue;
											}
											String name = ((Attribute)filter).getName();
											Capability mandatoryCapability = capabilityNameSpace.mandatoryList.get(name);
											if(mandatoryCapability != null)
											{
												 if(mandatoryCapability.getValue().getExpression().equals(((Attribute)filter).getValue()))
												 {
													 Map<String,Capability> mandatoryByNS = matchedMandatoryPositivIndex.get(capabilityNameSpace.nameSpaceName);
													 if(mandatoryByNS == null)
													 {
														 mandatoryByNS = new HashMap<String,Capability>();
														 matchedMandatoryPositivIndex.put(capabilityNameSpace.nameSpaceName,mandatoryByNS);
													 }
													 mandatoryByNS.put(name,mandatoryCapability);
												 }
											}
										}
										else if(filter instanceof AttributeLinker)
										{
											if(((AttributeLinker)filter).getOperator() != LogicalOperator.AND)
											{
												continue;
											}
											discoverLDAPItem.addAll(((AttributeLinker)filter).getLinkedItemList());
										}
									}
								}
								else // Not Match
								{
									if(requirement.isOptional())
									{
										continue requirementloop;
									}
									else
									{
										continue serviceloop;
									}
								}
							}
							
						}
						else
						{
							throw new RuntimeException("DiscoveryService: filter type not supported: " + entry.getKey() ); // TODO ExceptionClass
						}
					} // End test requirementloop
					
					for(Entry<String,NameSpace> nameSpaceEntry : serviceContainer.nameSpaceIndex.entrySet())
					{
						if(nameSpaceEntry.getValue().mandatoryList.isEmpty())
						{
							continue;
						}
						
						Map<String,Capability> mandatoryByNS = matchedMandatoryPositivIndex.get(nameSpaceEntry.getValue().nameSpaceName);
						if(mandatoryByNS == null)
						{
							continue serviceloop;
						}
						
						for(String mandatory : nameSpaceEntry.getValue().mandatoryList.keySet())
						{
							if(mandatoryByNS.get(mandatory) == null)
							{
								continue serviceloop;
							}
						}
					}
					
					
					if(referenceList == null)
					{
						referenceList = new ArrayList<IReactiveServiceReference>();
					}
					referenceList.add(serviceReference);
				}
				catch (Exception e) 
				{
					e.printStackTrace();
					// TODO: handle exception
				}
			}
		}
		finally 
		{
			serviceContainerReadLock.unlock();
		}
		
		return referenceList == null ? null : Collections.unmodifiableList(referenceList);
	}
	
}
