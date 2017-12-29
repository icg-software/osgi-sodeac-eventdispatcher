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
package org.sodeac.eventdispatcher.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.extension.api.IExtensiblePropertyBlock;
import org.sodeac.eventdispatcher.extension.api.IPropertyBlockModifyListener;

public class PropertyBlockImpl implements IPropertyBlock,IExtensiblePropertyBlock
{
	public PropertyBlockImpl(EventDispatcherImpl dispatcher)
	{
		super();
		
		this.propertiesLock = new ReentrantReadWriteLock(true);
		this.propertiesReadLock = this.propertiesLock.readLock();
		this.propertiesWriteLock = this.propertiesLock.writeLock();
		
		this.dispatcher = dispatcher;
	}
	
	private List<IPropertyBlockModifyListener> modifyListenerList = null;
	
	private Map<String,Object> properties;
	private Map<String,Object> propertiesCopy;
	private List<String> keyList;
	private ReentrantReadWriteLock propertiesLock;
	private ReadLock propertiesReadLock;
	private WriteLock propertiesWriteLock;
	
	private EventDispatcherImpl dispatcher = null;
	
	@Override
	public Object setProperty(String key, Object value)
	{
		Object old = null;
		IPropertyBlockModifyListener.ModifyType modifyType = IPropertyBlockModifyListener.ModifyType.INSERT;
		List<IPropertyBlockModifyListener> listenerList = null;
		
		propertiesWriteLock.lock();
		try
		{
			if(this.properties == null)
			{
				this.properties = new HashMap<String,Object>();
			}
			else
			{
				if(this.properties.containsKey(key))
				{
					modifyType = IPropertyBlockModifyListener.ModifyType.UPDATE;
				}
				old = this.properties.get(key);
			}
			this.properties.put(key, value);
			this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(this.properties));
			this.keyList = null;
			listenerList = this.modifyListenerList;
			
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		
		if(listenerList != null)
		{
			try
			{
				for(IPropertyBlockModifyListener listener : listenerList)
				{
					try
					{
						listener.onModify(modifyType, key, old, value);
					}
					catch (Exception e) 
					{
						if(dispatcher != null)
						{
							dispatcher.log(LogService.LOG_ERROR,"execute property modify listener (update/insert)", e);
						}
					}
				}
			}
			catch (Exception e) 
			{
				if(dispatcher != null)
				{
					dispatcher.log(LogService.LOG_ERROR,"execute property modify listener list (update/insert)", e);
				}
			}
		}
		return old;
	}

