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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Supplier;

import org.osgi.service.log.LogService;
import org.sodeac.eventdispatcher.api.IPropertyBlock;
import org.sodeac.eventdispatcher.api.IPropertyBlockAtomicProcedure;
import org.sodeac.eventdispatcher.api.IPropertyLock;
import org.sodeac.eventdispatcher.api.PropertyIsLockedException;
import org.sodeac.eventdispatcher.extension.api.IExtensiblePropertyBlock;
import org.sodeac.eventdispatcher.api.IPropertyBlockModifyListener;
import org.sodeac.eventdispatcher.api.PropertyBlockModifyItem;

public class PropertyBlockImpl implements IPropertyBlock,IExtensiblePropertyBlock
{
	public PropertyBlockImpl(EventDispatcherImpl dispatcher)
	{
		super();
		
		this.propertiesLock = new ReentrantReadWriteLock(true);
		this.propertiesReadLock = this.propertiesLock.readLock();
		this.propertiesWriteLock = this.propertiesLock.writeLock();
		
		this.lockedProperties = null;
		
		this.dispatcher = dispatcher;
	}
	
	public static final Map<String,Object> EMPTY_PROPERTIES = Collections.emptyMap();
	public static final Set<String> EMPTY_KEYSET = Collections.emptySet();
	public static final List<IPropertyBlockModifyListener> EMPTY_MODIFY_LISTENER = Collections.emptyList();
	
	private volatile List<IPropertyBlockModifyListener> modifyListenerList = null;
	private volatile List<IPropertyBlockModifyListener> modifyListenerListCopy = null;
	
	private Map<String,Object> properties;
	private Map<String,Object> propertiesCopy;
	private Set<String> keySet;
	
	private ReentrantReadWriteLock propertiesLock;
	private ReadLock propertiesReadLock;
	private WriteLock propertiesWriteLock;
	
	private Map<String,UUID> lockedProperties;
	
	private EventDispatcherImpl dispatcher;
	
