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
package org.sodeac.eventdispatcher.common.directorywatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.log.LogService;
import org.osgi.service.event.Event;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.ITaskControl;
import org.sodeac.eventdispatcher.api.IMetrics;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueTask;
import org.sodeac.eventdispatcher.api.IQueueService;
import org.sodeac.eventdispatcher.common.CommonEventDispatcherHelper;

@Component
(
	name				= "DirectoryWatcherServiceController"				,
	service				=  {IQueueController.class,IQueueService.class}		,
	property=
	{
		EventDispatcherConstants.PROPERTY_QUEUE_MATCH_FILTER+"=("+EventDispatcherConstants.PROPERTY_QUEUE_MATCH_FILTER + "=" + DirectoryWatcherServiceController.SERVICE_ID + ")",
		EventDispatcherConstants.PROPERTY_SERVICE_ID + "=" + DirectoryWatcherServiceController.SERVICE_ID,
		EventDispatcherConstants.PROPERTY_PERIODIC_REPETITION_INTERVAL + "=0",
		EventDispatcherConstants.PROPERTY_TIMEOUT_MS + "=" + (1000L * 3600L * 24L * 365L * 1080L), 
	}
)
public class DirectoryWatcherServiceController implements IQueueController, IOnTaskError, IOnQueueAttach, IOnQueueDetach, IQueueService
{
	private volatile ComponentContext context = null;
	
	public static final String SERVICE_ID = "org.sodeac.eventdispatcher.common.directorywatcher";
	
	
	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile LogService logService = null;
	
	@Activate
	public void activate(ComponentContext context)
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
		queue.getConfigurationPropertyBlock().setProperty(SERVICE_ID + ".run", false);
		
