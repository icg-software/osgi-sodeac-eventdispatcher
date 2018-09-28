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
package org.sodeac.eventdispatcher.common.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.osgi.service.log.LogService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.common.CommonEventDispatcherHelper;
import org.sodeac.eventdispatcher.common.controller.DirectoryWatcherConfigurationAdapter.EventType;
import org.sodeac.eventdispatcher.common.controller.DirectoryWatcherConfigurationAdapter.FileType;

@Component
(
	service				= IQueueController.class					,
	configurationPid	= DirectoryWatcherQueueFactoryController.SERVICE_PID	, 
	configurationPolicy	= ConfigurationPolicy.REQUIRE
)
public class DirectoryWatcherQueueFactoryController implements IQueueController, IOnQueueObserve, IOnQueueReverse, IOnQueuedEvent
{
	public static final String SERVICE_PID = "org.sodeac.eventdispatcher.common.controller.directorywatcher";
	
	@ObjectClassDefinition(name=SERVICE_PID, description="",factoryPid=DirectoryWatcherQueueFactoryController.SERVICE_PID)
	interface Config
	{
		@AttributeDefinition(name="dispatcher",description = "id of dispatcher (default:'default')" , defaultValue=IEventDispatcher.DEFAULT_DISPATCHER_ID, type=AttributeType.STRING, required=true)
		String dispatcherid();
		
		@AttributeDefinition(name="queue",description = "queueid of observed queue" ,type=AttributeType.STRING, required=true)
		String queueid();
		
		@AttributeDefinition(name="directory",description = "directory to monitored" ,type=AttributeType.STRING, required=true)
		String directory();
		
		@AttributeDefinition(name="filenamefilter",description = "regularexpression filter for name" ,type=AttributeType.STRING, required=true)
		String filefilter();
		
		@AttributeDefinition(name="filetypefilter",description = "filter for filetype (directory or/and file - ignored on deleteevent)" ,defaultValue="file+directory",type=AttributeType.STRING, required=true)
		String filetypefilter();
		
		@AttributeDefinition(name="eventtypefilter",description = "filter for eventtype (create,delete,modify)" ,defaultValue="create+delete+modify",type=AttributeType.STRING, required=true)
		String eventtypefilter();
		
		@AttributeDefinition(name="eventtopic",description = "topic of event fired on directory update" ,type=AttributeType.STRING, required=true)
		String eventtopic();
		
		@AttributeDefinition(name="enabled",description = "enabled state of watcher" ,type=AttributeType.BOOLEAN,defaultValue="true")
		Boolean enabled();
		
		@AttributeDefinition(name="osgievent",description = "fire event over osgi event admin" ,type=AttributeType.BOOLEAN,defaultValue="true")
		Boolean osgievent();
		
		@AttributeDefinition(name="name",description = "name of filewatcher" ,type=AttributeType.STRING)
		String jmxname();
		
		@AttributeDefinition(name="category",description = "category of filewatcher" ,type=AttributeType.STRING)
		String jmxcategory();
	}
	
	private volatile ComponentContext context = null;
	private volatile Map<String, ?> properties = null;
	private volatile UUID id = null;
	
	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile LogService logService = null;
	
	@Activate
	public void activate(ComponentContext context, Map<String, ?> properties)
	{
		this.context = context;
		this.properties = properties;
	}
	
	@Deactivate
	public void deactivate(ComponentContext context)
	{
		this.context = null;
		this.properties = null;
	}
	
	@Modified 
	public void modified(Map<String, ?> properties)
	{
		this.properties = properties;
		// TODO 
	}

	@Override
	public void onQueueReverse(IQueue queue)
	{
		if(this.id == null)
		{
			return;
		}
		DirectoryWatcherConfigurationAdapter directoryWatcherConfigurationAdapter = queue.getConfigurationPropertyBlock().getAdapter(DirectoryWatcherConfigurationAdapter.class);
		if(directoryWatcherConfigurationAdapter != null)
		{
			directoryWatcherConfigurationAdapter.removeWatcher(this.id);
		}
	}

	@Override
	public void onQueueObserve(IQueue queue)
	{
		DirectoryWatcherConfigurationAdapter directoryWatcherConfigurationAdapter = queue.getConfigurationPropertyBlock().getAdapter(DirectoryWatcherConfigurationAdapter.class, () -> new DirectoryWatcherConfigurationAdapter(queue));
		
		String dir = (String)properties.get("directory");
		if((dir == null) || dir.isEmpty())
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "directory to monitored not defined",null);
			return;
		}
		String topic = (String)properties.get("eventtopic");
		if((topic == null) || topic.isEmpty())
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "topic to fire on directory modified not defined", null);
			return;
		}
		
		FileType fileTypeFilter = FileType.ALL;// TODO
		EventType eventTypeFilter = EventType.ALL; // TODO
		
		this.id = directoryWatcherConfigurationAdapter.addWatcher(new File(dir), (String)properties.get("filefilter"), fileTypeFilter, eventTypeFilter, topic, true,queue);
	}

	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		Object osgiEvent = properties.get("osgievent");
		if(osgiEvent == null)
		{
			return;
		}
		if(! osgiEvent.toString().equalsIgnoreCase("true"))
		{
			return;
		}
		if(! event.getEvent().getTopic().equals((String)properties.get("eventtopic")))
		{
			return;
		}
		if(event.getEvent().getProperty("generatedbyfactorycontroller") != null)
		{
			return;
		}
		
		Map<String,Object> eventProperties = new HashMap<String,Object>(event.getNativeEventProperties());
		eventProperties.put("generatedbyfactorycontroller", true);
		
		event.removeFromQueue();
		event.getQueue().postEvent(event.getEvent().getTopic(), eventProperties);
		
	}

}