	@Override
	public Object setProperty(String key, Object value)
	{
		Object old = null;
		IPropertyBlockModifyListener.ModifyType modifyType = IPropertyBlockModifyListener.ModifyType.INSERT;
		List<IPropertyBlockModifyListener> listenerList = null;
		
		propertiesWriteLock.lock();
		try
		{
			if((this.lockedProperties != null) && (this.lockedProperties.get(key) != null))
			{
				throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
			}
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
			this.keySet = null;
			listenerList = getModifyListenerList();
			
			
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		
		if((listenerList != null) && (! listenerList.isEmpty()))
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
	public Map<String, Object> setPropertyEntrySet(Set<Entry<String,Object>> propertyEntrySet, boolean ignoreIfEquals)
	{
		if(propertyEntrySet == null)
		{
			return EMPTY_PROPERTIES;
		}
		
		if(propertyEntrySet.isEmpty())
		{
			return EMPTY_PROPERTIES;
		}
		
		Map<String, Object> oldValues = null;
		List<PropertyBlockModifyItem> modifyList = null;
		List<IPropertyBlockModifyListener> listenerList = null;
		
		propertiesWriteLock.lock();
		try
		{
			if(this.lockedProperties != null)
			{
				for(Entry<String,Object> entry : propertyEntrySet)
				{
					if(this.lockedProperties.get(entry.getKey()) != null)
					{
						throw new PropertyIsLockedException("writable access to \"" + entry.getKey() + "\" denied by lock");
					}
				}
			}
			
			if(this.properties == null)
			{
				this.properties = new HashMap<String,Object>();
			}
			
			IPropertyBlockModifyListener.ModifyType modifyType;
			String key;
			Object oldValue;
			Object newValue;
			boolean update;
			
			for(Entry<String,Object> propertyEntry : propertyEntrySet)
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
				this.keySet = null;
				listenerList = getModifyListenerList();
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
		
		if((listenerList != null) && (! listenerList.isEmpty()))
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
			
			if((this.lockedProperties != null) && (this.lockedProperties.get(key) != null))
			{
				throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
			}
			
			if(! properties.containsKey(key))
			{
				return null;
			}
			
			oldPropertyValue = this.properties.get(key);
			
			this.properties.remove(key);
			this.propertiesCopy = null;
			this.keySet = null;
			listenerList = getModifyListenerList();
			
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		
		if((listenerList != null) && (! listenerList.isEmpty()))
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
	public Set<String> getPropertyKeySet()
	{
		if(this.properties == null)
		{
			return EMPTY_KEYSET;
		}
		
		try
		{
			propertiesReadLock.lock();
			if(this.keySet == null)
			{
				this.keySet = Collections.unmodifiableSet(this.properties.keySet());
			}
			return this.keySet;
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
			if((this.lockedProperties != null) && (!this.lockedProperties.isEmpty()))
			{
				throw new PropertyIsLockedException("clear failed. property block as locks");
			}
			
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
			
			listenerList = getModifyListenerList();
			
			for(Entry<String,Object> oldEntry : oldValues.entrySet())
			{
				modifyList.add(new PropertyBlockModifyItem(IPropertyBlockModifyListener.ModifyType.REMOVE, oldEntry.getKey(), oldEntry.getValue(), null));
			}
			
			this.keySet = null;
			this.properties.clear();
			this.propertiesCopy = null;
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		
		if((listenerList != null) && (! listenerList.isEmpty()))
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
			
			List<IPropertyBlockModifyListener> modifyListenerListCopy = this.modifyListenerListCopy;
			if((modifyListenerListCopy != null) && (! modifyListenerListCopy.isEmpty()))
			{
				try
				{
					 // TODO doit in future -- this.modifyListenerListCopy.clear();
				}
				catch (Exception e) {}
			}
			this.modifyListenerListCopy = null;
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
			
			List<IPropertyBlockModifyListener> modifyListenerListCopy = this.modifyListenerListCopy;
			if((modifyListenerListCopy != null) && (! modifyListenerListCopy.isEmpty()))
			{
				try
				{
					 // TODO doit in future -- this.modifyListenerListCopy.clear();
				}
				catch (Exception e) {}
			}
			this.modifyListenerListCopy = null;
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
				try
				{
					if(! this.modifyListenerList.isEmpty())
					{
						this.modifyListenerList.clear();
					}
				}
				catch (Exception e) {}
				this.modifyListenerList = null;
			}
			this.modifyListenerListCopy = null;
			this.keySet = null;
			if(this.properties != null)
			{
				try
				{
					this.properties.clear();
				}
				catch (Exception e) {}
				this.properties =  null;
			}
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
	public <T> T getPropertyOrDefault(String key,Class<T> resultClass, T defaultValue)
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
	public String getPropertyOrDefaultAsString(String key, String defaultValue)
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

	@Override
	public IPropertyLock lockProperty(String key)
	{
		if(key == null)
		{
			return null;
		}
		
		if(key.isEmpty())
		{
			return null;
		}
		
		propertiesWriteLock.lock();
		try
		{
			if(this.lockedProperties == null)
			{
				this.lockedProperties = new HashMap<String,UUID>();
			}
			else if(this.lockedProperties.get(key) != null)
			{
				return null;
			}
			
			UUID pin = UUID.randomUUID();
			this.lockedProperties.put(key, pin);
			return new PropertyLockImpl(this, key, pin);
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
	}
	
	protected void unlockAllProperties()
	{
		propertiesWriteLock.lock();
		try
		{
			this.lockedProperties = null;
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
	}
	
	protected boolean unlockProperty(PropertyLockImpl lock)
	{
		if(lock == null)
		{
			return false;
		}
		if(this != lock.getBlock())
		{
			return false;
		}
		if((lock.getKey() == null) || lock.getKey().isEmpty())
		{
			return false;
		}
		if(lock.getPin() == null)
		{
			return false;
		}
		propertiesWriteLock.lock();
		try
		{
			if(this.lockedProperties == null)
			{
				return true;
			}
			UUID currentPin = this.lockedProperties.get(lock.getKey());
			if((currentPin == null) || currentPin.equals(lock.getPin()))
			{
				this.lockedProperties.remove(lock.getKey());
				return true;
			}
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		return false;
	}

	@Override
	public Supplier<List<PropertyBlockModifyItem>> computeProcedure(IPropertyBlockAtomicProcedure operationHandler)
	{
		UnlockedWrapper wrapper = new UnlockedWrapper();
		List<IPropertyBlockModifyListener> listenerList = null;
		
		propertiesWriteLock.lock();
		try
		{
			operationHandler.accept(wrapper);
			
			if((wrapper.modifyList != null) && (!wrapper.modifyList.isEmpty()) && (!((this.modifyListenerList == null) || this.modifyListenerList.isEmpty())))
			{
				listenerList = getModifyListenerList();
			}
			if(wrapper.modifyList != null)
			{
				wrapper.modifyList = Collections.unmodifiableList(wrapper.modifyList);
			}
		}
		finally 
		{
			wrapper.valid.set(false);
			propertiesWriteLock.unlock();
		}
		
		if((listenerList != null) && (! listenerList.isEmpty()))
		{
			for(IPropertyBlockModifyListener listener : listenerList)
			{
				listener.onModifySet(wrapper.modifyList);
			}
		}
		
		return new PropertyBlockProcedureModifyAuditTrail(wrapper.modifyList);
	}
	
	private class PropertyBlockProcedureModifyAuditTrail implements Supplier<List<PropertyBlockModifyItem>>
	{
		private  List<PropertyBlockModifyItem> modifyList = null;
		
		public PropertyBlockProcedureModifyAuditTrail( List<PropertyBlockModifyItem> modifyList)
		{
			super();
			this.modifyList = modifyList;
		}
		
		@Override
		public List<PropertyBlockModifyItem> get()
		{
			return this.modifyList;
		}
		
		
	}
	
	private class UnlockedWrapper implements IPropertyBlock
	{
		private List<PropertyBlockModifyItem> modifyList = null;
		private AtomicBoolean valid = null;
		
		public UnlockedWrapper()
		{
			super();
			valid = new AtomicBoolean(true);
		}
	
		@Override
		public Object setProperty(String key, Object value) throws PropertyIsLockedException
		{
			checkValid();
			
			Object old = null;
			IPropertyBlockModifyListener.ModifyType modifyType = IPropertyBlockModifyListener.ModifyType.INSERT;
			
			if((PropertyBlockImpl.this.lockedProperties != null) && (PropertyBlockImpl.this.lockedProperties.get(key) != null))
			{
				throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
			}
			if(PropertyBlockImpl.this.properties == null)
			{
				PropertyBlockImpl.this.properties = new HashMap<String,Object>();
			}
			else
			{
				if(PropertyBlockImpl.this.properties.containsKey(key))
				{
					modifyType = IPropertyBlockModifyListener.ModifyType.UPDATE;
				}
				old = PropertyBlockImpl.this.properties.get(key);
			}
			PropertyBlockImpl.this.properties.put(key, value);
			PropertyBlockImpl.this.propertiesCopy = null;
			PropertyBlockImpl.this.keySet = null;
			if(this.modifyList == null)
			{
				this.modifyList = new ArrayList<PropertyBlockModifyItem>();
			}
			this.modifyList.add(new PropertyBlockModifyItem(modifyType,key,old,value));
			
			return old;
		}

		@Override
		public Map<String,Object> setPropertyEntrySet(Set<Entry<String,Object>> propertyEntrySet, boolean ignoreIfEquals) throws PropertyIsLockedException
		{
			checkValid();
			
			if(propertyEntrySet == null)
			{
				return EMPTY_PROPERTIES;
			}
			
			if(propertyEntrySet.isEmpty())
			{
				return EMPTY_PROPERTIES;
			}
			Map<String,Object> oldValue = new HashMap<String,Object>();
			for(Entry<String, Object> entry : propertyEntrySet)
			{
				oldValue.put(entry.getKey(),this.setProperty(entry.getKey(), entry.getValue()));
			}
			return oldValue;
		}

		@Override
		public Object getProperty(String key)
		{
			checkValid();
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return null;
			}
			return PropertyBlockImpl.this.properties.get(key);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getProperty(String key, Class<T> resultClass)
		{
			checkValid();
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return null;
			}
			return (T)PropertyBlockImpl.this.properties.get(key);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T getPropertyOrDefault(String key, Class<T> resultClass, T defaultValue)
		{
			checkValid();
			
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
		public String getPropertyOrDefaultAsString(String key, String defaultValue)
		{
			checkValid();
			
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

		@Override
		public Object removeProperty(String key) throws PropertyIsLockedException
		{
			checkValid();
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return null;
			}
			
			if(! properties.containsKey(key))
			{
				return null;
			}
			
			Object oldPropertyValue = null;
			
			if((PropertyBlockImpl.this.lockedProperties != null) && (PropertyBlockImpl.this.lockedProperties.get(key) != null))
			{
				throw new PropertyIsLockedException("writable access to \"" + key + "\" denied by lock");
			}
				
				
			oldPropertyValue = PropertyBlockImpl.this.properties.get(key);
				
			PropertyBlockImpl.this.properties.remove(key);
			PropertyBlockImpl.this.propertiesCopy = null;
			PropertyBlockImpl.this.keySet = null;
			
			if(this.modifyList == null)
			{
				this.modifyList = new ArrayList<PropertyBlockModifyItem>();
			}
			this.modifyList.add(new PropertyBlockModifyItem(IPropertyBlockModifyListener.ModifyType.REMOVE,key,oldPropertyValue,null));
			
			return oldPropertyValue;
		}

		@Override
		public Map<String, Object> getProperties()
		{
			checkValid();
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return EMPTY_PROPERTIES;
			}
			
			Map<String,Object> props = PropertyBlockImpl.this.propertiesCopy;
			if(props == null)
			{
				if(PropertyBlockImpl.this.properties == null)
				{
					return EMPTY_PROPERTIES;
				}
				PropertyBlockImpl.this.propertiesCopy = Collections.unmodifiableMap(new HashMap<String,Object>(PropertyBlockImpl.this.properties));
				props = PropertyBlockImpl.this.propertiesCopy; 
			}
			return props;
		}

		@Override
		public Set<String> getPropertyKeySet()
		{
			checkValid();
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return EMPTY_KEYSET;
			}
			
			if(PropertyBlockImpl.this.keySet == null)
			{
				PropertyBlockImpl.this.keySet = Collections.unmodifiableSet(PropertyBlockImpl.this.properties.keySet());
			}
			return PropertyBlockImpl.this.keySet;
		}

		@Override
		public boolean isEmpty()
		{
			checkValid();
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return false;
			}
			
			return PropertyBlockImpl.this.properties.isEmpty();
		}

		@Override
		public boolean containsKey(Object key)
		{
			checkValid();
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return false;
			}

			return PropertyBlockImpl.this.properties.containsKey(key);
		}

		@Override
		public Map<String, Object> clear() throws PropertyIsLockedException
		{
			checkValid();
			
			Map<String, Object> oldValues = null;
			
			if(PropertyBlockImpl.this.properties == null)
			{
				return EMPTY_PROPERTIES;
			}
			if(PropertyBlockImpl.this.properties.isEmpty())
			{
				return EMPTY_PROPERTIES;
			}
			
			if((PropertyBlockImpl.this.lockedProperties != null) && (!PropertyBlockImpl.this.lockedProperties.isEmpty()))
			{
				throw new PropertyIsLockedException("clear failed. property block as locks");
			}
			
			oldValues = new HashMap<>(PropertyBlockImpl.this.properties);
			for(Entry<String,Object> oldEntry : oldValues.entrySet())
			{
				if(this.modifyList == null)
				{
					this.modifyList = new ArrayList<PropertyBlockModifyItem>();
				}
				this.modifyList.add(new PropertyBlockModifyItem(IPropertyBlockModifyListener.ModifyType.REMOVE, oldEntry.getKey(), oldEntry.getValue(), null));
				
			}
				
			PropertyBlockImpl.this.keySet = null;
			PropertyBlockImpl.this.properties.clear();
			PropertyBlockImpl.this.propertiesCopy = null;
			
			return oldValues;
		}

		@Override
		public IPropertyLock lockProperty(String key)
		{
			checkValid();
			
			if(key == null)
			{
				return null;
			}
			
			if(key.isEmpty())
			{
				return null;
			}
			
			if(PropertyBlockImpl.this.lockedProperties == null)
			{
				PropertyBlockImpl.this.lockedProperties = new HashMap<String,UUID>();
			}
			else if(PropertyBlockImpl.this.lockedProperties.get(key) != null)
			{
				return null;
			}
				
			UUID pin = UUID.randomUUID();
			PropertyBlockImpl.this.lockedProperties.put(key, pin);
			return new PropertyLockImpl(PropertyBlockImpl.this, key, pin);

		}

		@Override
		public Supplier<List<PropertyBlockModifyItem>> computeProcedure(IPropertyBlockAtomicProcedure operationHandler)
		{
			checkValid();
			
			operationHandler.accept(this);
			
			if(this.modifyList == null)
			{
				return new PropertyBlockProcedureModifyAuditTrail(null);
			}
			return new PropertyBlockProcedureModifyAuditTrail(Collections.unmodifiableList(this.modifyList));
		}
		
		private void checkValid()
		{
			if(! this.valid.get())
			{
				throw new RuntimeException("PropertyBlock Wrapper not valid");
			}
		}
		
	}
	
	private List<IPropertyBlockModifyListener> getModifyListenerList()
	{
		List<IPropertyBlockModifyListener> list = this.modifyListenerListCopy;
		if(list != null)
		{
			return list;
		}
		
		propertiesWriteLock.lock();
		try
		{
			list = this.modifyListenerListCopy;
			if(list != null)
			{
				return list;
			}
			
			list = (this.modifyListenerList == null || this.modifyListenerList.isEmpty()) ? EMPTY_MODIFY_LISTENER : new ArrayList<IPropertyBlockModifyListener>(this.modifyListenerList);	
		}
		finally 
		{
			propertiesWriteLock.unlock();
		}
		this.modifyListenerListCopy = list;
		return list;
	}
}
