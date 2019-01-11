/*******************************************************************************
 * Copyright (c) 2019 Sebastian Palarus
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.sodeac.eventdispatcher.api.QueueComponentConfiguration;
import org.sodeac.multichainlist.LinkageDefinition;
import org.sodeac.multichainlist.Linker;
import org.sodeac.multichainlist.LinkerBuilder;

/**
 * ChainDispatcher is a kind of cached linker factory. The combination of applied chaindispatcherrules ist the key.
 * 
 * @author Sebastian Palarus
 *
 */
public class ChainDispatcher
{
	private QueueImpl queue = null;
	private volatile List<ConfigurationContainer> configurationContainerList = null;
	private volatile Map<String,Linker<QueuedEventImpl>> linkerCache = null;
	
	public ChainDispatcher(QueueImpl queue)
	{
		super();
		this.queue = queue;
		this.configurationContainerList = new ArrayList<ConfigurationContainer>();
		this.linkerCache = new HashMap<String,Linker<QueuedEventImpl>>();
	}
	
	public void reset()
	{
		
		List<ConfigurationContainer> newConfigurationContainerList = new ArrayList<ConfigurationContainer>();
		
		AtomicLong sequenceGenerator = new AtomicLong(1);
		for(ControllerContainer controllerContainer : queue.getConfigurationList())
		{
			if(controllerContainer.getChainDispatcherRuleConfigurationList() == null)
			{
				continue;
			}
			
			for(QueueComponentConfiguration.ChainDispatcherRuleConfiguration chainDispatcherRuleConfiguration : controllerContainer.getChainDispatcherRuleConfigurationList())
			{
				if(queue.ruleIsDisabled(chainDispatcherRuleConfiguration.getRuleId()))
				{
					continue;
				}
				ConfigurationContainer configurationContainer = new ConfigurationContainer();
				configurationContainer.sequence =sequenceGenerator.getAndIncrement();
				configurationContainer.chainDispatcherRuleConfiguration = chainDispatcherRuleConfiguration;
				newConfigurationContainerList.add(configurationContainer);
			}
		}
		
		this.configurationContainerList = newConfigurationContainerList;
		this.linkerCache = new HashMap<String,Linker<QueuedEventImpl>>();
	}
	
	public Linker<QueuedEventImpl> getLinker(QueuedEventImpl event)
	{
		if(this.configurationContainerList.isEmpty())
		{
			return queue.eventQueue.defaultLinker();
		}
		
		StringBuilder key = new StringBuilder();
		
		for(ConfigurationContainer configurationContainer : this.configurationContainerList)
		{
			
				if(configurationContainer.chainDispatcherRuleConfiguration.getEventPredicate() != null)
				{
					if(! configurationContainer.chainDispatcherRuleConfiguration.getEventPredicate().test(event.getEvent()))
					{
						configurationContainer.predicateResult = false;
						continue;
					}
				}
				
				configurationContainer.predicateResult = true;
				
				key.append(Long.toString(configurationContainer.sequence) +".");
		}
		
		if(key.length() == 0)
		{
			return this.linkerCache.get(key.toString()) ;
		}
		
		if(this.linkerCache.get(key.toString()) != null)
		{
			return this.linkerCache.get(key.toString()) ;
		}
		
		Map<String,String> linkageMap = new HashMap<String,String>();
		
		for(ConfigurationContainer configurationContainer : this.configurationContainerList)
		{
			if(! configurationContainer.predicateResult)
			{
				continue;
			}
			
			if(configurationContainer.chainDispatcherRuleConfiguration.getLinksToAdd() != null)
			{
				Linker<QueuedEventImpl>  linker = configurationContainer.chainDispatcherRuleConfiguration.getLinksToAdd().build(queue.eventQueue);
				Linker<QueuedEventImpl>.LinkageDefinitionContainer linkageDefinitionContainer = linker.getLinkageDefinitionContainer();
				for(LinkageDefinition<QueuedEventImpl> linkageDefinitionToAdd : linkageDefinitionContainer.getLinkageDefinitionList())
				{
					linkageMap.put(linkageDefinitionToAdd.getChainName(), linkageDefinitionToAdd.getPartition().getName());
				}
			}
			if(configurationContainer.chainDispatcherRuleConfiguration.getLinksToRemove() != null)
			{
				Linker<QueuedEventImpl>  linker = configurationContainer.chainDispatcherRuleConfiguration.getLinksToAdd().build(queue.eventQueue);
				Linker<QueuedEventImpl>.LinkageDefinitionContainer linkageDefinitionContainer = linker.getLinkageDefinitionContainer();
				for(LinkageDefinition<QueuedEventImpl> linkageDefinitionToAdd : linkageDefinitionContainer.getLinkageDefinitionList())
				{
					linkageMap.remove(linkageDefinitionToAdd.getChainName());
				}
			}
		}
		
		Map<String,Linker<QueuedEventImpl>> newLinkerCache = new HashMap<String,Linker<QueuedEventImpl>>(this.linkerCache);
		try
		{
			if(linkageMap.isEmpty())
			{
				newLinkerCache.put(key.toString(),queue.eventQueue.defaultLinker()); 
				return queue.eventQueue.defaultLinker();
			}
			
			LinkerBuilder builder = LinkerBuilder.newBuilder();
			for(Entry<String,String> entry : linkageMap.entrySet())
			{
				builder.inPartition(entry.getValue()).linkIntoChain(entry.getKey());
			}
			
			Linker<QueuedEventImpl> linker = builder.build(queue.eventQueue);
			newLinkerCache.put(key.toString(),linker); 
			return linker;
		}
		finally 
		{
			this.linkerCache = newLinkerCache;
			key.setLength(0);
			key.trimToSize(); // help gc
		}
	}

	private class ConfigurationContainer
	{
		private long sequence;
		private QueueComponentConfiguration.ChainDispatcherRuleConfiguration chainDispatcherRuleConfiguration;
		private boolean predicateResult = false;
		
	}
}
