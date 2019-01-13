/*******************************************************************************
 * Copyright (c) 2017, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.api.MetricsRequirement;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.xuri.ldapfilter.Attribute;
import org.sodeac.xuri.ldapfilter.AttributeLinker;
import org.sodeac.xuri.ldapfilter.IFilterItem;
import org.sodeac.xuri.ldapfilter.LDAPFilterDecodingHandler;

public class ServiceContainer
{
	public ServiceContainer
	(
		EventDispatcherImpl dispatcher, 
		List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList, 
		List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList,
		List<QueueComponentConfiguration.QueueServiceConfiguration> serviceConfigurationList,
		List<QueueComponentConfiguration.ChainDispatcherRuleConfiguration> chainDispatcherRuleConfigurationList,
		List<QueueComponentConfiguration.RunTaskOnTriggerRuleConfiguration> runTaskOnQueuedInChainRuleConfigurationList
	)
	{
		super();
		this.dispatcher = dispatcher;
		this.boundByIdList = boundByIdList;
		this.boundedByQueueConfigurationList = boundedByQueueConfigurationList;
		this.chainDispatcherRuleConfigurationList = chainDispatcherRuleConfigurationList;
		this.runTaskOnQueuedInChainRuleConfigurationList = runTaskOnQueuedInChainRuleConfigurationList;
		if((serviceConfigurationList != null) && (! serviceConfigurationList.isEmpty()))
		{
			this.serviceConfiguration = serviceConfigurationList.get(0);
		}
		else
		{
			this.serviceConfiguration = new QueueComponentConfiguration.QueueServiceConfiguration(UUID.randomUUID().toString());
		}
		this.createFilterObjectList();
	}
	
	private EventDispatcherImpl dispatcher;
	private List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList = null;
	private List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList = null;
	private List<QueueComponentConfiguration.ChainDispatcherRuleConfiguration> chainDispatcherRuleConfigurationList = null;
	private List<QueueComponentConfiguration.RunTaskOnTriggerRuleConfiguration> runTaskOnQueuedInChainRuleConfigurationList = null;
	private QueueComponentConfiguration.QueueServiceConfiguration serviceConfiguration = null;
	
	private volatile Map<String, ?> properties = null;
	private volatile IQueueService queueService = null;
	
	private volatile boolean registered = false;
	
	private volatile List<ServiceFilterObjects> filterObjectList;
	private volatile Set<String> filterAttributes;
	
	private void createFilterObjectList()
	{
		List<ServiceFilterObjects> list = new ArrayList<ServiceFilterObjects>();
		if(this.boundedByQueueConfigurationList != null)
		{
			for(QueueComponentConfiguration.BoundedByQueueConfiguration boundedByQueueConfiguration : boundedByQueueConfigurationList)
			{
				if(boundedByQueueConfiguration.getLdapFilter() == null)
				{
					continue;
				}
				if(boundedByQueueConfiguration.getLdapFilter().isEmpty())
				{
					continue;
				}
				ServiceFilterObjects controllerFilterObjects = new ServiceFilterObjects();
				controllerFilterObjects.bound = boundedByQueueConfiguration;
				controllerFilterObjects.filterExpression = boundedByQueueConfiguration.getLdapFilter();
				
				try
				{
					controllerFilterObjects.filter = FrameworkUtil.createFilter(controllerFilterObjects.filterExpression);
					
					LinkedList<IFilterItem> discoverLDAPItem = new LinkedList<IFilterItem>();
					IFilterItem filter = LDAPFilterDecodingHandler.getInstance().decodeFromString(controllerFilterObjects.filterExpression);
					
					discoverLDAPItem.addLast(filter);
					
					while(! discoverLDAPItem.isEmpty())
					{
						filter = discoverLDAPItem.removeFirst();
						
						if(filter instanceof Attribute) 
						{
							controllerFilterObjects.attributes.add(((Attribute)filter).getName());
						}
						else if(filter instanceof AttributeLinker)
						{
							discoverLDAPItem.addAll(((AttributeLinker)filter).getLinkedItemList());
						}
					}
					
					list.add(controllerFilterObjects);
				}
				catch (Exception e) 
				{
					dispatcher.log(LogService.LOG_ERROR,"parse bounded queue configuration " + boundedByQueueConfiguration.getLdapFilter(),e);
				}
			}
		}
		this.filterObjectList = list;
		this.filterAttributes = new HashSet<String>();
		for(ServiceFilterObjects controllerFilterObjects : this.filterObjectList)
		{
			if(controllerFilterObjects.attributes != null)
			{
				for(String attribute : controllerFilterObjects.attributes)
				{
					this.filterAttributes.add(attribute);
				}
			}
		}
	}
	
	
	public Map<String, ?> getProperties()
	{
		return properties;
	}
	public void setProperties(Map<String, ?> properties)
	{
		this.properties = properties;
	}
	public IQueueService getQueueService()
	{
		return queueService;
	}
	public void setQueueService(IQueueService queueService)
	{
		this.queueService = queueService;
	}
	public boolean isRegistered()
	{
		return registered;
	}
	public void setRegistered(boolean registered)
	{
		this.registered = registered;
	}
	
	public String getNonEmptyStringProperty(String key, String defaultValue)
	{
		String stringValue = defaultValue;
		Object current = this.properties.get(key);
		if(current != null)
		{
			if(! (current instanceof String))
			{
				current = current.toString();
			}
		}
		if((current != null) && (! ((String)current).isEmpty()))
		{
			stringValue = (String)current;
		}
		else
		{
			stringValue = defaultValue;
		}
		return stringValue;
	}
	
	public void clean()
	{
		this.dispatcher = null;
		this.properties = null;
		this.queueService = null;
		this.boundByIdList = null;
		this.boundedByQueueConfigurationList = null;
		this.serviceConfiguration = null;
		this.filterObjectList = null;
		this.filterAttributes = null;
	}
	
	public boolean isDisableQueueMetrics()
	{
		int countPreferMetrics = 0;
		int countPreferNoMetrics = 0;
		if(this.boundByIdList != null)
		{
			for(QueueComponentConfiguration.BoundedByQueueId boundedById : this.boundByIdList)
			{
				if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.RequireMetrics)
				{
					return false;
				}
				if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.PreferMetrics)
				{
					countPreferMetrics++;
				}
				else if(boundedById.getQueueMetricsRequirement() == MetricsRequirement.PreferNoMetrics)
				{
					countPreferNoMetrics++;
				}
			}
		}
		if(this.boundedByQueueConfigurationList != null)
		{
			for(QueueComponentConfiguration.BoundedByQueueConfiguration boundedByConfiguration : this.boundedByQueueConfigurationList)
			{
				if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.RequireMetrics)
				{
					return false;
				}
				if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.PreferMetrics)
				{
					countPreferMetrics++;
				}
				else if(boundedByConfiguration.getQueueMetricsRequirement() == MetricsRequirement.PreferNoMetrics)
				{
					countPreferNoMetrics++;
				}
			}
		}
		return countPreferNoMetrics > countPreferMetrics;
	}
	
	public boolean isDisableTaskMetrics()
	{
		return serviceConfiguration.getTaskMetricsRequirement() == MetricsRequirement.PreferNoMetrics;
	}
	
	public List<ServiceFilterObjects> getFilterObjectList()
	{
		return filterObjectList;
	}

	public List<QueueComponentConfiguration.BoundedByQueueId> getBoundByIdList()
	{
		return boundByIdList;
	}


	public List<QueueComponentConfiguration.BoundedByQueueConfiguration> getBoundedByQueueConfigurationList()
	{
		return boundedByQueueConfigurationList;
	}


	public QueueComponentConfiguration.QueueServiceConfiguration getServiceConfiguration()
	{
		return serviceConfiguration;
	}

	public Set<String> getFilterAttributeSet()
	{
		return filterAttributes;
	}
	
	public List<QueueComponentConfiguration.ChainDispatcherRuleConfiguration> getChainDispatcherRuleConfigurationList()
	{
		return chainDispatcherRuleConfigurationList;
	}


	public List<QueueComponentConfiguration.RunTaskOnTriggerRuleConfiguration> getRunTaskOnQueuedInChainRuleConfigurationList()
	{
		return runTaskOnQueuedInChainRuleConfigurationList;
	}



	public class ServiceFilterObjects
	{
		QueueComponentConfiguration.BoundedByQueueConfiguration bound = null;
		String filterExpression = null;
		Filter filter = null;
		Set<String> attributes = new HashSet<String>();
	}
}