	@Override
	public Object getProperty(String key)
	{
		if(this.properties == null)
		{
			return null;
		}
		
		try
		{
			propertiesReadLock.lock();
			return this.properties.get(key);
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
	}
	
	@Override
	public Object removeProperty(String key)
	{
		if(this.properties == null)
		{
			return null;
		}
		
		try
		{
			propertiesReadLock.lock();
			if(! properties.containsKey(key))
			{
				return null;
			}
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
		
		Object oldPropertyValue = null;
		List<IPropertyBlockModifyListener> listenerList = null;
		
		try
		{
			propertiesWriteLock.lock();
			
			if(! properties.containsKey(key))
			{
				return null;
			}
			
			oldPropertyValue = this.properties.get(key);
			
			this.properties.remove(key);
			this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(this.properties));
			this.keyList = null;
			listenerList = this.modifyListenerList;
			
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		
		if(listenerList != null)
		{
			try
			{
				for(IPropertyBlockModifyListener listener : listenerList)
				{
					try
					{
						listener.onModify(IPropertyBlockModifyListener.ModifyType.REMOVE, key, oldPropertyValue, null);
					}
					catch (Exception e) 
					{
						if(dispatcher != null)
						{
							dispatcher.log(LogService.LOG_ERROR,"execute property modify listener (remove)", e);
						}
					}
				}
			}
			catch (Exception e) 
			{
				if(dispatcher != null)
				{
					dispatcher.log(LogService.LOG_ERROR,"execute property modify listener list (remove)", e);
				}
			}
		}
		
		return oldPropertyValue;
	}

	@Override
	public List<String> getPropertyKeys()
	{
		if(this.properties == null)
		{
			if(this.keyList == null)
			{
				this.keyList = Collections.unmodifiableList(new ArrayList<String>());
			}
			return keyList;
		}
		try
		{
			propertiesReadLock.lock();
			if(this.keyList == null)
			{
				this.keyList = new ArrayList<String>();
				for(String key : this.properties.keySet())
				{
					this.keyList.add(key);
				}
			}
			return Collections.unmodifiableList(this.keyList);
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
	}
	
	@Override
	public Map<String, Object> getProperties()
	{
		Map<String,Object> props = this.propertiesCopy;
		if(props == null)
		{
			propertiesWriteLock.lock();
			try
			{
				if(this.properties == null)
				{
					this.properties = new HashMap<String,Object>();
				}
				this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(this.properties));
				props = this.propertiesCopy;
			}
			finally 
			{
				propertiesWriteLock.unlock();
			} 
		}
		return props;
	}

	@Override
	public void clear()
	{
		if(this.properties == null)
		{
			return;
		}
		
		Map<String,Object> propertiesOld = null;
		List<IPropertyBlockModifyListener> listenerList = null;
		
		propertiesWriteLock.lock();
		try
		{
			if(this.properties == null)
			{
				return;
			}
			
			if((this.modifyListenerList != null) && (! this.modifyListenerList.isEmpty()) && (! this.properties.isEmpty()))
			{
				propertiesOld = new HashMap<String,Object>(this.properties);
				listenerList = this.modifyListenerList;
			}
			
			this.keyList = null;
			this.properties.clear();
			this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(this.properties));
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		
		if(listenerList != null)
		{
			for(Entry<String,Object> oldEntry : propertiesOld.entrySet())
			{
				if(listenerList != null)
				{
					try
					{
						for(IPropertyBlockModifyListener listener : listenerList)
						{
							try
							{
								listener.onModify(IPropertyBlockModifyListener.ModifyType.REMOVE, oldEntry.getKey(), oldEntry.getValue(), null);
							}
							catch (Exception e) 
							{
								if(dispatcher != null)
								{
									dispatcher.log(LogService.LOG_ERROR,"execute property modify listener (clear)", e);
								}
							}
						}
					}
					catch (Exception e) 
					{
						if(dispatcher != null)
						{
							dispatcher.log(LogService.LOG_ERROR,"execute property modify listener list (clear)", e);
						}
					}
				}
			}
		}
		
	}

	@Override
	public void addModifyListener(IPropertyBlockModifyListener listener)
	{
		propertiesWriteLock.lock();
		try
		{
			if(this.modifyListenerList == null)
			{
				this.modifyListenerList = new ArrayList<>();
			}
			for(IPropertyBlockModifyListener listenerExists : this.modifyListenerList)
			{
				if(listenerExists == listener)
				{
					return;
				}
			}
			this.modifyListenerList.add(listener);
			this.modifyListenerList = new ArrayList<IPropertyBlockModifyListener>(this.modifyListenerList);
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
	}

	@Override
	public void removeModifyListener(IPropertyBlockModifyListener listener)
	{
		propertiesWriteLock.lock();
		try
		{
			if(this.modifyListenerList == null)
			{
				return;
			}
			
			while(this.modifyListenerList.remove(listener)) {}
			
			this.modifyListenerList = new ArrayList<IPropertyBlockModifyListener>(this.modifyListenerList);
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
	}

	@Override
	public void dispose()
	{
		propertiesWriteLock.lock();
		try
		{
			if(this.modifyListenerList != null)
			{
				this.modifyListenerList.clear();
				this.modifyListenerList = null;
			}
			this.keyList = null;
			this.properties.clear();
			this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(this.properties));
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
	}
	
}