		WatchService watchService = queue.getStatePropertyBlock().getAdapter(WatchService.class);
		if(watchService != null)
		{
			try
			{
				queue.getStatePropertyBlock().removeAdapter(WatchService.class);
				watchService.close();
			}
			catch(Exception e){}
		}
	}

	@Override
	public void onQueueAttach(IQueue queue)
	{
		File dir = queue.getConfigurationPropertyBlock().getProperty("directory", File.class);
		if(dir == null) 
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "directory to monitored not defined",null);
			return;
		}
		String topic = queue.getConfigurationPropertyBlock().getProperty("eventtopic",String.class);
		if((topic == null) || topic.isEmpty())
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "topic to fire on directory modified not defined", null);
			return;
		}
		
		queue.getConfigurationPropertyBlock().setProperty(SERVICE_ID + ".run", true);
	}
	
	@Override
	public void run
	(
		IQueue queue, IMetrics metrics, IPropertyBlock propertyBlock, 
		ITaskControl taskControl,
		List<IQueueTask> currentProcessedTaskList
	)
	{
		if(! queue.getConfigurationPropertyBlock().getPropertyOrDefault(SERVICE_ID + ".run" , Boolean.class, false))
		{
			taskControl.setExecutionTimestamp(System.currentTimeMillis() + (1080L * 3600L),true);
			return;
		}
		
		File dir = queue.getConfigurationPropertyBlock().getProperty("directory",File.class);
		if(dir == null)
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "directory to monitored not defined",null);
			taskControl.setExecutionTimestamp(System.currentTimeMillis() + (1080L * 3600L),true);
			return;
		}
		String topic = queue.getConfigurationPropertyBlock().getProperty("eventtopic",String.class);
		if((topic == null) || topic.isEmpty())
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "topic to fire on directory modified not defined", null);
			taskControl.setExecutionTimestamp(System.currentTimeMillis() + (1080L * 3600L),true);
			return;
		}
		
		WatchService watchService = null;
		
		try
		{
			Path path = Paths.get(dir.getCanonicalPath());
			
			boolean filterFile = CommonEventDispatcherHelper.getSwitchFromPropertyBlock(queue.getConfigurationPropertyBlock(), "filetypefilter", "file");
			boolean filterDirectory = CommonEventDispatcherHelper.getSwitchFromPropertyBlock(queue.getConfigurationPropertyBlock(), "filetypefilter", "directory");
			
			boolean filterCreate = CommonEventDispatcherHelper.getSwitchFromPropertyBlock(queue.getConfigurationPropertyBlock(), "eventtypefilter", "create");
			boolean filterDelete = CommonEventDispatcherHelper.getSwitchFromPropertyBlock(queue.getConfigurationPropertyBlock(), "eventtypefilter", "delete");
			boolean filterModify = CommonEventDispatcherHelper.getSwitchFromPropertyBlock(queue.getConfigurationPropertyBlock(), "eventtypefilter", "modify");
			
			String fileNameFilter = queue.getConfigurationPropertyBlock().getPropertyOrDefault("filefilter", String.class, "");
			IQueue notifyQueue = queue.getConfigurationPropertyBlock().getProperty("notifyqueue",IQueue.class);
			
			watchService = FileSystems.getDefault().newWatchService();
			queue.getStatePropertyBlock().setAdapter(WatchService.class, watchService);
			path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,StandardWatchEventKinds.ENTRY_MODIFY); // if path not exists: java.nio.file.NoSuchFileException
			List<Event> list = new ArrayList<Event>();
			while(queue.getConfigurationPropertyBlock().getPropertyOrDefault(SERVICE_ID + ".run" , Boolean.class, false))
			{
				try
				{
					WatchKey key;
					
					while ((key = watchService.take()) != null)
					{
						for (WatchEvent<?> event : key.pollEvents())
						{
							WatchEvent.Kind<?> kind = event.kind();

					        if (kind == StandardWatchEventKinds.OVERFLOW) 
					        {
					        	continue;
					        }
					        
					        File file = new File(path.toFile(),event.context().toString());
					        
							Map<String,Object> properties = new HashMap<String,Object>();
							properties.put("kind", event.kind().toString());
							properties.put("filename", event.context().toString());
							properties.put("count", event.count());
							properties.put("file", file);
							Event updateEvent = new Event(topic,properties);
							list.add(updateEvent);
						}
						key.reset();
						
						for(Event event : list)
						{
							boolean fileFilterOk = false;
							
							File file = (File)event.getProperty("file");
							if(filterDirectory && file.isDirectory())
							{
								fileFilterOk = true;
							}
							else if(filterFile && file.isFile())
							{
								fileFilterOk = true;
							}
							else if(event.getProperty("kind").equals(StandardWatchEventKinds.ENTRY_DELETE.toString()))
							{
								fileFilterOk = true;
							}
							if(! fileFilterOk)
							{
								continue;
							}
							
							boolean eventFilterOk = false;
							if(filterCreate && event.getProperty("kind").equals(StandardWatchEventKinds.ENTRY_CREATE.toString()))
							{
								eventFilterOk = true;
							}
							else if(filterDelete && event.getProperty("kind").equals(StandardWatchEventKinds.ENTRY_DELETE.toString()))
							{
								eventFilterOk = true;
							}
							
							else if(filterModify && event.getProperty("kind").equals(StandardWatchEventKinds.ENTRY_MODIFY.toString()))
							{
								eventFilterOk = true;
							}
							
							if(! eventFilterOk)
							{
								continue;
							}
							
							if((fileNameFilter != null) && (! fileNameFilter.isEmpty()))
							{
								String fileName = ((File)event.getProperty("file")).getName();
								if(! fileName.matches(fileNameFilter))
								{
									continue;
								}
							}
							
							if(notifyQueue != null)
							{
								notifyQueue.queueEvent(event);
							}
						}
						
						list.clear();
					}
				}
				catch(InterruptedException e){}
				catch(Exception e)
				{
					if(e instanceof ClosedWatchServiceException)
					{
						queue.getConfigurationPropertyBlock().setProperty(SERVICE_ID + ".run", false);
					}
					else
					{
						CommonEventDispatcherHelper.log(DirectoryWatcherServiceController.this.context, DirectoryWatcherServiceController.this.logService, LogService.LOG_ERROR, "Error on watch directory for modification", e);
					}
				}
			}
			if(! queue.getConfigurationPropertyBlock().getPropertyOrDefault(SERVICE_ID + ".run" , Boolean.class, false))
			{
				taskControl.setExecutionTimestamp(System.currentTimeMillis() + (1080L * 3600L),true);
				return;
			}
			
			
		}
		catch(NoSuchFileException e )
		{
			taskControl.setExecutionTimestamp(System.currentTimeMillis() + (1080L * 3600L),true);
			CommonEventDispatcherHelper.log(DirectoryWatcherServiceController.this.context, DirectoryWatcherServiceController.this.logService, LogService.LOG_ERROR, "directory for modification not exists", e);
		}
		catch(IOException e )
		{
			taskControl.setExecutionTimestamp(System.currentTimeMillis() + (1080L * 3600L),true);
			CommonEventDispatcherHelper.log(DirectoryWatcherServiceController.this.context, DirectoryWatcherServiceController.this.logService, LogService.LOG_ERROR, "error watching directory for modification", e);
		}
		finally
		{
			try
			{
				queue.getStatePropertyBlock().removeAdapter(WatchService.class);
				if(watchService != null) { watchService.close();}
			}
			catch(Exception e){}
		}
		
	}
	

	@Override
	public void onTaskError(IQueue queue, IQueueTask task, Throwable exception)
	{
		CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "error watching directory for modification", exception);
	}
}
