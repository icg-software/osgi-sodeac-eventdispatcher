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
						IOnFireEvent,
						IOnJobDone,
						IOnJobError,
						IOnJobTimeout,
						IOnQueueObserve,
						IOnQueueReverse,
						IOnQueueSignal,
						IOnScheduleEvent,
						IOnScheduleEventList,
						IOnRemoveEvent
{
	
	public default boolean implementsOnScheduleEventList()
	{
		return implementsControllerMethod("onScheduleEventList", Void.TYPE, Snapshot.class, List.class);
	}
	
	public default boolean implementsOnScheduleEvent()
	{
		return implementsControllerMethod("onScheduleEvent", Void.TYPE, IQueuedEvent.class);
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
	
	public default boolean implementsOnJobError()
	{
		return implementsControllerMethod("onJobError", Void.TYPE, IQueueJob.class, Throwable.class);
	}
	
	public default boolean implementsOnJobDone()
	{
		return implementsControllerMethod("onJobDone", Void.TYPE, IQueueJob.class);
	}
	
	public default boolean implementsOnJobTimeout()
	{
		return implementsControllerMethod("onJobTimeout", Void.TYPE, IQueueJob.class);
	}
	
	public default boolean implementsOnFireEvent()
	{
		return implementsControllerMethod("onFireEvent", Void.TYPE, Event.class, IQueue.class);
	}

	public default boolean implementsOnRemoveEvent()
	{
		return implementsControllerMethod("onRemoveEvent", Void.TYPE, IQueuedEvent.class);
	}
	
	@Override
	default void onScheduleEventList(IQueue queue, Snapshot<IQueuedEvent> newEvents){}

	@Override
	default void onScheduleEvent(IQueuedEvent event){}

	@Override
	default void onQueueSignal(IQueue queue, String signal){}

	@Override
	default void onQueueReverse(IQueue queue){}

	@Override
	default void onQueueObserve(IQueue queue){}

	@Override
	default void onJobError(IQueueJob job, Throwable throwable){}

	@Override
	default void onJobDone(IQueueJob job){}

	@Override
	default void onJobTimeout(IQueueJob job){}

	@Override
	default void onFireEvent(Event event, IQueue queue){}
	
	@Override
	default void onRemoveEvent(IQueuedEvent event) {};
	
	
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
