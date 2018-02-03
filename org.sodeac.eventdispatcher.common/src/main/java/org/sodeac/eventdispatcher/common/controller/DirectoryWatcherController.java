/*******************************************************************************
 * Copyright (c) 2017 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.common.controller;

import java.io.File;
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
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.log.LogService;
import org.osgi.service.event.Event;
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
import org.sodeac.eventdispatcher.api.IDescriptionProvider;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnScheduleEvent;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.api.IQueueJob;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.api.IStateInfoProvider;
import org.sodeac.eventdispatcher.common.CommonEventDispatcherHelper;
import org.sodeac.eventdispatcher.common.job.FireAsyncEvent;

@Component
(
	name				= "DirectoryWatcherController"				,
	service				= IEventController.class					,
	configurationPid	= DirectoryWatcherController.SERVICE_PID	, 
	configurationPolicy	= ConfigurationPolicy.REQUIRE
)
public class DirectoryWatcherController implements IEventController, IOnScheduleEvent, IOnJobError, IOnQueueObserve, IOnQueueReverse,IDescriptionProvider,IStateInfoProvider
{
	public static final String SERVICE_PID = "org.sodeac.eventdispatcher.common.controller.directorywatcher";
	
	@ObjectClassDefinition(name=SERVICE_PID, description="",factoryPid=DirectoryWatcherController.SERVICE_PID)
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
		
		@AttributeDefinition(name="enabled",description = "directory to monitored" ,type=AttributeType.BOOLEAN,defaultValue="true")
		Boolean enabled();
		
		@AttributeDefinition(name="name",description = "name of filewatcher" ,type=AttributeType.STRING)
		String jmxname();
		
		@AttributeDefinition(name="category",description = "category of filewatcher" ,type=AttributeType.STRING)
		String jmxcategory();
	}
	
	private volatile ComponentContext context = null;
	private volatile Map<String, ?> properties = null;
	private volatile WatcherThread watcherThread = null;
	private volatile IQueue queue = null;
	
	private ReentrantLock lock = null;
	
	public DirectoryWatcherController()
	{
		super();
		lock = new ReentrantLock();
	}
	
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
		stopWatching();
	}
	
	@Modified 
	public void modified(Map<String, ?> properties)
	{
		this.properties = properties;
		stopWatching();
		startWatching();
	}

	@Override
	public void onQueueReverse(IQueue queue)
	{
		stopWatching();
		this.queue = null;
	}

	@Override
	public void onQueueObserve(IQueue queue)
	{
		this.queue = queue;
		startWatching();
	}

	@Override
	public void onJobError(IQueueJob job, Exception exception)
	{
		CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "Error on run EventDispatcher job", exception);
	}

	@Override
	public void onScheduleEvent(IQueuedEvent event)
	{
		event.getQueue().removeEvent(event.getUUID());
		
		IQueue currentQueue = this.queue;
		
		if(currentQueue == null)
		{
			return;
		}
		
		if(currentQueue != event.getQueue())
		{
			return;
		}
		
		Map<String, ?> currentProperties = this.properties;
		if(currentProperties == null)
		{
			return;
		}
		
		String topic = (String)currentProperties.get("eventtopic");
		if((topic == null) || topic.isEmpty())
		{
			return;
		}
		
		if(! topic.equals(event.getEvent().getTopic()))
		{
			return;
		}
		
		event.getQueue().scheduleJob(new FireAsyncEvent(event,false));
	}
	
	private void stopWatching()
	{
		lock.lock();
		try
		{
			WatcherThread currentWatcherThread = this.watcherThread;
			if(currentWatcherThread == null)
			{
				return;
			}
			
			currentWatcherThread.stopService();
			this.watcherThread = null;
		}
		catch (Exception e) 
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "Error on stop directory watcher thread", e);
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	private void startWatching()
	{
		lock.lock();
		try
		{
			ComponentContext currentContext = this.context;
			Map<String, ?> currentProperties = this.properties;
			IQueue currentQueue = this.queue;
			if(currentContext == null)
			{
				return;
			}
			if(currentProperties == null)
			{
				return;
			}
			if(currentQueue == null)
			{
				return;
			}
			
			String dir = (String)currentProperties.get("directory");
			if((dir == null) || dir.isEmpty())
			{
				CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "directory to monitored not defined",null);
				return;
			}
			String topic = (String)currentProperties.get("eventtopic");
			if((topic == null) || topic.isEmpty())
			{
				CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "topic to fire on directory modified not defined", null);
				return;
			}
			
			Path path = Paths.get(dir);
			WatcherThread currentWatcherThread = new WatcherThread
			(
				path,
				currentQueue,
				topic,
				CommonEventDispatcherHelper.getSwitchFromProperty(this.properties, "filetypefilter", "file"),
				CommonEventDispatcherHelper.getSwitchFromProperty(this.properties, "filetypefilter", "directory"),
				CommonEventDispatcherHelper.getSwitchFromProperty(this.properties, "eventtypefilter", "create"),
				CommonEventDispatcherHelper.getSwitchFromProperty(this.properties, "eventtypefilter", "delete"),
				CommonEventDispatcherHelper.getSwitchFromProperty(this.properties, "eventtypefilter", "modify"),
				(String)currentProperties.get("filefilter")
			);
			this.watcherThread = currentWatcherThread;
			this.watcherThread.start();
		}
		catch (Exception e) 
		{
			CommonEventDispatcherHelper.log(context, logService, LogService.LOG_ERROR, "Error on start directory watcher thread", e);
		}
		finally 
		{
			lock.unlock();
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
	
	private class WatcherThread extends Thread
	{
		private boolean runFlag = true;
		private Path path = null;
		private volatile WatchService watchService = null;
		private IQueue queue = null;
		private String topic = null;
		
		private boolean filterFile = false;
		private boolean filterDirectory = false;
		
		private boolean filterCreate = false;
		private boolean filterDelete = false;
		private boolean filterModify = false;
		
		private String fileNameFilter = null;
		
		public WatcherThread(Path path,IQueue queue,String topic,boolean filterFile, boolean filterDirectory, boolean filterCreate, boolean filterDelete, boolean filterModify, String fileNameFilter)
		{
			super();
			super.setName("DirectoryWatcherEventControllerThread");
			super.setDaemon(true);
			this.path = path;
			this.queue = queue;
			this.topic = topic;
			
			this.filterDirectory = filterDirectory;
			this.filterFile = filterFile;
			
			this.filterCreate = filterCreate;
			this.filterDelete = filterDelete;
			this.filterModify = filterModify;
			
			this.fileNameFilter = fileNameFilter;
		}

		@Override
		public void run()
		{
			try
			{
				watchService = FileSystems.getDefault().newWatchService();
				path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE,StandardWatchEventKinds.ENTRY_MODIFY); // if path not exists: java.nio.file.NoSuchFileException
				List<Event> list = new ArrayList<Event>();
				while(runFlag)
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
								Event updateEvent = new Event(this.topic,properties);
								list.add(updateEvent);
							}
							key.reset();
							
							for(Event event : list)
							{
								boolean fileFilterOk = false;
								
								File file = (File)event.getProperty("file");
								if(this.filterDirectory && file.isDirectory())
								{
									fileFilterOk = true;
								}
								else if(this.filterFile && file.isFile())
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
								if(this.filterCreate && event.getProperty("kind").equals(StandardWatchEventKinds.ENTRY_CREATE.toString()))
								{
									eventFilterOk = true;
								}
								else if(this.filterDelete && event.getProperty("kind").equals(StandardWatchEventKinds.ENTRY_DELETE.toString()))
								{
									eventFilterOk = true;
								}
								
								else if(this.filterModify && event.getProperty("kind").equals(StandardWatchEventKinds.ENTRY_MODIFY.toString()))
								{
									eventFilterOk = true;
								}
								
								if(! eventFilterOk)
								{
									continue;
								}
								
								if((fileNameFilter != null) && (! fileNameFilter.isEmpty()))
								{
									String fileName = (String)event.getProperty("file");
									if(! fileName.matches(fileNameFilter))
									{
										continue;
									}
								}
								
								queue.scheduleEvent(event);
							}
							
							list.clear();
						}
					}
					catch(InterruptedException e){}
					catch(Exception e)
					{
						if(e instanceof ClosedWatchServiceException)
						{
							runFlag = false;
						}
						else
						{
							CommonEventDispatcherHelper.log(DirectoryWatcherController.this.context, DirectoryWatcherController.this.logService, LogService.LOG_ERROR, "Error on watch directory for modification", e);
						}
					}
				}
				
			}
			catch(NoSuchFileException e )
			{
				CommonEventDispatcherHelper.log(DirectoryWatcherController.this.context, DirectoryWatcherController.this.logService, LogService.LOG_ERROR, "directory for modification not exists", e);
			}
			catch (Exception e) 
			{
				CommonEventDispatcherHelper.log(DirectoryWatcherController.this.context, DirectoryWatcherController.this.logService, LogService.LOG_ERROR, "Error on watch directory for modification", e);
			}
			
			try
			{
				watchService.close();
			}
			catch(Exception e){}
		}

		public void stopService()
		{
			this.runFlag = false;
			try
			{
				this.watchService.close();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}

		public boolean isRunFlag()
		{
			return runFlag;
		}
		
	}

}
