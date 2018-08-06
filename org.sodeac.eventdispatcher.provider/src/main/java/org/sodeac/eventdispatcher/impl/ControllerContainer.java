/*******************************************************************************
 * Copyright (c) 2017, 2018 Sebastian Palarus
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

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.MetricsRequirement;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.xuri.ldapfilter.Attribute;
import org.sodeac.xuri.ldapfilter.AttributeLinker;
import org.sodeac.xuri.ldapfilter.IFilterItem;
import org.sodeac.xuri.ldapfilter.LDAPFilterDecodingHandler;
public class ControllerContainer
{
	public ControllerContainer(EventDispatcherImpl dispatcher,List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList, List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList, List<QueueComponentConfiguration.SubscribeEvent> subscribeEventList)
	{
		super();
		this.boundedByQueueConfigurationList = boundedByQueueConfigurationList;
		this.boundByIdList = boundByIdList;
		this.subscribeEventList = subscribeEventList;
		this.dispatcher = dispatcher;
		this.createFilterObjectList();
	}
	
	private EventDispatcherImpl dispatcher = null;
	private volatile Map<String, ?> properties = null;
	private volatile IQueueController queueController = null;
	private List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList = null;
	private List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList = null;
	private List<QueueComponentConfiguration.SubscribeEvent> subscribeEventList = null;
	
	private volatile boolean registered = false;
	
	private volatile List<ControllerFilterObjects> filterObjectList;
	private volatile Set<String> filterAttributes;
	
	private void createFilterObjectList()
	{
		List<ControllerFilterObjects> list = new ArrayList<ControllerFilterObjects>();
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
				ControllerFilterObjects controllerFilterObjects = new ControllerFilterObjects();
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
		for(ControllerFilterObjects controllerFilterObjects : this.filterObjectList)
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
	public IQueueController getQueueController()
	{
		return queueController;
	}
	public void setQueueController(IQueueController queueController)
	{
		this.queueController = queueController;
	}
	public boolean isRegistered()
	{
		return registered;
	}
	public void setRegistered(boolean registered)
	{
		this.registered = registered;
	}
	public List<QueueComponentConfiguration.BoundedByQueueConfiguration> getBoundedByQueueConfigurationList()
	{
		return boundedByQueueConfigurationList;
	}
	public List<QueueComponentConfiguration.BoundedByQueueId> getBoundByIdList()
	{
		return boundByIdList;
	}
	public List<QueueComponentConfiguration.SubscribeEvent> getSubscribeEventList()
	{
		return subscribeEventList;
	}
	
	public List<ControllerFilterObjects> getFilterObjectList()
	{
		return filterObjectList;
	}

	public Set<String> getFilterAttributeSet()
	{
		return filterAttributes;
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
	
	public void clean()
	{
		this.dispatcher = null;
		this.properties = null;
		this.queueController = null;
		this.boundByIdList = null;
		this.boundedByQueueConfigurationList = null;
		this.subscribeEventList = null;
		this.filterObjectList = null;
		this.filterAttributes = null;
	}
	
	public class ControllerFilterObjects
	{
		QueueComponentConfiguration.BoundedByQueueConfiguration bound = null;
		String filterExpression = null;
		Filter filter = null;
		Set<String> attributes = new HashSet<String>();
	}
}
