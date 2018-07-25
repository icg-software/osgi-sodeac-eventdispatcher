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
package org.sodeac.eventdispatcher.common.service.impl;

import org.osgi.service.component.annotations.Component;
import org.sodeac.eventdispatcher.api.IEventController;
import org.sodeac.eventdispatcher.api.IEventDispatcher;
import org.sodeac.eventdispatcher.api.IOnQueueObserve;
import org.sodeac.eventdispatcher.api.IOnQueueReverse;
import org.sodeac.eventdispatcher.api.IOnQueueSignal;
import org.sodeac.eventdispatcher.api.IQueue;
import org.sodeac.eventdispatcher.common.service.api.IServiceRegistrationAdapter;

@Component
(
	service= {IEventController.class},
	property=
	{
		IEventDispatcher.PROPERTY_QUEUE_MATCH_FILTER+"="+"(org.sodeac.eventdispatcher.common.service.api.IServiceRegistrationAdapter=*)",
	}
)
public class ServiceManagementController implements IEventController, IOnQueueObserve, IOnQueueReverse, IOnQueueSignal
{
	@Override
	public void onQueueObserve(IQueue queue)
	{
	}

	@Override
	public void onQueueReverse(IQueue queue)
	{
	}

	@Override
	public void onQueueSignal(IQueue queue, String signal)
	{
		if(IServiceRegistrationAdapter.SIGNAL_REGISTRATION_UPDATE.equals(signal))
		{
			IServiceRegistrationAdapter registration = queue.getConfigurationPropertyBlock().getAdapter(IServiceRegistrationAdapter.class);
			if(registration == null)
			{
				return;
			}
			registration.updateRegistrations();
		}
	}

}
