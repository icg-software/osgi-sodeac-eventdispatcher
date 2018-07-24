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
import org.sodeac.eventdispatcher.extension.api.PropertyBlockModifyItem;

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
	
	public static final Map<String,Object> EMPTY_PROPERTIES = Collections.unmodifiableMap(new HashMap<String,Object>());
	public static final List<String> EMPTY_KEYLIST = Collections.unmodifiableList(new ArrayList<String>());
	
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
			this.propertiesCopy = null;
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
	public Map<String, Object> setPropertySet(Map<String, Object> propertySet, boolean ignoreIfEquals)
	{
		if(propertySet == null)
		{
			return EMPTY_PROPERTIES;
		}
		
		if(propertySet.isEmpty())
		{
			return EMPTY_PROPERTIES;
		}
		
		Map<String, Object> oldValues = null;
		List<PropertyBlockModifyItem> modifyList = null;
		List<IPropertyBlockModifyListener> listenerList = null;
		
		propertiesWriteLock.lock();
		try
		{
			if(this.properties == null)
			{
				this.properties = new HashMap<String,Object>();
			}
			
			IPropertyBlockModifyListener.ModifyType modifyType;
			String key;
			Object oldValue;
			Object newValue;
			boolean update;
			
			for(Entry<String,Object> propertyEntry : propertySet.entrySet())
			{
				if(this.properties.containsKey(propertyEntry.getKey()))
				{
					modifyType = IPropertyBlockModifyListener.ModifyType.UPDATE;
				}
				else
				{
					modifyType = IPropertyBlockModifyListener.ModifyType.INSERT;
				}
				
				key = propertyEntry.getKey();
				oldValue = this.properties.get(key);
				newValue = propertyEntry.getValue();
				
				update = ! ignoreIfEquals;
				if(ignoreIfEquals)
				{
					if
					(
						((oldValue == null) && (newValue != null)) || 
						((oldValue != null) && (newValue == null))
					)
					{
						update = true;
					}
					else if((oldValue == null) && (newValue != null))
					{
						continue;
					}
					else if(oldValue.equals(newValue))
					{
						continue;
					}
				}
				
				if(update)
				{
					if(modifyList == null)
					{
						oldValues = new HashMap<String, Object>();
						modifyList = new ArrayList<PropertyBlockModifyItem>();
					}
					modifyList.add(new PropertyBlockModifyItem(modifyType, key, oldValue, newValue));
					oldValues.put(key, oldValue);
					this.properties.put(key, newValue);
				}
			}
			
			if (modifyList != null)
			{
				this.propertiesCopy = null;
				this.keyList = null;
				listenerList = this.modifyListenerList;
			}
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		
		if(modifyList == null)
		{
			return EMPTY_PROPERTIES;
		}
		
		if(listenerList != null)
		{
			try
			{
				for(IPropertyBlockModifyListener listener : listenerList)
				{
					try
					{
						listener.onModifySet(modifyList);
					}
					catch (Exception e) 
					{
						if(dispatcher != null)
						{
							dispatcher.log(LogService.LOG_ERROR,"execute property modify listener (update/insert set)", e);
						}
					}
				}
			}
			catch (Exception e) 
			{
				if(dispatcher != null)
				{
					dispatcher.log(LogService.LOG_ERROR,"execute property modify listener list (update/insert set)", e);
				}
			}
		}
		
		return oldValues;
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
			this.propertiesCopy = null;
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
			return EMPTY_KEYLIST;
		}
		
		try
		{
			propertiesReadLock.lock();
			if(this.keyList == null)
			{
				this.keyList = Collections.unmodifiableList(new ArrayList<String>(this.properties.keySet()));
			}
			return this.keyList;
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
	}
	
	@Override
	public Map<String, Object> getProperties()
	{
		if(this.properties == null)
		{
			return EMPTY_PROPERTIES;
		}
		
		Map<String,Object> props = this.propertiesCopy;
		if(props == null)
		{
			propertiesWriteLock.lock();
			try
			{
				if(this.properties == null)
				{
					return EMPTY_PROPERTIES;
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
	public Map<String, Object> clear()
	{
		Map<String, Object> oldValues = null;
		
		if(this.properties == null)
		{
			return EMPTY_PROPERTIES;
		}
		
		List<IPropertyBlockModifyListener> listenerList = null;
		List<PropertyBlockModifyItem> modifyList = null;
		
		propertiesWriteLock.lock();
		try
		{
			if(this.properties == null)
			{
				return EMPTY_PROPERTIES;
			}
			
			if(this.properties.isEmpty())
			{
				return EMPTY_PROPERTIES;
			}
			
			modifyList = new ArrayList<PropertyBlockModifyItem>();
			
			oldValues = new HashMap<>(this.properties);
			
			if((this.modifyListenerList != null) && (! this.modifyListenerList.isEmpty()) && (! this.properties.isEmpty()))
			{
				listenerList = this.modifyListenerList;
			}
			
			for(Entry<String,Object> oldEntry : oldValues.entrySet())
			{
				modifyList.add(new PropertyBlockModifyItem(IPropertyBlockModifyListener.ModifyType.REMOVE, oldEntry.getKey(), oldEntry.getValue(), null));
			}
			
			this.keyList = null;
			this.properties.clear();
			this.propertiesCopy = null;
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
						listener.onModifySet(modifyList);
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
		return oldValues;
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
			this.properties =  null;
			this.propertiesCopy = null;
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
	}

	@Override
	public boolean isEmpty()
	{
		if(this.properties == null)
		{
			return false;
		}
		
		try
		{
			propertiesReadLock.lock();
			if(this.properties == null)
			{
				return false;
			}
			return this.properties.isEmpty();
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
	}
	
	@Override
	public boolean containsKey(Object key)
	{
		if(this.properties == null)
		{
			return false;
		}
		
		try
		{
			propertiesReadLock.lock();
			if(this.properties == null)
			{
				return false;
			}
			return this.properties.containsKey(key);
		}
		finally 
		{
			propertiesReadLock.unlock();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getProperty(String key,Class<T> resultClass)
	{
		return(T) getProperty(key);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getProperty(String key,Class<T> resultClass, T defaultValue)
	{
		T typedValue = defaultValue;
		Object current = getProperty(key);
		if(current != null)
		{
			typedValue = (T)current;
		}
		else
		{
			typedValue = defaultValue;
		}
		return typedValue;
	}
	
	@Override
	public String getNonEmptyStringProperty(String key, String defaultValue)
	{
		String stringValue = defaultValue;
		Object current = getProperty(key);
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
}
