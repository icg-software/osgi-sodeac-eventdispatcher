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

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IFeatureConfigurableController;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnQueuedEventList;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.MetricsRequirement;
import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.xuri.ldapfilter.Attribute;
import org.sodeac.xuri.ldapfilter.AttributeLinker;
import org.sodeac.xuri.ldapfilter.IFilterItem;
import org.sodeac.xuri.ldapfilter.LDAPFilterDecodingHandler;

public class ControllerContainer
{
	public ControllerContainer
	(
		EventDispatcherImpl dispatcher,
		IQueueController queueController, 
		Map<String, ?> properties, 
		List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList, 
		List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList, 
		List<QueueComponentConfiguration.SubscribeEvent> subscribeEventList,
		List<QueueComponentConfiguration.ChainDispatcherRuleConfiguration> chainDispatcherRuleConfigurationList,
		List<QueueComponentConfiguration.RunTaskOnTriggerRuleConfiguration> runTaskOnQueuedInChainRuleConfigurationList
	)
	{
		super();
		this.boundedByQueueConfigurationList = boundedByQueueConfigurationList;
		this.boundByIdList = boundByIdList;
		this.subscribeEventList = subscribeEventList;
		this.chainDispatcherRuleConfigurationList = chainDispatcherRuleConfigurationList;
		this.runTaskOnQueuedInChainRuleConfigurationList = runTaskOnQueuedInChainRuleConfigurationList;
		this.dispatcher = dispatcher;
		this.queueController = queueController;
		this.properties = properties;
		this.createFilterObjectList();
		this.detectControllerImplementions();
	}
	
	private EventDispatcherImpl dispatcher = null;
	private volatile Map<String, ?> properties = null;
	private volatile IQueueController queueController = null;
	private List<QueueComponentConfiguration.BoundedByQueueId> boundByIdList = null;
	private List<QueueComponentConfiguration.BoundedByQueueConfiguration> boundedByQueueConfigurationList = null;
	private List<QueueComponentConfiguration.SubscribeEvent> subscribeEventList = null;
	private List<QueueComponentConfiguration.ChainDispatcherRuleConfiguration> chainDispatcherRuleConfigurationList = null;
	private List<QueueComponentConfiguration.RunTaskOnTriggerRuleConfiguration> runTaskOnQueuedInChainRuleConfigurationList = null;
	
	private volatile boolean registered = false;
	
	private volatile List<ControllerFilterObjects> filterObjectList;
	private volatile Set<String> filterAttributes;
	
	private volatile boolean implementsIOnFireEvent = false;
	private volatile boolean implementsIOnTaskDone = false;
	private volatile boolean implementsIOnTaskError = false;
	private volatile boolean implementsIOnTaskTimeout = false;
	private volatile boolean implementsIOnQueueAttach = false;
	private volatile boolean implementsIOnQueueDetach = false;
	private volatile boolean implementsIOnQueueSignal = false;
	private volatile boolean implementsIOnScheduleEvent = false;
	private volatile boolean implementsIOnScheduleEventList = false;
	private volatile boolean implementsIOnRemoveEvent = false;
	
	public void detectControllerImplementions()
	{
		if(this.queueController == null)
		{
			implementsIOnFireEvent = false;
			implementsIOnTaskDone = false;
			implementsIOnTaskError = false;
			implementsIOnTaskTimeout = false;
			implementsIOnQueueAttach = false;
			implementsIOnQueueDetach = false;
			implementsIOnQueueSignal = false;
			implementsIOnScheduleEvent = false;
			implementsIOnScheduleEventList = false;
			implementsIOnRemoveEvent = false;
			implementsIOnTaskTimeout = false;
			return;
		}
		
		
		if(this.queueController instanceof IFeatureConfigurableController)
		{
			IFeatureConfigurableController featureConfigurableController = (IFeatureConfigurableController)this.queueController;
			implementsIOnFireEvent = featureConfigurableController.implementsOnFiredEvent();
			implementsIOnTaskDone = featureConfigurableController.implementsOnTaskDone();
			implementsIOnTaskError = featureConfigurableController.implementsOnTaskError();
			implementsIOnTaskTimeout = featureConfigurableController.implementsOnTaskTimeout();
			implementsIOnQueueAttach = featureConfigurableController.implementsOnQueueAttach();
			implementsIOnQueueDetach = featureConfigurableController.implementsOnQueueDetach();
			implementsIOnQueueSignal = featureConfigurableController.implementsOnQueueSignal();
			implementsIOnScheduleEvent = featureConfigurableController.implementsOnQueuedEvent();
			implementsIOnScheduleEventList = featureConfigurableController.implementsOnQueuedEventList();
			implementsIOnRemoveEvent = featureConfigurableController.implementsOnRemovedEvent();
		}
		else
		{
			implementsIOnFireEvent = this.queueController instanceof IOnFiredEvent;
			implementsIOnTaskDone = this.queueController instanceof IOnTaskDone;
			implementsIOnTaskError = this.queueController instanceof IOnTaskError;
			implementsIOnTaskTimeout = this.queueController instanceof IOnTaskTimeout;
			implementsIOnQueueAttach = this.queueController instanceof IOnQueueAttach;
			implementsIOnQueueDetach = this.queueController instanceof IOnQueueDetach;
			implementsIOnQueueSignal = this.queueController instanceof IOnQueueSignal;
			implementsIOnScheduleEvent = this.queueController instanceof IOnQueuedEvent;
			implementsIOnScheduleEventList = this.queueController instanceof IOnQueuedEventList;
			implementsIOnRemoveEvent = this.queueController instanceof IOnRemovedEvent;
		}
	}
	
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
	public IQueueController getQueueController()
	{
		return queueController;
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

	public boolean isImplementingIOnFireEvent()
	{
		return implementsIOnFireEvent;
	}

	public boolean isImplementingIOnTaskDone()
	{
		return implementsIOnTaskDone;
	}

	public boolean isImplementingIOnTaskError()
	{
		return implementsIOnTaskError;
	}

	public boolean isImplementingIOnQueueAttach()
	{
		return implementsIOnQueueAttach;
	}

	public boolean isImplementingIOnQueueDetach()
	{
		return implementsIOnQueueDetach;
	}

	public boolean isImplementingIOnQueueSignal()
	{
		return implementsIOnQueueSignal;
	}

	public boolean isImplementingIOnScheduleEvent()
	{
		return implementsIOnScheduleEvent;
	}

	public boolean isImplementingIOnScheduleEventList()
	{
		return implementsIOnScheduleEventList;
	}

	public boolean isImplementingIOnRemoveEvent()
	{
		return implementsIOnRemoveEvent;
	}

	public boolean isImplementingIOnTaskTimeout()
	{
		return implementsIOnTaskTimeout;
	}

	public List<QueueComponentConfiguration.ChainDispatcherRuleConfiguration> getChainDispatcherRuleConfigurationList()
	{
		return chainDispatcherRuleConfigurationList;
	}

	public List<QueueComponentConfiguration.RunTaskOnTriggerRuleConfiguration> getRunTaskOnQueuedInChainRuleConfigurationList()
	{
		return runTaskOnQueuedInChainRuleConfigurationList;
	}
	
}
