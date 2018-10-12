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
package org.sodeac.eventdispatcher.common.directorywatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.sodeac.eventdispatcher.api.IQueue;

public class DirectoryWatcherConfigurationAdapter
{
	public static final String ADAPTER_CLASS = "org.sodeac.eventdispatcher.common.directorywatcher.DirectoryWatcherConfigurationAdapter";
	
	public DirectoryWatcherConfigurationAdapter(IQueue watcherServiceControllerQueue)
	{
		super();
		this.watcherConfigurationList = new LinkedList<DirectoryWatcherConfigurationAdapter.WatcherConfiguration>();
		this.lock = new ReentrantLock();
		this.version = UUID.randomUUID();
		this.watcherServiceControllerQueue = watcherServiceControllerQueue;
	}
	
	public enum FileType {DIRECTORY,FILE,ALL};
	public enum EventType {CREATE,DELETE,MODIFY,CREATE_AND_DELETE,CREATE_AND_MODIFY,DELETE_AND_MODIFY,ALL};
	
	public static final String SIGNAL_NOTIFY_UPDATE = "org.sodeac.eventdispatcher.common.directorywatcher.configuration.update";
	
	private LinkedList<DirectoryWatcherConfigurationAdapter.WatcherConfiguration>  watcherConfigurationList = null;
	private Lock lock = null;
	private UUID version = null;
	private IQueue watcherServiceControllerQueue = null;
	
	public UUID addWatcher(File directory, String fileNameFilter, FileType fileTypeFilter, EventType eventTypeFilter, String eventtopic, boolean enabled, IQueue watchEventNotifyQueue)
	{
		UUID id = UUID.randomUUID();
		
		lock.lock();
		try
		{
			DirectoryWatcherConfigurationAdapter.WatcherConfiguration watcherConfiguration = new DirectoryWatcherConfigurationAdapter.WatcherConfiguration();
			watcherConfiguration.id = id;
			watcherConfiguration.directory = directory;
			watcherConfiguration.fileNameFilter = fileNameFilter;
			watcherConfiguration.fileTypeFilter = fileTypeFilter;
			watcherConfiguration.eventTypeFilter = eventTypeFilter;
			watcherConfiguration.eventtopic = eventtopic;
			watcherConfiguration.enabled = enabled;
			watcherConfiguration.watcherServiceControllerQueue = this.watcherServiceControllerQueue;
			watcherConfiguration.watchEventNotifyQueue = watchEventNotifyQueue;
			
			watcherConfigurationList.add(watcherConfiguration);
			this.version = UUID.randomUUID();
			this.notifyUpdate();
		}
		finally 
		{
			lock.unlock();
		}
		return id;
	}
	
	public void removeWatcher(UUID id)
	{
		lock.lock();
		try
		{
			for(DirectoryWatcherConfigurationAdapter.WatcherConfiguration watcherConfiguration : this.watcherConfigurationList)
			{
				if(watcherConfiguration.id.equals(id))
				{
					watcherConfiguration.id = null;
					watcherConfiguration.directory = null;
					watcherConfiguration.fileNameFilter = null;
					watcherConfiguration.fileTypeFilter = null;
					watcherConfiguration.eventTypeFilter = null;
					watcherConfiguration.eventtopic = null;
					watcherConfiguration.watcherServiceControllerQueue = null;
					watcherConfiguration.watchEventNotifyQueue = null;
					
					this.watcherConfigurationList.remove(watcherConfiguration);
					this.version = UUID.randomUUID();
					this.notifyUpdate();
					break;
				}
			}
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	protected List<DirectoryWatcherConfigurationAdapter.WatcherConfiguration> getWatcherConfigurationList()
	{
		lock.lock();
		try
		{
			List<DirectoryWatcherConfigurationAdapter.WatcherConfiguration> copy = new ArrayList<DirectoryWatcherConfigurationAdapter.WatcherConfiguration>();
			for(DirectoryWatcherConfigurationAdapter.WatcherConfiguration watcherConfiguration : this.watcherConfigurationList)
			{
				DirectoryWatcherConfigurationAdapter.WatcherConfiguration watcherConfigurationCopy = new DirectoryWatcherConfigurationAdapter.WatcherConfiguration();
				watcherConfigurationCopy.id = watcherConfiguration.id;
				watcherConfigurationCopy.directory = watcherConfiguration.directory;
				watcherConfigurationCopy.fileNameFilter = watcherConfiguration.fileNameFilter;
				watcherConfigurationCopy.fileTypeFilter = watcherConfiguration.fileTypeFilter;
				watcherConfigurationCopy.eventTypeFilter = watcherConfiguration.eventTypeFilter;
				watcherConfigurationCopy.eventtopic = watcherConfiguration.eventtopic;
				watcherConfigurationCopy.enabled = watcherConfiguration.enabled;
				watcherConfigurationCopy.watcherServiceControllerQueue = watcherConfiguration.watcherServiceControllerQueue;
				watcherConfigurationCopy.watchEventNotifyQueue = watcherConfiguration.watchEventNotifyQueue;
				
				copy.add(watcherConfigurationCopy);
			}
			return copy;
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	public void clear()
	{
		lock.lock();
		try
		{
			for(WatcherConfiguration watcherConfiguration : this.watcherConfigurationList)
			{
				watcherConfiguration.id = null;
				watcherConfiguration.directory = null;
				watcherConfiguration.fileNameFilter = null;
				watcherConfiguration.fileTypeFilter = null;
				watcherConfiguration.eventTypeFilter = null;
				watcherConfiguration.eventtopic = null;
				watcherConfiguration.watcherServiceControllerQueue = null;
				watcherConfiguration.watchEventNotifyQueue= null;
				
			}
			this.watcherConfigurationList.clear();
			this.version = UUID.randomUUID();
			this.notifyUpdate();
		}
		finally 
		{
			lock.unlock();
		}
	}
	
	protected UUID getVersion()
	{
		return this.version;
	}
	
	private void notifyUpdate()
	{
		try
		{
			this.watcherServiceControllerQueue.signal(SIGNAL_NOTIFY_UPDATE);
		}
		catch (Exception e) 
		{
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	protected class WatcherConfiguration
	{
		private UUID id = null;
		private File directory = null;
		private String fileNameFilter = null;
		private FileType fileTypeFilter = null;
		private EventType eventTypeFilter = null;
		private String eventtopic = null;
		private boolean enabled = false;
		private long version = 1L;
		private IQueue watcherServiceControllerQueue = null;
		private IQueue watchEventNotifyQueue = null;
		
		public UUID getId()
		{
			return id;
		}
		public File getDirectory()
		{
			return directory;
		}
		public String getFileNameFilter()
		{
			return fileNameFilter;
		}
		public FileType getFileTypeFilter()
		{
			return fileTypeFilter;
		}
		public EventType getEventTypeFilter()
		{
			return eventTypeFilter;
		}
		public String getEventtopic()
		{
			return eventtopic;
		}
		public boolean isEnabled()
		{
			return enabled;
		}
		public long getVersion()
		{
			return version;
		}
		public IQueue getWatcherServiceControllerQueue()
		{
			return watcherServiceControllerQueue;
		}
		public IQueue getWatchEventNotifyQueue()
		{
			return watchEventNotifyQueue;
		}
	}
}
