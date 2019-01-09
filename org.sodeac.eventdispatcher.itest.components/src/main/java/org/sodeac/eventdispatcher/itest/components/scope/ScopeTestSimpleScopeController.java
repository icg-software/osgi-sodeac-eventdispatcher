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
package org.sodeac.eventdispatcher.itest.components.scope;

import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.eventdispatcher.api.IQueueController;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnTaskDone;
import org.sodeac.eventdispatcher.api.IOnTaskError;
import org.sodeac.eventdispatcher.api.IOnTaskTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueAttach;
import org.sodeac.eventdispatcher.api.IOnQueueDetach;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemovedEvent;
import org.sodeac.eventdispatcher.api.IOnQueuedEvent;
import org.sodeac.eventdispatcher.api.IOnFiredEvent;
import org.sodeac.eventdispatcher.api.IQueueChildScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.base.AbstractBaseTestController;

@Component
(
	immediate=true,
	service={IQueueController.class},
	property=
	{
		EventDispatcherConstants.PROPERTY_QUEUE_MATCH_FILTER+"="+"("+EventDispatcherConstants.PROPERTY_QUEUE_TYPE + "=" + ScopeTestSimpleManagementController.SCOPE_TYPE + ")",
	}
)
public class ScopeTestSimpleScopeController extends AbstractBaseTestController implements IQueueController,IOnQueuedEvent,IOnRemovedEvent,IOnTaskDone,IOnTaskError,IOnTaskTimeout,IOnFiredEvent,IOnQueueAttach,IOnQueueDetach,IOnQueueSignal
{
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	
	@Override
	public void onQueuedEvent(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST1))
		{
			event.getQueue().signal(event.getEvent().getTopic());
			((CountDownLatch)event.getNativeEventProperties().get(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH)).countDown();
			return;
		}
		
		if(event.getEvent().getTopic().equals(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST2))
		{
			((IQueueChildScope)event.getQueue()).dispose();
			((CountDownLatch)event.getNativeEventProperties().get(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH)).countDown();
			return;
		}
		
	}
}
