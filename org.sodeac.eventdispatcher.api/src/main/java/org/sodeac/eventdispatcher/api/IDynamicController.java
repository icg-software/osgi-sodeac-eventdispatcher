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
package org.sodeac.eventdispatcher.api;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.osgi.service.event.Event;
import org.sodeac.multichainlist.Snapshot;

public interface IDynamicController extends 
						IQueueController,
						IOnFiredEvent,
						IOnTaskDone,
						IOnTaskError,
						IOnTaskTimeout,
						IOnQueueObserve,
						IOnQueueReverse,
						IOnQueueSignal,
						IOnQueuedEvent,
						IOnQueuedEventList,
						IOnRemovedEvent
{
	
	public default boolean implementsOnQueuedEventList()
	{
		return implementsControllerMethod("onQueuedEventList", Void.TYPE, Snapshot.class, List.class);
	}
	
	public default boolean implementsOnQueuedEvent()
	{
		return implementsControllerMethod("onQueuedEvent", Void.TYPE, IQueuedEvent.class);
	}
	
	public default boolean implementsOnQueueSignal()
	{
		return implementsControllerMethod("onQueueSignal", Void.TYPE, IQueue.class, String.class);
	}
	
	public default boolean implementsOnQueueReverse()
	{
		return implementsControllerMethod("onQueueReverse", Void.TYPE, IQueue.class);
	}
	
	public default boolean implementsOnQueueObserve()
	{
		return implementsControllerMethod("onQueueObserve", Void.TYPE, IQueue.class);
	}
	
	public default boolean implementsOnTaskError()
	{
		return implementsControllerMethod("onTaskError", Void.TYPE, IQueue.class, IQueueTask.class, Throwable.class);
	}
	
	public default boolean implementsOnTaskDone()
	{
		return implementsControllerMethod("onTaskDone", Void.TYPE, IQueue.class, IQueueTask.class);
	}
	
	public default boolean implementsOnTaskTimeout()
	{
		return implementsControllerMethod("onTaskTimeout", Void.TYPE, IQueue.class, IQueueTask.class);
	}
	
	public default boolean implementsOnFiredEvent()
	{
		return implementsControllerMethod("onFiredEvent", Void.TYPE, Event.class, IQueue.class);
	}

	public default boolean implementsOnRemovedEvent()
	{
		return implementsControllerMethod("onRemovedEvent", Void.TYPE, IQueuedEvent.class);
	}
	
	@Override
	default void onQueuedEventList(IQueue queue, Snapshot<IQueuedEvent> newEvents){}

	@Override
	default void onQueuedEvent(IQueuedEvent event){}

	@Override
	default void onQueueSignal(IQueue queue, String signal){}

	@Override
	default void onQueueReverse(IQueue queue){}

	@Override
	default void onQueueObserve(IQueue queue){}

	@Override
	default void onTaskError(IQueue queue, IQueueTask task, Throwable throwable){}

	@Override
	default void onTaskDone(IQueue queue,IQueueTask task){}

	@Override
	default void onTaskTimeout(IQueue queue, IQueueTask task){}

	@Override
	default void onFiredEvent(Event event, IQueue queue){}
	
	@Override
	default void onRemovedEvent(IQueuedEvent event) {};
	
	
	default boolean implementsControllerMethod(String name,Class<?> returnType, Class<?>... parameterTypes)
	{
		Class<?> clazz = this.getClass();
		while(clazz != null)
		{
			try
			{
				Method m = clazz.getDeclaredMethod(name,parameterTypes);
				if((m != null) && (m.getReturnType() == returnType) && (m.getModifiers() == Modifier.PUBLIC))
				{
					return true;
				}
			} 
			catch (NoSuchMethodException e){}
			clazz = clazz.getSuperclass();
		}
		return false;
	}
	
}
