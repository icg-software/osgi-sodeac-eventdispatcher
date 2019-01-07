/*******************************************************************************
 * Copyright (c) 2018, 2019 Sebastian Palarus
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Sebastian Palarus - initial API and implementation
 *******************************************************************************/
package org.sodeac.eventdispatcher.common.reactiveservice.api;

import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.Bundle;
import org.sodeac.eventdispatcher.api.EventDispatcherConstants;

public interface IReactiveServiceRegistrationAdapter
{
	public static final String SERVICE_PROPERTY__MATCH_QUEUE = EventDispatcherConstants.PROPERTY_QUEUE_MATCH_FILTER+"="+"(org.sodeac.eventdispatcher.common.reactiveservice.api.IReactiveServiceRegistrationAdapter=*)";
	public static final String SIGNAL_REGISTRATION_UPDATE = "SIGNAL_REGISTRATION_UPDATE";
	
	public void register(IReactiveService service, Dictionary<String, ?> properties, Bundle bundle);
	public void unregister(IReactiveService service, Dictionary<String, ?> properties, Bundle bundle);
	
	public void updateRegistrations();
	public List<IReactiveServiceReference> discoverServices(DiscoverReactiveServiceRequest request);
	public boolean isEmpty();
}
