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
package org.sodeac.eventdispatcher.itest.components.scope;

import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnJobDone;
import org.sodeac.eventdispatcher.api.IOnJobError;
import org.sodeac.eventdispatcher.api.IOnJobTimeout;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IOnRemoveEvent;
import org.sodeac.eventdispatcher.api.IOnEventScheduled;
import org.sodeac.eventdispatcher.api.IOnFireEvent;
import org.sodeac.eventdispatcher.api.IQueueScope;
import org.sodeac.eventdispatcher.api.IQueuedEvent;
import org.sodeac.eventdispatcher.itest.components.base.AbstractBaseTestController;

@Component
(
	immediate=true,
	service={IEventController.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"="+"("+IEventDispatcher.PROPERTY_QUEUE_TYPE + "=" + ScopeTestSimpleManagementController.SCOPE_TYPE + ")",
	}
)
public class ScopeTestSimpleScopeController extends AbstractBaseTestController implements IEventController,IOnEventScheduled,IOnRemoveEvent,IOnJobDone,IOnJobError,IOnJobTimeout,IOnFireEvent,IOnQueueObserve,IOnQueueReverse,IOnQueueSignal
{
	
	@Reference(cardinality=ReferenceCardinality.OPTIONAL,policy=ReferencePolicy.DYNAMIC)
	protected volatile IEventDispatcher dispatcher;
	
	
	@Override
	public void onEventScheduled(IQueuedEvent event)
	{
		if(event.getEvent().getTopic().equals(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST1))
		{
			event.getQueue().signal(event.getEvent().getTopic());
			((CountDownLatch)event.getNativeEventProperties().get(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH)).countDown();
			return;
		}
		
		if(event.getEvent().getTopic().equals(ScopeTestSimpleManagementController.REQUEST_EVENT_SCOPE_REQUEST2))
		{
			((IQueueScope)event.getQueue()).dispose();
			((CountDownLatch)event.getNativeEventProperties().get(ScopeTestSimpleManagementController.EVENT_PROPERTY_WORKLATCH)).countDown();
			return;
		}
		
	}
}
