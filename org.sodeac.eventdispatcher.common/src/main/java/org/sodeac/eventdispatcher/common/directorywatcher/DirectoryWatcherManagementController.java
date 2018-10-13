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
package org.sodeac.eventdispatcher.common.directorywatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IDescriptionProvider;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.IQueueSessionScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IStateInfoProvider;
import org.sodeac.eventdispatcher.common.CommonEventDispatcherHelper;

@Component
(
	name				= "DirectoryWatcherManagementController"				,
	service				= IQueueController.class					,
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"="+"("+DirectoryWatcherConfigurationAdapter.ADAPTER_CLASS + "=*)",
	}
)
public class DirectoryWatcherManagementController implements IQueueController,IOnQueueAttach,IOnQueueDetach,IOnQueueSignal,IDescriptionProvider,IStateInfoProvider
{
	private volatile ComponentContext context = null;
	
	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile LogService logService = null;
	
	@Activate
	public void activate(ComponentContext context, Map<String, ?> properties)
	{
		this.context = context;
	}
	
	@Deactivate
	public void deactivate(ComponentContext context)
	{
		this.context = null;
	}
	
	
	@Override
	public void onQueueDetach(IQueue queue)
	{
		queue.getStatePropertyBlock().getAdapter(ConfigurationManagementAdapter.class).close();
		
	}

	@Override
	public void onQueueAttach(IQueue queue)
	{
		// TODO. if queue instanceof Scope
		this.update(queue);
		
	}

	@Override
	public void onQueueSignal(IQueue queue, String signal)
	{
		if(signal.equals(DirectoryWatcherConfigurationAdapter.SIGNAL_NOTIFY_UPDATE))
		{
			this.update(queue);
		}
	}
	
	public void update(IQueue queue)
	{
		try
		{
			DirectoryWatcherConfigurationAdapter directoryWatcherConfigurationAdapter = queue.getConfigurationPropertyBlock().getAdapter(DirectoryWatcherConfigurationAdapter.class);
			queue.getStatePropertyBlock().getAdapter(ConfigurationManagementAdapter.class,() -> new ConfigurationManagementAdapter(queue, directoryWatcherConfigurationAdapter)).update();
		}
		catch (Exception e) 
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "directory to monitored not defined",null);
		}
	}

	@Override
	public String getStateInfo()
	{
		return "TODO";
	}

	@Override
	public String getDescription()
	{
		return "TODO";
	}
	
	private class ConfigurationManagementAdapter
	{
		public ConfigurationManagementAdapter(IQueue queue, DirectoryWatcherConfigurationAdapter directoryWatcherConfigurationAdapter)
		{
			super();
			this.queue = queue;
			this.registeredServiceController = new HashMap<UUID,Long>();
			this.directoryWatcherConfigurationAdapter = directoryWatcherConfigurationAdapter;
		}
		
		private Map<UUID,Long> registeredServiceController = null;
		private IQueue queue = null;
		private DirectoryWatcherConfigurationAdapter directoryWatcherConfigurationAdapter = null;
		
		private void remove(UUID id)
		{
			registeredServiceController.remove(id);
			IQueueSessionScope scope = queue.getSessionScope(id);
			if(scope != null)
			{
				try
				{
					scope.dispose();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			registeredServiceController.remove(id);
		}
		
		public void update()
		{
			List<DirectoryWatcherConfigurationAdapter.WatcherConfiguration> watcherConfigurationList = directoryWatcherConfigurationAdapter.getWatcherConfigurationList();
			Set<UUID> validConfigurationSet = new HashSet<UUID>();
			for(DirectoryWatcherConfigurationAdapter.WatcherConfiguration watcherConfiguration : watcherConfigurationList)
			{
				validConfigurationSet.add(watcherConfiguration.getId());
				
				if(registeredServiceController.get(watcherConfiguration.getId()) != null)
				{
					if(registeredServiceController.get(watcherConfiguration.getId()) == watcherConfiguration.getVersion())
					{
						continue;
					}
					remove(watcherConfiguration.getId());
				}
				
				Map<String,Object> configurationProperties = new HashMap<String,Object>();
				
				configurationProperties.put("directory", watcherConfiguration.getDirectory());
				configurationProperties.put("filefilter", watcherConfiguration.getFileNameFilter());
				
				if(watcherConfiguration.getFileTypeFilter() == DirectoryWatcherConfigurationAdapter.FileType.ALL)
				{
					configurationProperties.put("filetypefilter", "file+directory");
				}
				else
				{
					configurationProperties.put("filetypefilter", watcherConfiguration.getFileTypeFilter().toString().toLowerCase().replaceAll("_and_", "+"));
				}
				
				if(watcherConfiguration.getEventTypeFilter() == DirectoryWatcherConfigurationAdapter.EventType.ALL)
				{
					configurationProperties.put("eventtypefilter", "create+delete+modify");
				}
				else
				{
					configurationProperties.put("eventtypefilter", watcherConfiguration.getFileTypeFilter().toString().toLowerCase().replaceAll("_and_", "+"));
				}
				
				configurationProperties.put("eventtopic", watcherConfiguration.getEventtopic());
				configurationProperties.put("notifyqueue", watcherConfiguration.getWatchEventNotifyQueue());
				configurationProperties.put(IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER, DirectoryWatcherServiceController.SERVICE_ID);
				
				queue.createSessionScope
				(
					watcherConfiguration.getId(), 
					"DirectoryWatcher " + watcherConfiguration.getId().toString(), 
					null, 
					configurationProperties, 
					null, 
					false, false
				);
				
				registeredServiceController.put(watcherConfiguration.getId(), watcherConfiguration.getVersion());
			}
			
			List<UUID> removeList = new ArrayList<UUID>();
			for(UUID id : registeredServiceController.keySet())
			{
				if(! validConfigurationSet.contains(id))
				{
					removeList.add(id);
				}
			}
			for(UUID id : removeList)
			{
				remove(id);
			}
		}
		
		public void close()
		{
			List<UUID> removeList = new ArrayList<UUID>();
			for(UUID id : registeredServiceController.keySet())
			{
				removeList.add(id);
			}
			for(UUID id : removeList)
			{
				remove(id);
			}
		}
	}
}
